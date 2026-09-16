package net.momirealms.craftengine.bukkit.compatibility.denizen.tags;

import com.denizenscript.denizen.objects.EntityTag;
import com.denizenscript.denizen.objects.ItemTag;
import com.denizenscript.denizen.objects.LocationTag;
import com.denizenscript.denizen.objects.PlayerTag;
import com.denizenscript.denizen.tags.BukkitTagContext;
import com.denizenscript.denizen.tags.core.ServerTagBase;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.core.ListTag;
import com.denizenscript.denizencore.objects.core.MapTag;
import com.denizenscript.denizencore.tags.TagManager;
import net.momirealms.craftengine.bukkit.api.BukkitAdaptor;
import net.momirealms.craftengine.bukkit.api.CraftEngineBlocks;
import net.momirealms.craftengine.bukkit.api.CraftEngineFurniture;
import net.momirealms.craftengine.bukkit.api.CraftEngineItems;
import net.momirealms.craftengine.bukkit.compatibility.util.FurnitureResolver;
import net.momirealms.craftengine.bukkit.entity.furniture.BukkitFurniture;
import net.momirealms.craftengine.bukkit.item.BukkitItemDefinition;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.core.block.BlockDefinition;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.UpdateFlags;
import net.momirealms.craftengine.core.block.property.Property;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.sparrow.nbt.CompoundTag;
import net.momirealms.sparrow.nbt.Tag;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

/**
 * Registers CraftEngine tags and mechanisms onto Denizen object types.
 */
public final class CraftEngineTags {
    private CraftEngineTags() {}

    public static void register() {
        // <--[tag]
        // @attribute <ItemTag.is_ce_item>
        // @returns ElementTag(Boolean)
        // @plugin CraftEngine
        // @description
        // Returns whether the item is a CraftEngine custom item.
        // @Example
        // - if <player.item_in_hand.is_ce_item>:
        //     - narrate "You are holding a CraftEngine item!"
        // -->
        ItemTag.tagProcessor.registerTag(ElementTag.class, "is_ce_item", (attribute, object) -> {
            return new ElementTag(CraftEngineItems.isCustomItem(object.getItemStack()));
        });

        // <--[tag]
        // @attribute <ItemTag.ce_id>
        // @returns ElementTag
        // @plugin CraftEngine
        // @description
        // Returns the CraftEngine item id of the item, like "default:topaz_rod".
        // Returns nothing if the item is not a CraftEngine custom item.
        // @Example
        // - narrate "You are holding <player.item_in_hand.ce_id>!"
        // -->
        ItemTag.tagProcessor.registerTag(ElementTag.class, "ce_id", (attribute, object) -> {
            Key id = CraftEngineItems.getCustomItemId(object.getItemStack());
            if (id == null) {
                return null;
            }
            return new ElementTag(id.toString());
        });

        // <--[tag]
        // @attribute <LocationTag.is_ce_block>
        // @returns ElementTag(Boolean)
        // @plugin CraftEngine
        // @description
        // Returns whether the block at the location is a CraftEngine custom block.
        // @Example
        // - if <player.cursor_on.is_ce_block>:
        //     - narrate "You are looking at a CraftEngine block!"
        // -->
        LocationTag.tagProcessor.registerTag(ElementTag.class, "is_ce_block", (attribute, object) -> {
            return new ElementTag(CraftEngineBlocks.isCustomBlock(object.getBlock()));
        });

        // <--[tag]
        // @attribute <LocationTag.ce_id>
        // @returns ElementTag
        // @plugin CraftEngine
        // @description
        // Returns the CraftEngine block id of the block at the location, like "default:togglable_light_block".
        // Returns nothing if the block is not a CraftEngine custom block.
        // @Example
        // - narrate "You are looking at <player.cursor_on.ce_id>!"
        // -->
        LocationTag.tagProcessor.registerTag(ElementTag.class, "ce_id", (attribute, object) -> {
            ImmutableBlockState state = CraftEngineBlocks.getCustomBlockState(object.getBlock());
            if (state == null) {
                return null;
            }
            return new ElementTag(state.owner().value().id().toString());
        });

        // <--[tag]
        // @attribute <LocationTag.ce_block_states>
        // @returns MapTag
        // @plugin CraftEngine
        // @mechanism LocationTag.ce_block_state
        // @description
        // Returns a map of all block state properties of the CraftEngine custom block at the location.
        // Returns nothing if the block is not a CraftEngine custom block.
        // @Example
        // - narrate "The block you are looking at has light level <player.cursor_on.ce_block_states.get[light]>."
        // -->
        LocationTag.tagProcessor.registerTag(MapTag.class, "ce_block_states", (attribute, object) -> {
            ImmutableBlockState state = CraftEngineBlocks.getCustomBlockState(object.getBlock());
            if (state == null) {
                return null;
            }
            MapTag map = new MapTag();
            for (Property<?> property : state.getProperties()) {
                map.putObject(property.name(), new ElementTag(Property.formatValue(property, state.get(property))));
            }
            return map;
        });

        // <--[mechanism]
        // @object LocationTag
        // @name ce_block_state
        // @group CraftEngine
        // @input ElementTag
        // @tags
        // <LocationTag.ce_block_states>
        // @description
        // Sets a block state property of the CraftEngine custom block at the location, in the format "key=value".
        // @Example
        // - adjust <player.cursor_on> ce_block_state:light=10
        // -->
        LocationTag.tagProcessor.registerMechanism("ce_block_state", false, ElementTag.class, (object, mechanism, input) -> {
            ImmutableBlockState state = CraftEngineBlocks.getCustomBlockState(object.getBlock());
            if (state == null) {
                mechanism.echoError("Block at location is not a CraftEngine custom block.");
                return;
            }
            String raw = input.asString();
            int split = raw.indexOf('=');
            if (split <= 0) {
                mechanism.echoError("Invalid ce_block_state input '" + raw + "', expected format 'key=value'.");
                return;
            }
            String name = raw.substring(0, split);
            String value = raw.substring(split + 1);
            Property<?> property = state.getProperty(name);
            if (property == null) {
                mechanism.echoError("CraftEngine block '" + state.owner().value().id() + "' has no block state property named '" + name + "'.");
                return;
            }
            Optional<Tag> tag = property.createOptionalTag(value);
            if (tag.isEmpty()) {
                mechanism.echoError("Invalid value '" + value + "' for block state property '" + name + "'.");
                return;
            }
            CompoundTag properties = new CompoundTag();
            properties.put(name, tag.get());
            CraftEngineBlocks.place(object, state.with(properties), UpdateFlags.UPDATE_ALL, false);
        });

        // <--[tag]
        // @attribute <EntityTag.is_ce_furniture>
        // @returns ElementTag(Boolean)
        // @plugin CraftEngine
        // @description
        // Returns whether the entity is part of a CraftEngine furniture (base entity, seat, or collider).
        // @Example
        // - if <player.precise_target.is_ce_furniture.if_null[false]>:
        //     - narrate "You are looking at a CraftEngine furniture!"
        // -->
        EntityTag.tagProcessor.registerTag(ElementTag.class, "is_ce_furniture", (attribute, object) -> {
            return new ElementTag(FurnitureResolver.resolve(object.getBukkitEntity()) != null);
        });

        // <--[tag]
        // @attribute <EntityTag.is_ce_furniture_seat>
        // @returns ElementTag(Boolean)
        // @plugin CraftEngine
        // @description
        // Returns whether the entity is a seat of a CraftEngine furniture.
        // -->
        EntityTag.tagProcessor.registerTag(ElementTag.class, "is_ce_furniture_seat", (attribute, object) -> {
            return new ElementTag(CraftEngineFurniture.isSeat(object.getBukkitEntity()));
        });

        // <--[tag]
        // @attribute <EntityTag.is_ce_furniture_collider>
        // @returns ElementTag(Boolean)
        // @plugin CraftEngine
        // @description
        // Returns whether the entity is a collision entity of a CraftEngine furniture.
        // -->
        EntityTag.tagProcessor.registerTag(ElementTag.class, "is_ce_furniture_collider", (attribute, object) -> {
            return new ElementTag(CraftEngineFurniture.isCollisionEntity(object.getBukkitEntity()));
        });

        // <--[tag]
        // @attribute <EntityTag.ce_id>
        // @returns ElementTag
        // @plugin CraftEngine
        // @description
        // Returns the CraftEngine furniture id of the entity, like "default:wooden_chair".
        // Returns nothing if the entity is not part of a CraftEngine furniture.
        // @Example
        // - narrate "You are looking at <player.precise_target.ce_id>!"
        // -->
        EntityTag.tagProcessor.registerTag(ElementTag.class, "ce_id", (attribute, object) -> {
            BukkitFurniture furniture = FurnitureResolver.resolve(object.getBukkitEntity());
            if (furniture == null) {
                return null;
            }
            return new ElementTag(furniture.id().toString());
        });

        // <--[tag]
        // @attribute <EntityTag.ce_furniture_variant>
        // @returns ElementTag
        // @plugin CraftEngine
        // @mechanism EntityTag.ce_furniture_variant
        // @description
        // Returns the current variant name of the CraftEngine furniture.
        // Returns nothing if the entity is not part of a CraftEngine furniture.
        // @Example
        // - narrate "The chair's variant is <player.precise_target.ce_furniture_variant>."
        // -->
        EntityTag.tagProcessor.registerTag(ElementTag.class, "ce_furniture_variant", (attribute, object) -> {
            BukkitFurniture furniture = FurnitureResolver.resolve(object.getBukkitEntity());
            if (furniture == null) {
                return null;
            }
            return new ElementTag(furniture.currentVariant().name());
        });

        // <--[tag]
        // @attribute <EntityTag.ce_furniture_variants>
        // @returns ListTag
        // @plugin CraftEngine
        // @description
        // Returns a list of all variant names of the CraftEngine furniture.
        // Returns nothing if the entity is not part of a CraftEngine furniture.
        // -->
        EntityTag.tagProcessor.registerTag(ListTag.class, "ce_furniture_variants", (attribute, object) -> {
            BukkitFurniture furniture = FurnitureResolver.resolve(object.getBukkitEntity());
            if (furniture == null) {
                return null;
            }
            return new ListTag(furniture.config.variants().keySet());
        });

        // <--[mechanism]
        // @object EntityTag
        // @name ce_furniture_variant
        // @group CraftEngine
        // @input ElementTag
        // @tags
        // <EntityTag.ce_furniture_variant>
        // @description
        // Sets the variant of the CraftEngine furniture. Does nothing if the variant doesn't exist or is blocked.
        // @Example
        // - adjust <player.precise_target> ce_furniture_variant:oak
        // -->
        EntityTag.tagProcessor.registerMechanism("ce_furniture_variant", false, ElementTag.class, (object, mechanism, input) -> {
            BukkitFurniture furniture = FurnitureResolver.resolve(object.getBukkitEntity());
            if (furniture == null) {
                mechanism.echoError("Entity is not part of a CraftEngine furniture.");
                return;
            }
            furniture.setVariant(input.asString(), false);
        });

        // <--[tag]
        // @attribute <server.ce_block_ids>
        // @returns ListTag
        // @plugin CraftEngine
        // @description
        // Returns a list of all loaded CraftEngine block ids.
        // -->
        ServerTagBase.instance.tagProcessor.registerTag(ListTag.class, "ce_block_ids", (attribute, object) -> {
            ListTag list = new ListTag();
            for (Key key : CraftEngineBlocks.loadedBlocks().keySet()) {
                list.add(key.toString());
            }
            return list;
        });

        // <--[tag]
        // @attribute <server.ce_item_ids>
        // @returns ListTag
        // @plugin CraftEngine
        // @description
        // Returns a list of all loaded CraftEngine item ids.
        // -->
        ServerTagBase.instance.tagProcessor.registerTag(ListTag.class, "ce_item_ids", (attribute, object) -> {
            ListTag list = new ListTag();
            for (Key key : CraftEngineItems.loadedItems().keySet()) {
                list.add(key.toString());
            }
            return list;
        });

        // <--[tag]
        // @attribute <server.ce_furniture_ids>
        // @returns ListTag
        // @plugin CraftEngine
        // @description
        // Returns a list of all loaded CraftEngine furniture ids.
        // -->
        ServerTagBase.instance.tagProcessor.registerTag(ListTag.class, "ce_furniture_ids", (attribute, object) -> {
            ListTag list = new ListTag();
            for (Key key : CraftEngineFurniture.loadedFurniture().keySet()) {
                list.add(key.toString());
            }
            return list;
        });

        // <--[tag]
        // @attribute <ce_item[<item_id>]>
        // @returns ItemTag
        // @plugin CraftEngine
        // @description
        // Returns a CraftEngine custom item object constructed from the input id, like "default:topaz_rod".
        // @Example
        // - give <ce_item[default:topaz_rod]>
        // -->
        TagManager.registerTagHandler(ItemTag.class, "ce_item", (attribute) -> {
            if (!attribute.hasParam()) {
                attribute.echoError("Missing item id for ce_item tag.");
                return null;
            }
            BukkitItemDefinition definition = CraftEngineItems.byId(attribute.getParam());
            if (definition == null) {
                attribute.echoError("Invalid CraftEngine item id '" + attribute.getParam() + "'.");
                return null;
            }
            BukkitServerPlayer player = null;
            if (attribute.context instanceof BukkitTagContext context) {
                PlayerTag playerTag = context.player;
                if (playerTag != null && playerTag.isOnline()) {
                    player = BukkitAdaptor.adapt(playerTag.getPlayerEntity());
                }
            }
            ItemStack itemStack = definition.buildBukkitItem(ItemBuildContext.of(player));
            return new ItemTag(itemStack);
        });

        // <--[tag]
        // @attribute <ce_possible_states[<block_id>]>
        // @returns ListTag
        // @plugin CraftEngine
        // @description
        // Returns a list of all possible block states of a CraftEngine block id,
        // in the form directly usable by the placeceblock command, like "default:togglable_light_block[light=15]".
        // @Example
        // - foreach <ce_possible_states[default:togglable_light_block]> as:state:
        //     - narrate "<[state]>"
        // -->
        TagManager.registerTagHandler(ListTag.class, "ce_possible_states", (attribute) -> {
            if (!attribute.hasParam()) {
                attribute.echoError("Missing block id for ce_possible_states tag.");
                return null;
            }
            BlockDefinition definition = CraftEngineBlocks.byId(Key.of(attribute.getParam()));
            if (definition == null) {
                attribute.echoError("Invalid CraftEngine block id '" + attribute.getParam() + "'.");
                return null;
            }
            ListTag list = new ListTag();
            for (ImmutableBlockState state : definition.getPossibleStates(new CompoundTag())) {
                list.add(state.toString());
            }
            return list;
        });
    }
}
