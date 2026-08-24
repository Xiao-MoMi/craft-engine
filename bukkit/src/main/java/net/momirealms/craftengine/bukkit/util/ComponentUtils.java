package net.momirealms.craftengine.bukkit.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.kyori.adventure.nbt.api.BinaryTagHolder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.DataComponentValue;
import net.kyori.adventure.text.event.HoverEvent;
import net.momirealms.craftengine.bukkit.item.BukkitItemManager;
import net.momirealms.craftengine.bukkit.plugin.network.BukkitNetworkManager;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.util.AdventureHelper;
import net.momirealms.craftengine.core.util.GsonHelper;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.proxy.adventure.text.serializer.gson.GsonComponentSerializerProxy;
import net.momirealms.craftengine.proxy.adventure.text.TextComponentProxy;
import net.momirealms.craftengine.proxy.adventure.text.TranslatableComponentProxy;
import net.momirealms.craftengine.proxy.adventure.text.TranslationArgumentProxy;
import net.momirealms.craftengine.proxy.minecraft.nbt.CompoundTagProxy;
import net.momirealms.craftengine.proxy.minecraft.nbt.IntTagProxy;
import net.momirealms.craftengine.proxy.minecraft.nbt.StringTagProxy;
import net.momirealms.craftengine.proxy.minecraft.nbt.TagParserProxy;
import net.momirealms.craftengine.proxy.minecraft.network.chat.ComponentProxy;
import net.momirealms.craftengine.proxy.minecraft.network.chat.ComponentSerializationProxy;
import net.momirealms.craftengine.proxy.minecraft.network.chat.MutableComponentProxy;
import net.momirealms.craftengine.proxy.minecraft.network.chat.contents.PlainTextContentsProxy;
import net.momirealms.craftengine.proxy.minecraft.network.chat.contents.TranslatableContentsProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.ItemStackProxy;
import net.momirealms.craftengine.proxy.paper.adventure.AdventureComponentProxy;
import net.momirealms.sparrow.nbt.CompoundTag;
import net.momirealms.sparrow.nbt.Tag;
import net.momirealms.sparrow.nbt.adventure.NBTDataComponentValue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class ComponentUtils {
    public static final Codec<Object> ComponentSerialization$CODEC = VersionHelper.isOrAbove1_20_3 ? ComponentSerializationProxy.INSTANCE.getCodec() : null;

    private ComponentUtils() {}

    public static Object adventureToMinecraft(Component component) {
        return jsonElementToMinecraft(AdventureHelper.componentToJsonElement(component));
    }

    public static Object adventureToPaperAdventure(Component component) {
        return jsonElementToPaperAdventure(AdventureHelper.componentToJsonElement(component));
    }

    public static Object jsonElementToMinecraft(JsonElement json) {
        if (VersionHelper.isOrAbove1_21_6) {
            if (json == null) return null;
            return ComponentSerialization$CODEC.parse(RegistryOps.JSON, json).getOrThrow(JsonParseException::new);
        } else if (VersionHelper.isOrAbove1_20_5) {
            return ComponentProxy.SerializerProxy.INSTANCE.fromJson(json, RegistryUtils.getRegistryAccess());
        } else {
            return ComponentProxy.SerializerProxy.INSTANCE.fromJson(json);
        }
    }

    public static Object jsonToMinecraft(String json) {
        if (VersionHelper.isOrAbove1_21_6) {
            JsonElement jsonElement = GsonHelper.get().fromJson(json, JsonElement.class);
            return ComponentSerialization$CODEC.parse(RegistryOps.JSON, jsonElement).getOrThrow(JsonParseException::new);
        } else if (VersionHelper.isOrAbove1_20_5) {
            return ComponentProxy.SerializerProxy.INSTANCE.fromJson(json, RegistryUtils.getRegistryAccess());
        } else {
            return ComponentProxy.SerializerProxy.INSTANCE.fromJson(json);
        }
    }

    public static String minecraftToJson(Object component) {
        if (VersionHelper.isOrAbove1_21_6) {
            JsonElement jsonElement = ComponentSerialization$CODEC.encodeStart(RegistryOps.JSON, component).getOrThrow(JsonParseException::new);
            return GsonHelper.get().toJson(jsonElement);
        } else if (VersionHelper.isOrAbove1_20_5) {
            return ComponentProxy.SerializerProxy.INSTANCE.toJson(component, RegistryUtils.getRegistryAccess());
        } else {
            return ComponentProxy.SerializerProxy.INSTANCE.toJson(component);
        }
    }

    public static String paperAdventureToJson(Object component) {
        return GsonComponentSerializerProxy.GSON.toJson(component);
    }

    public static Object jsonToPaperAdventure(String json) {
        return GsonComponentSerializerProxy.GSON.fromJson(json, net.momirealms.craftengine.proxy.adventure.text.ComponentProxy.CLASS);
    }

    public static JsonElement paperAdventureToJsonElement(Object component) {
        return GsonComponentSerializerProxy.GSON.toJsonTree(component);
    }

    public static Object jsonElementToPaperAdventure(JsonElement json) {
        return GsonComponentSerializerProxy.GSON.fromJson(json, net.momirealms.craftengine.proxy.adventure.text.ComponentProxy.CLASS);
    }

    public static boolean hasNetworkTag(Object component) {
        if (!MutableComponentProxy.CLASS.isInstance(component)) {
            if (!VersionHelper.hasPaperPatch || !AdventureComponentProxy.CLASS.isInstance(component)) {
                return false;
            }
            Object adventureComponent = AdventureComponentProxy.INSTANCE.adventureComponent(component);
            if (!net.momirealms.craftengine.proxy.adventure.text.ComponentProxy.CLASS.isInstance(adventureComponent)) {
                return false;
            }
            return hasPaperAdventureNetworkTag(adventureComponent);
        }

        Object contents = MutableComponentProxy.INSTANCE.getContents(component);
        if (PlainTextContentsProxy.CLASS.isInstance(contents)) {
            String text = PlainTextContentsProxy.INSTANCE.getText(contents);
            if (BukkitNetworkManager.instance().hasNetworkTag(text)) {
                return true;
            }
        } else if (TranslatableContentsProxy.CLASS.isInstance(contents)) {
            Object[] args = TranslatableContentsProxy.INSTANCE.getArgs(contents);
            for (Object arg : args) {
                if (ComponentProxy.CLASS.isInstance(arg)) {
                    if (hasNetworkTag(arg)) {
                        return true;
                    }
                }
            }
        }

        List<Object> children = MutableComponentProxy.INSTANCE.getSiblings(component);
        if (children.isEmpty()) {
            return false;
        }

        for (Object child : children) {
            if (hasNetworkTag(child)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasPaperAdventureNetworkTag(Object component) {
        if (TextComponentProxy.CLASS.isInstance(component)) {
            String text = TextComponentProxy.INSTANCE.content(component);
            if (BukkitNetworkManager.instance().hasNetworkTag(text)) {
                return true;
            }
        } else if (TranslatableComponentProxy.CLASS.isInstance(component)) {
            for (Object argument : TranslatableComponentProxy.INSTANCE.arguments(component)) {
                Object value = net.momirealms.craftengine.proxy.adventure.text.ComponentProxy.CLASS.isInstance(argument)
                        ? argument
                        : TranslationArgumentProxy.INSTANCE.value(argument);
                if (net.momirealms.craftengine.proxy.adventure.text.ComponentProxy.CLASS.isInstance(value) && hasPaperAdventureNetworkTag(value)) {
                    return true;
                }
            }
        }

        List<Object> children = net.momirealms.craftengine.proxy.adventure.text.ComponentProxy.INSTANCE.children(component);
        if (children.isEmpty()) {
            return false;
        }

        for (Object child : children) {
            if (hasPaperAdventureNetworkTag(child)) {
                return true;
            }
        }
        return false;
    }

    // 把 hover show_item 中的服务端物品重映射为客户端应显示的物品
    @SuppressWarnings("PatternValidation")
    public static HoverEvent.ShowItem replaceShowItem(HoverEvent.ShowItem showItem, BukkitServerPlayer player) {
        Object nmsItemStack;
        if (VersionHelper.COMPONENT_RELEASE) {
            CompoundTag itemTag = new CompoundTag();
            itemTag.putInt("count", showItem.count());
            itemTag.putString("id", showItem.item().asMinimalString());
            Map<net.kyori.adventure.key.Key, DataComponentValue> components = showItem.dataComponents();
            if (!components.isEmpty()) {
                CompoundTag componentsTag = new CompoundTag();
                Map<net.kyori.adventure.key.Key, NBTDataComponentValue> componentsMap = showItem.dataComponentsAs(NBTDataComponentValue.class);
                for (Map.Entry<net.kyori.adventure.key.Key, NBTDataComponentValue> entry : componentsMap.entrySet()) {
                    componentsTag.put(entry.getKey().asMinimalString(), entry.getValue().tag());
                }
                itemTag.put("components", componentsTag);
            }
            DataResult<Object> nmsItemStackResult = ItemStackProxy.INSTANCE.getCodec().parse(RegistryOps.SPARROW_NBT, itemTag);
            Optional<Object> result = nmsItemStackResult.result();
            if (result.isEmpty()) {
                return showItem;
            }
            nmsItemStack = result.get();
        } else {
            Object compoundTag = CompoundTagProxy.INSTANCE.newInstance();
            CompoundTagProxy.INSTANCE.put(compoundTag, "Count", IntTagProxy.INSTANCE.valueOf(showItem.count()));
            CompoundTagProxy.INSTANCE.put(compoundTag, "id", StringTagProxy.INSTANCE.valueOf(showItem.item().asMinimalString()));
            BinaryTagHolder nbt = showItem.nbt();
            if (nbt != null) {
                try {
                    Object nmsTag = TagParserProxy.INSTANCE.parseCompoundFully(nbt.string());
                    CompoundTagProxy.INSTANCE.put(compoundTag, "tag", nmsTag);
                } catch (CommandSyntaxException ignored) {
                    return showItem;
                }
            }
            nmsItemStack = ItemStackProxy.INSTANCE.of(compoundTag);
        }

        BukkitItemManager itemManager = BukkitItemManager.instance();
        Item wrap = itemManager.wrap(ItemStackUtils.getBukkitStack(nmsItemStack));
        Optional<Item> remapped = itemManager.s2c(wrap, player);
        if (remapped.isEmpty()) {
            return showItem;
        }

        Item clientBoundItem = remapped.get();
        net.kyori.adventure.key.Key id = KeyUtils.toAdventureKey(clientBoundItem.vanillaId());
        int count = clientBoundItem.count();
        if (VersionHelper.COMPONENT_RELEASE) {
            DataResult<Tag> tagDataResult = ItemStackProxy.INSTANCE.getCodec().encodeStart(RegistryOps.SPARROW_NBT, clientBoundItem.minecraftItem());
            Optional<Tag> result = tagDataResult.result();
            if (result.isEmpty()) {
                return showItem;
            }
            CompoundTag itemTag = (CompoundTag) result.get();
            CompoundTag componentsTag = itemTag.getCompound("components");
            if (componentsTag != null) {
                Map<net.kyori.adventure.key.Key, NBTDataComponentValue> componentsMap = new HashMap<>();
                for (Map.Entry<String, Tag> entry : componentsTag.entrySet()) {
                    componentsMap.put(net.kyori.adventure.key.Key.key(entry.getKey()), NBTDataComponentValue.of(entry.getValue()));
                }
                return HoverEvent.ShowItem.showItem(id, count, componentsMap);
            } else {
                return HoverEvent.ShowItem.showItem(id, count);
            }
        } else {
            Object tag = ItemStackProxy.INSTANCE.getTag(clientBoundItem.minecraftItem());
            if (tag != null) {
                return HoverEvent.ShowItem.showItem(id, count, BinaryTagHolder.binaryTagHolder(tag.toString()));
            } else {
                return HoverEvent.ShowItem.showItem(id, count);
            }
        }
    }
}
