package net.momirealms.craftengine.core.pack.bedrock;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.momirealms.craftengine.bedrock.BedrockPackLayout;
import net.momirealms.craftengine.core.util.GsonHelper;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourcePackUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class BedrockAttachableSharing {
    private static final Pattern PRIVATE_ID = Pattern.compile("(?:controller\\.render|animation)\\.ce\\.[a-z0-9_./-]+");

    private BedrockAttachableSharing() {
    }

    static void share(Path pack, List<String> files, Map<String, JsonObject> documents, Map<String, String> aliases) throws IOException {
        Map<String, Use> uses = new HashMap<>();
        for (String id : aliases.keySet()) {
            if (id.startsWith("controller.render.ce.") || id.startsWith("animation.ce.")) {
                uses.put(id, new Use());
            }
        }
        if (uses.isEmpty()) {
            return;
        }
        int owner = 0;
        for (String file : files) {
            JsonElement document = documents.get(file);
            if (document == null) {
                document = GsonHelper.readJsonFromFile(pack.resolve(file));
            }
            if (file.startsWith("attachables/")) {
                JsonObject attachable = document.getAsJsonObject();
                documents.put(file, attachable);
                JsonObject description = attachable.getAsJsonObject("minecraft:attachable").getAsJsonObject("description");
                String identifier = description.get("identifier").getAsString();
                if (identifier.startsWith(BedrockPackLayout.ID_PREFIX)) {
                    collectOwner(description, ResourcePackUtils.bedrockResourceIdentifier(Key.of(identifier)), owner++, uses);
                }
            }
            countReferences(document, uses);
        }
        for (String file : files) {
            boolean animation = file.startsWith("animations/");
            if (animation || file.startsWith("render_controllers/")) {
                JsonObject definitions = documents.get(file).getAsJsonObject(animation ? "animations" : "render_controllers");
                if (definitions != null) {
                    shareDefinitions(definitions, animation, uses, aliases);
                }
            }
        }
    }

    private static void collectOwner(JsonObject description, String stem, int owner, Map<String, Use> uses) {
        JsonObject animations = description.getAsJsonObject("animations");
        if (animations != null) {
            String prefix = "animation." + stem;
            for (JsonElement value : animations.asMap().values()) {
                if (value instanceof JsonPrimitive name && name.isString()) {
                    collectReference(name.getAsString(), prefix, owner, uses);
                }
            }
        }
        JsonArray controllers = description.getAsJsonArray("render_controllers");
        if (controllers != null) {
            String prefix = "controller.render." + stem;
            for (JsonElement controller : controllers) {
                if (controller.isJsonObject()) {
                    for (String id : controller.getAsJsonObject().keySet()) {
                        collectReference(id, prefix, owner, uses);
                    }
                } else if (controller instanceof JsonPrimitive name && name.isString()) {
                    collectReference(name.getAsString(), prefix, owner, uses);
                }
            }
        }
    }

    private static void collectReference(String id, String prefix, int owner, Map<String, Use> uses) {
        Use use = uses.get(id);
        if (use != null) {
            use.owners.set(owner);
            use.privateOwner |= id.equals(prefix) || id.startsWith(prefix + ".");
            use.localReferences++;
        }
    }

    private static void countReferences(JsonElement value, Map<String, Use> uses) {
        if (value instanceof JsonObject object) {
            for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                countReferences(entry.getKey(), uses);
                countReferences(entry.getValue(), uses);
            }
        } else if (value instanceof JsonArray array) {
            for (JsonElement entry : array) {
                countReferences(entry, uses);
            }
        } else if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
            countReferences(value.getAsString(), uses);
        }
    }

    private static void countReferences(String text, Map<String, Use> uses) {
        Use exact = uses.get(text);
        if (exact != null) {
            exact.references++;
        } else if (text.contains("animation.ce.") || text.contains("controller.render.ce.")) {
            Matcher matches = PRIVATE_ID.matcher(text);
            while (matches.find()) {
                Use use = uses.get(matches.group());
                if (use != null) {
                    use.references++;
                }
            }
        }
    }

    private static void shareDefinitions(JsonObject definitions, boolean animation, Map<String, Use> uses, Map<String, String> aliases) {
        Map<String, List<Shared>> groups = new HashMap<>();
        Iterator<Map.Entry<String, JsonElement>> entries = definitions.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry<String, JsonElement> entry = entries.next();
            String id = entry.getKey();
            Use use = uses.get(id);
            // 一处定义加上已识别的物品引用才是私有资源；其他入口或不同文件头下的重名定义保留独立 ID。
            if (use == null || !use.privateOwner || use.references != use.localReferences + 1 || (animation && !isStaticPose(entry.getValue()))) {
                continue;
            }
            // 序列化内容保留对象成员顺序及数字写法，避免合并顺序敏感的材质、骨骼或表达式。
            List<Shared> candidates = groups.computeIfAbsent(entry.getValue().toString(), ignored -> new ArrayList<>());
            Shared shared = null;
            for (Shared candidate : candidates) {
                if (!candidate.owners().intersects(use.owners)) {
                    shared = candidate;
                    break;
                }
            }
            if (shared == null) {
                candidates.add(new Shared(aliases.get(id), use.owners));
            } else {
                shared.owners().or(use.owners);
                aliases.put(id, shared.id());
                entries.remove();
            }
        }
    }

    private static boolean isStaticPose(JsonElement definition) {
        if (!(definition instanceof JsonObject animation) || animation.size() != 2
                || !(animation.get("loop") instanceof JsonPrimitive loop) || !loop.isBoolean() || !loop.getAsBoolean()
                || !(animation.get("bones") instanceof JsonObject bones)) {
            return false;
        }
        for (JsonElement bone : bones.asMap().values()) {
            if (!(bone instanceof JsonObject channels)) {
                return false;
            }
            for (Map.Entry<String, JsonElement> channel : channels.entrySet()) {
                String name = channel.getKey();
                if (!name.equals("position") && !name.equals("rotation") && !name.equals("scale")) {
                    return false;
                }
                JsonElement value = channel.getValue();
                if (value instanceof JsonArray vector) {
                    if (vector.size() != 3) {
                        return false;
                    }
                    for (JsonElement axis : vector) {
                        if (!axis.isJsonPrimitive() || !axis.getAsJsonPrimitive().isNumber()) {
                            return false;
                        }
                    }
                } else if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
                    return false;
                }
            }
        }
        return true;
    }

    private static final class Use {
        private final BitSet owners = new BitSet();
        private int references;
        private int localReferences;
        private boolean privateOwner;
    }

    private record Shared(String id, BitSet owners) {
    }
}
