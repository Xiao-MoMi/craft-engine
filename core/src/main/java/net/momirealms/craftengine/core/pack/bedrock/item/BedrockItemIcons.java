package net.momirealms.craftengine.core.pack.bedrock.item;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.bedrock.BedrockPackLayout;
import net.momirealms.craftengine.core.item.ItemKeys;
import net.momirealms.craftengine.core.pack.bedrock.JavaResourcePack;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModelResolver;
import net.momirealms.craftengine.core.pack.bedrock.texture.JavaPackTextureSource;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.GsonHelper;
import net.momirealms.craftengine.core.util.ResourcePackUtils;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class BedrockItemIcons {
    private final BedrockItemHandler defaultHandler;
    private final Map<Key, List<BedrockItemHandler>> handlers = new HashMap<>();

    public BedrockItemIcons(JavaResourcePack pack) {
        JavaPackTextureSource textures = new JavaPackTextureSource(pack);
        JavaModelResolver models = new JavaModelResolver(pack, textures);
        BedrockItemIconRenderer renderer = new BedrockItemIconRenderer(models, textures);
        this.defaultHandler = new BedrockDefaultItemHandler(models, textures, renderer);
        this.handlers.put(ItemKeys.BOW, List.of(new Bedrock2DBowItemHandler(models, renderer), new Bedrock3DBowItemHandler(models, textures)));
        this.handlers.put(ItemKeys.CROSSBOW, List.of(new Bedrock2DCrossbowItemHandler(models, renderer), new Bedrock3DCrossbowItemHandler(models, textures)));
        this.handlers.put(ItemKeys.SHIELD, List.of(new Bedrock3DShieldItemHandler(models, textures)));
        List<BedrockItemHandler> spears = List.of(new Bedrock2DSpearItemHandler(renderer), new Bedrock3DSpearItemHandler(models, textures));
        for (Key spear : ItemKeys.SPEARS) {
            this.handlers.put(spear, spears);
        }
    }

    public Result write(ItemRegistrationPlan plan, Path root) throws IOException {
        BedrockItemGenerationContext context = new BedrockItemGenerationContext(root);
        for (ItemRegistrationPlan.Entry entry : plan.entries()) {
            try {
                boolean handled = false;
                for (BedrockItemHandler handler : this.handlers.getOrDefault(entry.javaItemType(), List.of())) {
                    if (handler.write(entry, context)) {
                        handled = true;
                        break;
                    }
                }
                if (!handled) {
                    this.defaultHandler.write(entry, context);
                }
            } catch (IOException | RuntimeException exception) { // 读取或生成失败时补充定义 ID、路径并终止
                throw new IOException("Failed to generate icon for " + entry.definitionId() + " at " + ResourcePackUtils.itemDefinitionPath(entry.definitionId()) + ": " + exception.getMessage(), exception);
            }
        }
        JsonObject atlas = new JsonObject(); // 汇总写出物品图集
        atlas.addProperty("resource_pack_name", "craftengine");
        atlas.addProperty("texture_name", "atlas.items");
        atlas.add("texture_data", context.icons());
        Path output = root.resolve(BedrockPackLayout.ITEM_TEXTURE_ATLAS);
        Files.createDirectories(output.getParent());
        GsonHelper.writeJsonFile(GsonHelper.sortObjectKeys(atlas), output);
        context.blocks().finish(root); // 汇总写出方块图集、flipbook 和方块定义
        context.crossbowPlayerAnimations().write(root);
        BedrockSpearPlayerAnimations.write(context.javaSpears(), root);
        BedrockShieldPlayerAnimations.write(context.javaShields(), root);
        BedrockBowPlayerAnimations.write(context.bows(), context.javaBows(), root);
        return new Result(context.icons().keySet(), context.displays(), context.variants(), context.attachables()); // 供注册计划关联已生成资源与分支条件
    }

    public record Result(Set<String> icons, Map<Key, BedrockItemBlocks.DisplayBlock> displayBlocks, Map<Key, List<Variant>> variants, Set<Key> attachables) {
    }

    public record Variant(Key id, ItemBindingRule rule, @Nullable String unsupportedReason) {
    }
}
