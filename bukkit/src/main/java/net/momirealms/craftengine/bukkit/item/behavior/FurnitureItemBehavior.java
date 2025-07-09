package net.momirealms.craftengine.bukkit.item.behavior;

import net.momirealms.craftengine.bukkit.api.event.FurnitureAttemptPlaceEvent;
import net.momirealms.craftengine.bukkit.api.event.FurniturePlaceEvent;
import net.momirealms.craftengine.bukkit.entity.furniture.BukkitFurniture;
import net.momirealms.craftengine.bukkit.entity.furniture.BukkitFurnitureManager;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.util.DirectionUtils;
import net.momirealms.craftengine.bukkit.util.EventUtils;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.core.entity.furniture.AnchorType;
import net.momirealms.craftengine.core.entity.furniture.CustomFurniture;
import net.momirealms.craftengine.core.entity.furniture.FurnitureExtraData;
import net.momirealms.craftengine.core.entity.furniture.HitBox;
import net.momirealms.craftengine.core.entity.player.InteractionResult;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.behavior.ItemBehavior;
import net.momirealms.craftengine.core.item.behavior.ItemBehaviorFactory;
import net.momirealms.craftengine.core.item.context.UseOnContext;
import net.momirealms.craftengine.core.pack.Pack;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.context.ContextHolder;
import net.momirealms.craftengine.core.plugin.context.PlayerOptionalContext;
import net.momirealms.craftengine.core.plugin.context.event.EventTrigger;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.plugin.locale.LocalizedResourceConfigException;
import net.momirealms.craftengine.core.util.*;
import net.momirealms.craftengine.core.world.Vec3d;
import net.momirealms.craftengine.core.world.collision.AABB;
import org.bukkit.Location;
import org.bukkit.World;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class FurnitureItemBehavior extends ItemBehavior {
    public static final Factory FACTORY = new Factory();
    private final Key id;

    public FurnitureItemBehavior(Key id) {
        this.id = id;
    }

    public Key furnitureId() {
        return id;
    }

    @Override
    public InteractionResult useOnBlock(UseOnContext context) {
        return this.place(context);
    }

    public InteractionResult place(UseOnContext context) {
        Optional<CustomFurniture> optionalCustomFurniture = BukkitFurnitureManager.instance().furnitureById(this.id);
        if (optionalCustomFurniture.isEmpty()) {
            CraftEngine.instance().logger().warn("Furniture " + this.id + " not found");
            return InteractionResult.FAIL;
        }
        CustomFurniture customFurniture = optionalCustomFurniture.get();

        Direction clickedFace = context.getClickedFace();
        AnchorType anchorType = switch (clickedFace) {
            case EAST, WEST, NORTH, SOUTH -> AnchorType.WALL;
            case UP -> AnchorType.GROUND;
            case DOWN -> AnchorType.CEILING;
        };

        CustomFurniture.Placement placement = customFurniture.getPlacement(anchorType);
        if (placement == null) {
            return InteractionResult.FAIL;
        }

        Player player = context.getPlayer();
        // todo adventure check
        if (player.isAdventureMode()) {
            return InteractionResult.FAIL;
        }

        Vec3d clickedPosition = context.getClickLocation();

        // trigger event
        org.bukkit.entity.Player bukkitPlayer = (org.bukkit.entity.Player) player.platformPlayer();
        World world = (World) context.getLevel().platformWorld();

        // get position and rotation for placement
        Vec3d finalPlacePosition;
        double furnitureYaw;
        if (anchorType == AnchorType.WALL) {
            furnitureYaw = Direction.getYaw(clickedFace);
            if (clickedFace == Direction.EAST || clickedFace == Direction.WEST) {
                Pair<Double, Double> xz = placement.alignmentRule().apply(Pair.of(clickedPosition.y(), clickedPosition.z()));
                finalPlacePosition = new Vec3d(clickedPosition.x(), xz.left(), xz.right());
            } else {
                Pair<Double, Double> xz = placement.alignmentRule().apply(Pair.of(clickedPosition.x(), clickedPosition.y()));
                finalPlacePosition = new Vec3d(xz.left(), xz.right(), clickedPosition.z());
            }
        } else {
            furnitureYaw = placement.rotationRule().apply(180 + player.xRot());
            Pair<Double, Double> xz = placement.alignmentRule().apply(Pair.of(clickedPosition.x(), clickedPosition.z()));
            finalPlacePosition = new Vec3d(xz.left(), clickedPosition.y(), xz.right());
        }

        Location furnitureLocation = new Location(world, finalPlacePosition.x(), finalPlacePosition.y(), finalPlacePosition.z(), (float) furnitureYaw, 0);

        List<AABB> aabbs = new ArrayList<>();
        for (HitBox hitBox : placement.hitBoxes()) {
            hitBox.initShapeForPlacement(finalPlacePosition.x(), finalPlacePosition.y(), finalPlacePosition.z(), (float) furnitureYaw, QuaternionUtils.toQuaternionf(0, Math.toRadians(180 - furnitureYaw), 0).conjugate(), aabbs::add);
        }
        if (!aabbs.isEmpty()) {
            if (!FastNMS.INSTANCE.checkEntityCollision(context.getLevel().serverWorld(), aabbs.stream().map(it -> FastNMS.INSTANCE.constructor$AABB(it.minX, it.minY, it.minZ, it.maxX, it.maxY, it.maxZ)).toList(), finalPlacePosition.x(), finalPlacePosition.y(), finalPlacePosition.z())) {
                return InteractionResult.FAIL;
            }
        }

        if (!BukkitCraftEngine.instance().antiGriefProvider().canPlace(bukkitPlayer, furnitureLocation)) {
            return InteractionResult.FAIL;
        }

        FurnitureAttemptPlaceEvent attemptPlaceEvent = new FurnitureAttemptPlaceEvent(bukkitPlayer, customFurniture, anchorType, furnitureLocation.clone(),
                DirectionUtils.toBlockFace(clickedFace), context.getHand(), world.getBlockAt(context.getClickedPos().x(), context.getClickedPos().y(), context.getClickedPos().z()));
        if (EventUtils.fireAndCheckCancel(attemptPlaceEvent)) {
            return InteractionResult.FAIL;
        }

        Item<?> item = context.getItem();

        BukkitFurniture bukkitFurniture = BukkitFurnitureManager.instance().place(
                furnitureLocation.clone(), customFurniture,
                FurnitureExtraData.builder()
                        .item(item.copyWithCount(1))
                        .anchorType(anchorType)
                        .dyedColor(item.dyedColor().orElse(null))
                        .fireworkExplosionColors(item.fireworkExplosion().map(explosion -> explosion.colors().toIntArray()).orElse(null))
                        .build(), false);

        FurniturePlaceEvent placeEvent = new FurniturePlaceEvent(bukkitPlayer, bukkitFurniture, furnitureLocation, context.getHand());
        if (EventUtils.fireAndCheckCancel(placeEvent)) {
            bukkitFurniture.destroy();
            return InteractionResult.FAIL;
        }

        Cancellable dummy = Cancellable.dummy();
        PlayerOptionalContext functionContext = PlayerOptionalContext.of(player, ContextHolder.builder()
                .withParameter(DirectContextParameters.FURNITURE, bukkitFurniture)
                .withParameter(DirectContextParameters.POSITION, LocationUtils.toWorldPosition(furnitureLocation))
                .withParameter(DirectContextParameters.EVENT, dummy)
                .withParameter(DirectContextParameters.HAND, context.getHand())
                .withParameter(DirectContextParameters.ITEM_IN_HAND, item)
        );
        customFurniture.execute(functionContext, EventTrigger.PLACE);
        if (dummy.isCancelled()) {
            return InteractionResult.SUCCESS_AND_CANCEL;
        }

        if (!player.isCreativeMode()) {
            item.count(item.count() - 1);
        }

        context.getLevel().playBlockSound(finalPlacePosition, customFurniture.settings().sounds().placeSound());
        player.swingHand(context.getHand());
        return InteractionResult.SUCCESS;
    }

    public static class Factory implements ItemBehaviorFactory {

        @Override
        public ItemBehavior create(Pack pack, Path path, Key key, Map<String, Object> arguments) {
            Object id = arguments.get("furniture");
            if (id == null) {
                throw new LocalizedResourceConfigException("warning.config.item.behavior.furniture.missing_furniture", new IllegalArgumentException("Missing required parameter 'furniture' for furniture_item behavior"));
            }
            if (id instanceof Map<?,?> map) {
                if (map.containsKey(key.toString())) {
                    // 防呆
                    BukkitFurnitureManager.instance().parser().parseSection(pack, path, key, MiscUtils.castToMap(map.get(key.toString()), false));
                } else {
                    BukkitFurnitureManager.instance().parser().parseSection(pack, path, key, MiscUtils.castToMap(map, false));
                }
                return new FurnitureItemBehavior(key);
            } else {
                return new FurnitureItemBehavior(Key.of(id.toString()));
            }
        }
    }
}
