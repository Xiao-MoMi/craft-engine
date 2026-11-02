package net.momirealms.craftengine.core.pack.bedrock.item;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.bedrock.BedrockPackLayout;
import net.momirealms.craftengine.core.item.ItemKeys;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModelResolver;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModelResources;
import net.momirealms.craftengine.core.pack.bedrock.texture.JavaPackTextureSource;
import net.momirealms.craftengine.core.util.GsonHelper;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourcePackUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BedrockItemResourceGenerator {
    private BedrockItemResourceGenerator() {
    }

    public static Result write(JavaModelResources resources, ItemRegistrationPlan plan, Path root) throws IOException {
        BedrockItemIconRenderer renderer = new BedrockItemIconRenderer(resources);
        BedrockDefaultItemHandler defaultHandler = new BedrockDefaultItemHandler(resources, renderer);
        Map<Key, List<BedrockItemHandler>> handlers = createHandlers(resources, renderer);
        BedrockItemGenerationContext context = new BedrockItemGenerationContext(root);

        Map<Key, List<GeneratedItemVariant>> items = new LinkedHashMap<>();
        for (ItemRegistrationPlan.Entry entry : plan.entries()) {
            items.put(entry.bedrockId(), writeItem(entry, context, handlers.getOrDefault(entry.javaItemType(), List.of()), defaultHandler));
        }
        writeSharedResources(context, items);
        return new Result(items);
    }

    private static List<GeneratedItemVariant> writeItem(ItemRegistrationPlan.Entry entry,
                                                        BedrockItemGenerationContext context,
                                                        List<BedrockItemHandler> handlers,
                                                        BedrockDefaultItemHandler defaultHandler) throws IOException {
        try {
            for (BedrockItemHandler handler : handlers) {
                List<GeneratedItemVariant> variants = handler.write(entry, context);
                if (variants != null) {
                    return variants;
                }
            }
            return defaultHandler.write(entry, context);
        } catch (IOException | RuntimeException exception) {
            throw new IOException("Failed to generate item resources for " + entry.definitionId() + " at " + ResourcePackUtils.itemDefinitionPath(entry.definitionId()) + ": " + exception.getMessage(), exception);
        }
    }

    private static void writeSharedResources(BedrockItemGenerationContext context, Map<Key, List<GeneratedItemVariant>> items) throws IOException {
        Path root = context.root();
        // 所有变体生成后，将图标及其染色遮罩的纹理路径汇总到 textures/item_texture.json。
        writeItemAtlas(root, items);
        // 有物品使用展示方块时，写入 blocks.json，并将方块贴图和动画播放配置分别登记到
        // textures/terrain_texture.json 和 textures/flipbook_textures.json。
        context.blocks().finish(root);

        // 各写入器按需修改同一份玩家控制器，所有物品处理完毕后再写盘。
        BedrockPlayerAnimationController controller = new BedrockPlayerAnimationController(root);
        // 弩：按装填时长生成装填和持弩动作，写入 crossbow_player.animation.json 及素材来源声明。
        context.crossbowPlayerAnimations().write(controller, root);
        // 矛：为使用 Java 持矛姿态的物品写入 spear_player.animation.json。
        BedrockSpearPlayerAnimations.write(context.javaSpears(), controller, root);
        // 盾：为使用 Java 手持姿态的盾写入主手和副手的格挡动作 shield_player.animation.json。
        BedrockShieldPlayerAnimations.write(context.javaShields(), controller, root);
        // 弓：所有自定义弓都加入举弓条件，只有使用 Java 手持姿态的弓才需要 bow_player.animation.json。
        BedrockBowPlayerAnimations.write(context.bows(), context.javaBows(), controller, root);
        // 挥动：为需要 Java 攻击动作的物品写入 swing_player.animation.json。
        BedrockSwingPlayerAnimations.write(context.javaSwings(), controller, root);
        controller.write();
    }

    private static void writeItemAtlas(Path root, Map<Key, List<GeneratedItemVariant>> items) throws IOException {
        JsonObject icons = new JsonObject();
        for (List<GeneratedItemVariant> variants : items.values()) {
            for (GeneratedItemVariant variant : variants) {
                if (variant.iconTexture() != null) {
                    JsonObject icon = new JsonObject();
                    icon.addProperty("textures", variant.iconTexture());
                    icons.add(variant.id().asString(), icon);
                    if (variant.dyeColor() != null) {
                        JsonObject mask = new JsonObject();
                        mask.addProperty("textures", variant.iconTexture() + "_dyed.tga");
                        icons.add(BedrockPackLayout.dyedIconKey(variant.id().asString()), mask);
                    }
                }
            }
        }
        JsonObject atlas = new JsonObject();
        atlas.addProperty("resource_pack_name", "craftengine");
        atlas.addProperty("texture_name", "atlas.items");
        atlas.add("texture_data", icons);
        Path output = root.resolve(BedrockPackLayout.ITEM_TEXTURE_ATLAS);
        Files.createDirectories(output.getParent());
        GsonHelper.writeJsonFile(GsonHelper.sortObjectKeys(atlas), output);
    }

    private static Map<Key, List<BedrockItemHandler>> createHandlers(JavaModelResources resources, BedrockItemIconRenderer renderer) {
        JavaModelResolver models = resources.models();
        JavaPackTextureSource textures = resources.textures();
        Map<Key, List<BedrockItemHandler>> handlers = new HashMap<>();
        handlers.put(ItemKeys.BOW, List.of(new Bedrock2DBowItemHandler(models, renderer), new Bedrock3DBowItemHandler(models, textures)));
        handlers.put(ItemKeys.CROSSBOW, List.of(new Bedrock2DCrossbowItemHandler(models, renderer), new Bedrock3DCrossbowItemHandler(models, textures)));
        handlers.put(ItemKeys.SHIELD, List.of(new Bedrock3DShieldItemHandler(resources)));
        handlers.put(ItemKeys.TRIDENT, List.of(new BedrockTridentItemHandler(resources, renderer)));
        List<BedrockItemHandler> spears = List.of(new Bedrock2DSpearItemHandler(renderer), new Bedrock3DSpearItemHandler(models, textures));
        for (Key spear : ItemKeys.SPEARS) {
            handlers.put(spear, spears);
        }
        return handlers;
    }

    public record Result(Map<Key, List<GeneratedItemVariant>> variants) {
    }
}
