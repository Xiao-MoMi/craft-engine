package net.momirealms.craftengine.bukkit.item.behavior;

import net.momirealms.craftengine.bukkit.api.event.FurnitureAttemptPlaceEvent;
import net.momirealms.craftengine.bukkit.api.event.FurniturePlaceEvent;
import net.momirealms.craftengine.bukkit.entity.furniture.BukkitFurnitureManager;
import net.momirealms.craftengine.bukkit.entity.furniture.LoadedFurniture;
import net.momirealms.craftengine.bukkit.util.DirectionUtils;
import net.momirealms.craftengine.bukkit.util.EntityUtils;
import net.momirealms.craftengine.bukkit.util.EventUtils;
import net.momirealms.craftengine.core.block.BlockSounds;
import net.momirealms.craftengine.core.entity.furniture.*;
import net.momirealms.craftengine.core.entity.player.InteractionResult;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.behavior.ItemBehavior;
import net.momirealms.craftengine.core.item.behavior.ItemBehaviorFactory;
import net.momirealms.craftengine.core.item.context.BlockPlaceContext;
import net.momirealms.craftengine.core.item.context.UseOnContext;
import net.momirealms.craftengine.core.loot.LootTable;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.*;
import net.momirealms.craftengine.core.world.Vec3d;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;

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
        return this.place(new BlockPlaceContext(context));
    }

    public InteractionResult place(BlockPlaceContext context) {
        if (!context.canPlace()) {
            return InteractionResult.FAIL;
        }
        Optional<CustomFurniture> optionalCustomFurniture = BukkitFurnitureManager.instance().getFurniture(this.id);
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
        int gameTicks = player.gameTicks();
        if (!player.updateLastSuccessfulInteractionTick(gameTicks)) {
            return InteractionResult.FAIL;
        }

        Vec3d clickedPosition = context.getClickLocation();

        // trigger event
        org.bukkit.entity.Player bukkitPlayer = (org.bukkit.entity.Player) player.platformPlayer();
        World world = (World) context.getLevel().getHandle();

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
            furnitureYaw = placement.rotationRule().apply(180 + player.getXRot());
            Pair<Double, Double> xz = placement.alignmentRule().apply(Pair.of(clickedPosition.x(), clickedPosition.z()));
            finalPlacePosition = new Vec3d(xz.left(), clickedPosition.y(), xz.right());
        }

        Location furnitureLocation = new Location(world, finalPlacePosition.x(), finalPlacePosition.y(), finalPlacePosition.z(), (float) furnitureYaw, 0);
        FurnitureAttemptPlaceEvent attemptPlaceEvent = new FurnitureAttemptPlaceEvent(bukkitPlayer, customFurniture, anchorType, furnitureLocation.clone(),
                DirectionUtils.toBlockFace(clickedFace), context.getHand(), world.getBlockAt(context.getClickedPos().x(), context.getClickedPos().y(), context.getClickedPos().z()));
        if (EventUtils.fireAndCheckCancel(attemptPlaceEvent)) {
            return InteractionResult.FAIL;
        }

        LoadedFurniture loadedFurniture = BukkitFurnitureManager.instance().place(customFurniture, furnitureLocation.clone(), anchorType, true);
        if (!player.isCreativeMode()) {
            Item<?> item = context.getItem();
            item.count(item.count() - 1);
            item.load();
        }
        player.swingHand(context.getHand());

        FurniturePlaceEvent placeEvent = new FurniturePlaceEvent(bukkitPlayer, loadedFurniture, furnitureLocation, context.getHand());
        EventUtils.fireAndForget(placeEvent);
        return InteractionResult.SUCCESS;
    }

    public static class Factory implements ItemBehaviorFactory {

        @Override
        public ItemBehavior create(Key id, Map<String, Object> arguments) {
            if (!arguments.containsKey("furniture")) {
                throw new IllegalArgumentException("Missing furniture argument");
            }
            String furnitureId = arguments.get("furniture").toString();
            return new FurnitureItemBehavior(Key.of(furnitureId));
        }
    }
}
