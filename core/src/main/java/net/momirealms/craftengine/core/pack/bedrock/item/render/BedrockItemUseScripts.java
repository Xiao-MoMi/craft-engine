package net.momirealms.craftengine.core.pack.bedrock.item.render;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.util.GsonHelper;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class BedrockItemUseScripts {
    private static final String USING = "variable.ce_using_item = context.item_slot == 'main_hand' && query.is_using_item && query.main_hand_item_use_duration > 0;";
    private static final String IDLE = "variable.ce_using_item = 0;";
    static final String BLOCKING = "variable.ce_using_item = query.blocking && ((context.item_slot == 'main_hand' && !query.is_emerging) || (context.item_slot == 'off_hand' && query.is_emerging));";
    private static final String ELAPSED = "variable.ce_use_ticks = variable.ce_using_item ? math.max(0, query.main_hand_item_max_duration - query.main_hand_item_use_duration) : 0;";
    private static final String REMAINING_PREFIX = "variable.ce_remaining_ticks = variable.ce_using_item ? math.max(0, ";
    private static final String REMAINING_SUFFIX = " - variable.ce_use_ticks) : 0;";
    private static final Pattern REMAINING = Pattern.compile(Pattern.quote(REMAINING_PREFIX) + "-?[0-9]+" + Pattern.quote(REMAINING_SUFFIX));
    private static final Pattern READ = Pattern.compile("(?<![\\w.])(?:variable|v)\\.ce_(?:(using_item)|(use_ticks)|(remaining_ticks))(?!\\w)", Pattern.CASE_INSENSITIVE);

    private final Map<String, Integer> geometryReads = new HashMap<>();
    private final Map<String, Integer> animationReads = new HashMap<>();
    private final Map<String, Integer> controllerReads = new HashMap<>();

    static JsonArray create(int maximumUseTicks, boolean enabled) {
        JsonArray scripts = new JsonArray(3);
        scripts.add(enabled ? USING : IDLE);
        scripts.add(ELAPSED);
        // Geyser 的最大使用时长可能不同于 Java，剩余时间使用源物品的时长。
        scripts.add(REMAINING_PREFIX + maximumUseTicks + REMAINING_SUFFIX);
        return scripts;
    }

    public void index(String type, String id, JsonElement definition) {
        Map<String, Integer> reads = switch (type) {
            case "minecraft:geometry" -> this.geometryReads;
            case "animations" -> this.animationReads;
            case "render_controllers" -> this.controllerReads;
            default -> null;
        };
        if (reads != null) {
            reads.merge(id, readLevel(definition, null), Math::max);
        }
    }

    public void trimAttachables(Path directory) throws IOException {
        if (!Files.isDirectory(directory)) {
            return;
        }
        try (Stream<Path> files = Files.walk(directory)) {
            Iterator<Path> iterator = files.filter(path -> path.toString().endsWith(".json") && Files.isRegularFile(path)).iterator();
            while (iterator.hasNext()) {
                Path path = iterator.next();
                JsonObject document = GsonHelper.readJsonFromFile(path).getAsJsonObject();
                JsonObject description = document.getAsJsonObject("minecraft:attachable").getAsJsonObject("description");
                if (this.trimUnused(description)) {
                    GsonHelper.writeJsonFile(document, path);
                }
            }
        }
    }

    boolean trimUnused(JsonObject description) {
        JsonObject scripts = description.getAsJsonObject("scripts");
        JsonArray preAnimation = scripts == null ? null : scripts.getAsJsonArray("pre_animation");
        if (preAnimation == null || preAnimation.isEmpty() || scripts.has("parent_setup") || scripts.has("variables")) {
            return false;
        }
        int[] assignments = new int[preAnimation.size()];
        int highestAssignment = 0;
        int required = 0;
        for (int i = 0; i < assignments.length; i++) {
            JsonElement statement = preAnimation.get(i);
            int level = statement.isJsonPrimitive() ? assignmentLevel(statement.getAsString()) : 0;
            assignments[i] = level;
            highestAssignment = Math.max(highestAssignment, level);
            if (level == 0) {
                required = Math.max(required, readLevel(statement, null));
            }
        }
        // 剩余时间依赖已用时间，已用时间依赖使用状态。保留最高读取层及其所有前置赋值。
        if (required >= highestAssignment) {
            return false;
        }
        required = Math.max(required, this.resourceLevel(description));
        if (required >= highestAssignment) {
            return false;
        }
        required = Math.max(required, readLevel(description, preAnimation));
        if (required >= highestAssignment) {
            return false;
        }
        JsonArray kept = new JsonArray(preAnimation.size());
        for (int i = 0; i < assignments.length; i++) {
            if (assignments[i] <= required) {
                kept.add(preAnimation.get(i));
            }
        }
        if (kept.isEmpty()) {
            scripts.remove("pre_animation");
        } else {
            scripts.add("pre_animation", kept);
        }
        return true;
    }

    private static int assignmentLevel(String statement) {
        return switch (statement) {
            case USING, IDLE, BLOCKING -> 1;
            case ELAPSED -> 2;
            default -> statement.startsWith(REMAINING_PREFIX) && REMAINING.matcher(statement).matches() ? 3 : 0;
        };
    }

    private int resourceLevel(JsonObject description) {
        int level = Math.max(aliasLevel(description.getAsJsonObject("animations"), this.animationReads),
                aliasLevel(description.getAsJsonObject("geometry"), this.geometryReads));
        JsonArray controllers = description.getAsJsonArray("render_controllers");
        if (level == 3 || controllers == null) {
            return level;
        }
        for (JsonElement entry : controllers) {
            if (entry.isJsonObject()) {
                for (String id : entry.getAsJsonObject().keySet()) {
                    level = Math.max(level, this.controllerReads.getOrDefault(id, 3));
                }
            } else {
                level = Math.max(level, this.controllerReads.getOrDefault(entry.getAsString(), 3));
            }
            if (level == 3) {
                break;
            }
        }
        return level;
    }

    private static int aliasLevel(@Nullable JsonObject aliases, Map<String, Integer> reads) {
        if (aliases == null) {
            return 0;
        }
        int level = 0;
        for (JsonElement id : aliases.asMap().values()) {
            // 原版或外部资源的读取不可见，保留完整使用状态链。
            level = Math.max(level, reads.getOrDefault(id.getAsString(), 3));
            if (level == 3) {
                break;
            }
        }
        return level;
    }

    private static int readLevel(JsonElement value, @Nullable JsonArray preAnimation) {
        if (value == preAnimation) {
            return 0;
        }
        int level = 0;
        if (value.isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry : value.getAsJsonObject().entrySet()) {
                level = Math.max(level, Math.max(readLevel(entry.getKey()), readLevel(entry.getValue(), preAnimation)));
                if (level == 3) {
                    break;
                }
            }
        } else if (value.isJsonArray()) {
            for (JsonElement child : value.getAsJsonArray()) {
                level = Math.max(level, readLevel(child, preAnimation));
                if (level == 3) {
                    break;
                }
            }
        } else if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
            level = readLevel(value.getAsString());
        }
        return level;
    }

    private static int readLevel(String text) {
        if (text.indexOf('.') < 0) {
            return 0;
        }
        int level = 0;
        Matcher matcher = READ.matcher(text);
        while (matcher.find()) {
            if (matcher.start(3) >= 0) {
                return 3;
            }
            level = Math.max(level, matcher.start(2) >= 0 ? 2 : 1);
        }
        return level;
    }
}
