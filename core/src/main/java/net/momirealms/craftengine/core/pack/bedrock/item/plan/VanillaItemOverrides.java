package net.momirealms.craftengine.core.pack.bedrock.item.plan;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.network.ItemModelMappings;
import net.momirealms.craftengine.core.item.network.ItemPacketSource;
import net.momirealms.craftengine.core.pack.bedrock.JavaResourcePack;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModelResolver;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModelResources;
import net.momirealms.craftengine.core.plugin.locale.TranslationManager;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourcePackUtils;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

final class VanillaItemOverrides {
    private VanillaItemOverrides() {
    }

    static List<ResolvedItemBinding> plan(List<Key> vanillaItems, Function<Key, Item> itemFactory,
                                          ItemBindingResolver resolver, JavaModelResources resources, ItemRegistrationIndex index) {
        if (vanillaItems.isEmpty()) {
            return List.of();
        }
        List<ResolvedItemBinding> overrides = new ArrayList<>();
        Map<Key, Key> mappedModels = ItemModelMappings.getMappings();
        for (Key itemId : vanillaItems) {
            try {
                Item item = itemFactory.apply(itemId);
                if (item == null) {
                    continue;
                }
                Key vanillaModel = item.itemModel().map(Key::of).orElse(null);
                if (vanillaModel == null) {
                    continue;
                }
                Key mappedModel = mappedModels.get(vanillaModel);
                JsonObject definition = mappedModel == null ? changedDefinition(vanillaModel, vanillaModel, resources) : null;
                if (definition == null && mappedModel == null) {
                    continue;
                }

                Item clientItem = item.copy().toClientSide(null, ItemPacketSource.GENERIC);
                Key clientModel = clientItem.itemModel().map(Key::of).orElse(null);
                if (clientModel == null) {
                    continue;
                }
                if (mappedModel != null) {
                    definition = changedDefinition(vanillaModel, clientModel, resources);
                    if (definition == null) {
                        continue;
                    }
                }
                if (index.containsModel(clientItem.vanillaId(), clientModel)) {
                    TranslationManager.warn("resource_pack.bedrock.item.vanilla_occupied", itemId.asString(), clientModel.asString());
                    continue;
                }

                ResolvedItemBinding binding = resolver.resolve(item, clientItem, clientModel, ItemModelBindingCompiler.compile(clientModel, definition));
                if (!index.add(binding)) {
                    TranslationManager.warn("resource_pack.bedrock.item.vanilla_occupied", itemId.asString(), clientModel.asString());
                    continue;
                }
                overrides.add(binding);
            } catch (IOException | RuntimeException exception) {
                TranslationManager.warn("resource_pack.bedrock.item.binding", itemId.asString(), itemId.asString(), exception.getLocalizedMessage());
            }
        }
        return overrides;
    }

    private static @Nullable JsonObject changedDefinition(Key vanillaModel, Key clientModel, JavaModelResources resources) throws IOException {
        JavaResourcePack pack = resources.pack();
        String path = ResourcePackUtils.itemDefinitionPath(vanillaModel);
        JsonObject vanilla = pack.readVanillaJson(path);
        // Java 保护包可能把已覆盖的 items 定义移到了客户端收到的模型 ID
        boolean custom = false;
        if (!vanillaModel.equals(clientModel)) {
            String clientPath = ResourcePackUtils.itemDefinitionPath(clientModel);
            if (pack.hasCustomResource(clientPath)) {
                path = clientPath;
                custom = true;
            }
        }
        custom = custom || pack.hasCustomResource(path);
        if (!custom && pack.filtersVanilla(path)) {
            return null;
        }
        JsonObject definition = custom ? pack.readJson(path) : vanilla;
        if (definition == null) {
            return null;
        }
        // items 定义可以沿用原版文件，任意分支的模型或父模型变化也会触发转换
        if (!Objects.equals(definition, vanilla) || hasChangedReference(definition.get("model"), resources.models())) {
            return definition;
        }
        return null;
    }

    private static boolean hasChangedReference(@Nullable JsonElement node, JavaModelResolver models) throws IOException {
        if (node instanceof JsonObject object) {
            if (object.get("model") instanceof JsonPrimitive model && model.isString()) {
                return models.hasChangedModel(Key.of(model.getAsString()));
            }
            if (object.get("base") instanceof JsonPrimitive base && base.isString()) {
                return models.hasChangedModel(Key.of(base.getAsString()));
            }
            for (JsonElement child : object.asMap().values()) {
                if (hasChangedReference(child, models)) {
                    return true;
                }
            }
        } else if (node instanceof JsonArray array) {
            for (JsonElement child : array) {
                if (hasChangedReference(child, models)) {
                    return true;
                }
            }
        }
        return false;
    }
}
