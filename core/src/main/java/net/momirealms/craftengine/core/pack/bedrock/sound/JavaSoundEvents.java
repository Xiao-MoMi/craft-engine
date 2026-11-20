package net.momirealms.craftengine.core.pack.bedrock.sound;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.pack.bedrock.JavaResourcePack;
import net.momirealms.craftengine.core.sound.SoundData;
import net.momirealms.craftengine.core.util.GsonHelper;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

final class JavaSoundEvents {
    static final Event MISSING = new Event("", List.of(), false);
    private static final Event SILENT = new Event("", List.of(), true);
    private static final Key EMPTY_SOUND_ID = SoundData.EMPTY.id();
    private static final JsonArray NO_SOUNDS = new JsonArray(0);
    private final JavaResourcePack resources;
    private final JsonObject mappings;
    private final Map<Key, Key> replacements;
    private final Map<Key, Key> originals = new HashMap<>();
    private final Map<String, JsonObject> vanillaDefinitions = new HashMap<>();
    private final Map<String, JsonObject> customDefinitions = new HashMap<>();
    private final Map<Key, Event> events = new HashMap<>();
    private final Map<Key, Path> audio = new HashMap<>();
    private final Map<Key, Boolean> silentFiles = new HashMap<>();
    private final Set<Key> visiting = new HashSet<>();
    private byte[] silence;

    JavaSoundEvents(JavaResourcePack resources, JsonObject mappings, Map<Key, Key> replacements) {
        this.resources = resources;
        this.mappings = mappings;
        this.replacements = replacements;
        replacements.forEach((original, alias) -> this.originals.put(alias, original));
    }

    Event resolve(Key id) throws IOException {
        Event cached = this.events.get(id);
        if (cached != null) {
            return cached;
        }
        if (!this.visiting.add(id)) {
            return MISSING;
        }
        try {
            Event result = this.read(id);
            this.events.put(id, result);
            return result;
        } finally {
            this.visiting.remove(id);
        }
    }

    // 确定事件最终使用的声音条目，以及是否还能沿用基岩原版事件
    private Event read(Key id) throws IOException {
        if (EMPTY_SOUND_ID.equals(id)) {
            return SILENT;
        }
        Key original = this.originals.getOrDefault(id, id);
        JsonObject custom = this.definition(original, true);
        Key replacement = this.replacements.get(original);
        // 给 Java 载体事件写入静音定义，并把原本要播放的声音保存在替代 ID 下
        // 生成基岩包时，用替代 ID 下的定义恢复原事件的声音
        JsonObject restored = replacement == null ? null : this.definition(replacement, true);
        boolean carrierMute = restored != null && this.isCarrierMute(custom);
        if (carrierMute) {
            custom = restored;
            id = original;
        } else if (!id.equals(original)) {
            custom = this.definition(id, true);
        }
        JsonObject vanilla = this.definition(id, false);
        JsonElement mapped = id.isMinecraftNamespace() && !this.resources.filtersVanilla("assets/minecraft/sounds.json") ? this.mappings.get(id.value()) : null;
        JsonArray base = sounds(vanilla);
        JsonArray entries = sounds(custom);
        boolean replace = carrierMute || replacesSounds(custom);
        boolean modified = custom != null && (replace || !entries.isEmpty());
        // 自定义定义里按顺序引用的声音与原版一致时，仍可尝试使用基岩原版映射
        if (modified && !base.isEmpty() && sameSamples(entries, base)) {
            modified = false;
        }
        if (!modified) {
            entries = base;
        } else if (!replace) {
            entries = mergeSounds(base, entries);
        }
        if (entries.isEmpty()) {
            if (!modified && mapped != null) {
                return new Event(mapped.getAsString(), List.of(), true);
            }
            return custom != null || vanilla != null ? SILENT : MISSING;
        }
        return this.resolveSamples(id, entries, !modified && mapped != null ? mapped.getAsString() : null);
    }

    // 递归展开事件引用为 OGG 样本，必要时查找并缓存生成基岩声音定义所需的文件
    private Event resolveSamples(Key id, JsonArray entries, @Nullable String nativeSound) throws IOException {
        int entryCount = entries.size();
        List<Key> samples = new ArrayList<>(entryCount);
        Event singleReference = null;
        for (JsonElement entry : entries) {
            Key key = sampleId(entry);
            if (isReference(entry)) {
                Event nested = this.resolve(key);
                if (nested == MISSING) {
                    return MISSING;
                }
                // 列表只有这一条引用时可直接沿用子事件，多个条目则无法混入只有事件名而没有样本的基岩声音
                if (entryCount > 1 && !nested.name().isEmpty() && nested.samples().isEmpty()) {
                    return MISSING;
                }
                if (!nested.vanilla()) {
                    nativeSound = null;
                }
                samples.addAll(nested.samples());
                if (entryCount == 1) {
                    singleReference = nested;
                }
            } else {
                String soundPath = audioPath(key);
                Path customFile = this.resources.soundResource(key.namespace(), soundPath, true);
                if (customFile != null) {
                    // 即使事件定义沿用原版，自定义包里的同名 OGG 也会覆盖实际播放的音频
                    nativeSound = null;
                    this.audio.put(key, customFile);
                } else if (this.resources.filtersVanilla("assets/" + key.namespace() + '/' + soundPath)) {
                    return MISSING;
                }
                samples.add(key);
            }
        }
        if (nativeSound != null) {
            return new Event(nativeSound, List.copyOf(samples), true);
        }
        if (singleReference != null) {
            return singleReference;
        }
        if (samples.isEmpty()) {
            return SILENT;
        }
        for (Key sample : samples) {
            Path file = this.audio.get(sample);
            // 自定义 OGG 已在上面缓存，这里补查原版 OGG，也记录找不到的结果
            if (file == null && !this.audio.containsKey(sample)) {
                try {
                    file = this.resources.soundResource(sample.namespace(), audioPath(sample), false);
                } catch (IOException unavailable) {
                    file = null;
                }
                this.audio.put(sample, file);
            }
            if (file == null) {
                return MISSING;
            }
        }
        return new Event(id.asString(), List.copyOf(samples), false);
    }

    // 取已定位的 OGG 路径，供基岩资源包复制
    Path audio(Key id) {
        return this.audio.get(id);
    }

    // 从指定来源的命名空间定义里取出单个声音事件
    private JsonObject definition(Key id, boolean custom) throws IOException {
        return this.definitions(id.namespace(), custom).getAsJsonObject(id.value());
    }

    // 按命名空间加载 sounds.json，原版与自定义定义各自缓存
    JsonObject definitions(String name, boolean custom) throws IOException {
        Map<String, JsonObject> definitions = custom ? this.customDefinitions : this.vanillaDefinitions;
        JsonObject namespace = definitions.get(name);
        if (namespace == null) {
            try {
                Path file = this.resources.soundResource(name, "sounds.json", custom);
                namespace = file == null ? new JsonObject() : GsonHelper.readJsonObjectFromFile(file);
            } catch (IOException unavailable) {
                if (custom || !name.equals("minecraft")) {
                    throw unavailable;
                }
                // 比如原版缓存只有字体且无法连接 Mojang 资源服务器时，用内置定义继续匹配基岩原版声音
                namespace = originalDefinitions();
            }
            definitions.put(name, namespace);
        }
        return namespace;
    }

    // 原事件被静音且替代 ID 有自定义定义时，才将这对 ID 视为载体声音的替换关系
    boolean hasCarrierReplacement(Key id, Key alias) throws IOException {
        return this.definition(alias, true) != null && this.isCarrierMute(this.definition(id, true));
    }

    // 载体静音必须设置 replace，并清空声音列表或只引用与内置静音 OGG 内容一致的文件
    private boolean isCarrierMute(@Nullable JsonObject definition) throws IOException {
        if (!replacesSounds(definition)) {
            return false;
        }
        JsonArray sounds = sounds(definition);
        if (sounds.isEmpty()) {
            return true;
        }
        if (sounds.size() != 1 || !sounds.get(0).isJsonPrimitive()) {
            return false;
        }
        Key key = Key.of(sounds.get(0).getAsString());
        Boolean cached = this.silentFiles.get(key);
        if (cached != null) {
            return cached;
        }
        Path sound = this.resources.soundResource(key.namespace(), audioPath(key), true);
        if (sound == null) {
            return false;
        }
        if (this.silence == null) {
            try (InputStream input = JavaSoundEvents.class.getResourceAsStream("/silence.ogg")) {
                if (input == null) {
                    return false;
                }
                this.silence = input.readAllBytes();
            }
        }
        boolean silent = Files.size(sound) == this.silence.length && Arrays.equals(this.silence, Files.readAllBytes(sound));
        this.silentFiles.put(key, silent);
        return silent;
    }

    // 首次使用时加载插件内置的原版 sounds.json，供声音解析和播放映射共用
    static JsonObject originalDefinitions() {
        return OriginalDefinitions.ALL;
    }

    // 取事件的 sounds 数组，缺省时返回共享空数组，调用方只读
    static JsonArray sounds(@Nullable JsonObject event) {
        JsonArray sounds = event == null ? null : event.getAsJsonArray("sounds");
        return sounds == null ? NO_SOUNDS : sounds;
    }

    // 按原版条目在前、自定义条目在后的顺序合并，一侧为空时直接复用另一侧的只读数组
    static JsonArray mergeSounds(JsonArray base, JsonArray additions) {
        if (base.isEmpty()) {
            return additions;
        }
        if (additions.isEmpty()) {
            return base;
        }
        JsonArray merged = new JsonArray(base.size() + additions.size());
        merged.addAll(base);
        merged.addAll(additions);
        return merged;
    }

    // 读取 replace 标记，缺省为 false，表示保留原版条目
    static boolean replacesSounds(@Nullable JsonObject definition) {
        JsonElement replace = definition == null ? null : definition.get("replace");
        return replace != null && replace.getAsBoolean();
    }

    // 把音频 ID 转成其命名空间目录下的 OGG 相对路径
    static String audioPath(Key id) {
        return "sounds/" + id.value() + ".ogg";
    }

    // 按顺序比较声音 ID 和引用类型，音量、音调、权重由播放映射单独处理
    private static boolean sameSamples(JsonArray first, JsonArray second) {
        if (first.size() != second.size()) {
            return false;
        }
        for (int index = 0; index < first.size(); index++) {
            JsonElement a = first.get(index);
            JsonElement b = second.get(index);
            if (!sampleId(a).equals(sampleId(b)) || isReference(a) != isReference(b)) {
                return false;
            }
        }
        return true;
    }

    // 声音条目可直接写成字符串，也可在对象的 name 字段里指定 ID
    static Key sampleId(JsonElement entry) {
        return Key.of(entry.isJsonPrimitive() ? entry.getAsString() : entry.getAsJsonObject().get("name").getAsString());
    }

    // type 为 event 时引用另一个声音事件，其余条目按 OGG 文件处理
    static boolean isReference(JsonElement entry) {
        JsonElement type = entry instanceof JsonObject object ? object.get("type") : null;
        return type != null && "event".equals(type.getAsString());
    }

    private static final class OriginalDefinitions {
        private static final JsonObject ALL = GsonHelper.readJsonFromResource("/internal/sounds.json").getAsJsonObject();
    }

    record Event(String name, List<Key> samples, boolean vanilla) {
    }
}
