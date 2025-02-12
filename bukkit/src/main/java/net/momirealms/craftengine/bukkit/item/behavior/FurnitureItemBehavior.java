package net.momirealms.craftengine.bukkit.item.behavior;

import net.momirealms.craftengine.bukkit.api.event.FurnitureAttemptPlaceEvent;
import net.momirealms.craftengine.bukkit.api.event.FurniturePlaceEvent;
import net.momirealms.craftengine.bukkit.entity.furniture.BukkitFurnitureManager;
import net.momirealms.craftengine.bukkit.entity.furniture.LoadedFurniture;
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
import org.joml.Vector3d;
import org.joml.Vector3f;

import java.util.*;

public class FurnitureItemBehavior extends ItemBehavior {
    public static final Factory FACTORY = new Factory();
    private final Key id;
    private final BlockSounds sounds;
    private final EnumMap<AnchorType, FurniturePlacement> placements;
    @Nullable
    private final LootTable<ItemStack> lootTable;

    public FurnitureItemBehavior(Key id,
                                 BlockSounds sounds,
                                 EnumMap<AnchorType, FurniturePlacement> placements,
                                 @Nullable LootTable<ItemStack> lootTable) {
        this.id = id;
        this.placements = placements;
        this.lootTable = lootTable;
        this.sounds = sounds;
    }

    public AnchorType getAnyPlacement() {
        return placements.keySet().stream().findFirst().orElse(null);
    }

    public boolean isAllowedPlacement(AnchorType anchorType) {
        return placements.containsKey(anchorType);
    }

    public FurniturePlacement getPlacement(AnchorType anchorType) {
        return placements.get(anchorType);
    }

    @Override
    public InteractionResult useOnBlock(UseOnContext context) {
        return this.place(new BlockPlaceContext(context));
    }

    public InteractionResult place(BlockPlaceContext context) {
        if (!context.canPlace()) {
            return InteractionResult.FAIL;
        }
        Direction clickedFace = context.getClickedFace();
        AnchorType anchorType = switch (clickedFace) {
            case EAST, WEST, NORTH, SOUTH -> AnchorType.WALL;
            case UP -> AnchorType.GROUND;
            case DOWN -> AnchorType.CEILING;
        };
        FurniturePlacement placement = this.placements.get(anchorType);
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
        FurnitureAttemptPlaceEvent attemptPlaceEvent = new FurnitureAttemptPlaceEvent(bukkitPlayer, this, anchorType, new Location(bukkitPlayer.getWorld(), clickedPosition.x(), clickedPosition.y(), clickedPosition.z()));
        if (EventUtils.fireAndCheckCancel(attemptPlaceEvent)) {
            return InteractionResult.FAIL;
        }

        if (!player.isCreativeMode()) {
            Item<?> item = context.getItem();
            item.count(item.count() - 1);
            item.load();
        }
        player.swingHand(context.getHand());

        World world = (World) context.getLevel().getHandle();

        // get position and rotation for placement
        Vec3d finalPlacePosition;
        double furnitureYaw;
        if (anchorType == AnchorType.WALL) {
            furnitureYaw = -Direction.getYaw(clickedFace);
            if (clickedFace == Direction.EAST || clickedFace == Direction.WEST) {
                Pair<Double, Double> xz = placement.alignmentRule.apply(Pair.of(clickedPosition.y(), clickedPosition.z()));
                finalPlacePosition = new Vec3d(clickedPosition.x(), xz.left(), xz.right());
            } else {
                Pair<Double, Double> xz = placement.alignmentRule.apply(Pair.of(clickedPosition.x(), clickedPosition.y()));
                finalPlacePosition = new Vec3d(xz.left(), xz.right(), clickedPosition.z());
            }
        } else {
            furnitureYaw = placement.rotationRule.apply(180 - player.getXRot());
            Pair<Double, Double> xz = placement.alignmentRule.apply(Pair.of(clickedPosition.x(), clickedPosition.z()));
            finalPlacePosition = new Vec3d(xz.left(), clickedPosition.y(), xz.right());
        }

        // spawn entity and load
        Quaternionf quaternion = QuaternionUtils.toQuaternionf(0, Math.toRadians(furnitureYaw), 0);
        Location furnitureLocation = new Location(world, finalPlacePosition.x(), finalPlacePosition.y(), finalPlacePosition.z());
        Entity furnitureEntity = EntityUtils.spawnEntity(world, furnitureLocation, EntityType.ITEM_DISPLAY, entity -> {
            ItemDisplay display = (ItemDisplay) entity;
            display.setTransformation(
                    new Transformation(
                            new Vector3f(),
                            new Quaternionf(),
                            new Vector3f(1, 1, 1),
                            quaternion
                    )
            );
            display.getPersistentDataContainer().set(BukkitFurnitureManager.FURNITURE_KEY, PersistentDataType.STRING, id.toString());
            display.getPersistentDataContainer().set(BukkitFurnitureManager.FURNITURE_ANCHOR_KEY, PersistentDataType.STRING, anchorType.name());
            BukkitFurnitureManager.instance().handleEntityLoadEarly(display);
        });

        context.getLevel().playBlockSound(clickedPosition, sounds.placeSound(), 1f, 1f);

        // TODO Make this event cancellable
        FurniturePlaceEvent placeEvent = new FurniturePlaceEvent(bukkitPlayer, Objects.requireNonNull(BukkitFurnitureManager.instance().getLoadedFurnitureByBaseEntityId(furnitureEntity.getEntityId())), furnitureLocation);
        EventUtils.fireAndForget(placeEvent);

        return InteractionResult.SUCCESS;
    }

    public BlockSounds sounds() {
        return sounds;
    }

    @Nullable
    public LootTable<ItemStack> lootTable() {
        return lootTable;
    }

    public Key itemId() {
        return id;
    }

    public static class FurniturePlacement {
        private final FurnitureElement[] elements;
        private final HitBox[] hitbox;
        private final RotationRule rotationRule;
        private final AlignmentRule alignmentRule;

        public FurniturePlacement(FurnitureElement[] elements, HitBox[] hitbox, RotationRule rotationRule, AlignmentRule alignmentRule) {
            this.elements = elements;
            this.hitbox = hitbox;
            this.rotationRule = rotationRule;
            this.alignmentRule = alignmentRule;
        }

        public HitBox[] hitbox() {
            return hitbox;
        }

        public FurnitureElement[] elements() {
            return elements;
        }

        public RotationRule rotationRule() {
            return rotationRule;
        }

        public AlignmentRule alignmentRule() {
            return alignmentRule;
        }
    }

    public static class Factory implements ItemBehaviorFactory {

        @SuppressWarnings("unchecked")
        @Override
        public ItemBehavior create(Key id, Map<String, Object> arguments) {
            Map<String, Object> lootMap = MiscUtils.castToMap(arguments.get("loot"), true);
            Map<String, Object> soundMap = MiscUtils.castToMap(arguments.get("sounds"), true);
            Map<String, Object> placementMap = MiscUtils.castToMap(arguments.get("placement"), true);
            EnumMap<AnchorType, FurniturePlacement> placements = new EnumMap<>(AnchorType.class);
            if (placementMap == null) {
                throw new IllegalArgumentException("Missing required parameter 'placement' for furniture_item behavior");
            }
            for (Map.Entry<String, Object> entry : placementMap.entrySet()) {
                AnchorType anchorType = AnchorType.valueOf(entry.getKey().toUpperCase(Locale.ENGLISH));
                Map<String, Object> placementArguments = MiscUtils.castToMap(entry.getValue(), true);

                List<FurnitureElement> elements = new ArrayList<>();
                List<Map<String, Object>> elementConfigs = (List<Map<String, Object>>) placementArguments.getOrDefault("elements", List.of());
                for (Map<String, Object> element : elementConfigs) {
                    String key = (String) element.get("item");
                    if (key == null) {
                        throw new IllegalArgumentException("Missing required parameter 'item' for furniture_item behavior");
                    }
                    ItemDisplayContext transform = ItemDisplayContext.valueOf(element.getOrDefault("transform", "NONE").toString().toUpperCase(Locale.ENGLISH));
                    Billboard billboard = Billboard.valueOf(element.getOrDefault("billboard", "FIXED").toString().toUpperCase(Locale.ENGLISH));
                    FurnitureElement furnitureElement = new FurnitureElement(Key.of(key), billboard, transform,
                            MiscUtils.getVector3f(element.getOrDefault("scale", "1")),
                            MiscUtils.getVector3f(element.getOrDefault("translation", "0")),
                            MiscUtils.getVector3f(element.getOrDefault("position", "0")),
                            MiscUtils.getQuaternionf(element.getOrDefault("rotation", "0"))
                    );
                    elements.add(furnitureElement);
                }
                List<Map<String, Object>> hitboxConfigs = (List<Map<String, Object>>) placementArguments.getOrDefault("hitboxes", List.of());
                List<HitBox> hitboxes = new ArrayList<>();
                for (Map<String, Object> config : hitboxConfigs) {
                    List<String> seats = (List<String>) config.getOrDefault("seats", List.of());
                    Seat[] seatArray = seats.stream()
                            .map(arg -> {
                                String[] split = arg.split(" ");
                                if (split.length == 1) return new Seat(MiscUtils.getVector3f(split[0]), 0, false);
                                return new Seat(MiscUtils.getVector3f(split[0]), Float.parseFloat(split[1]), true);
                            })
                            .toArray(Seat[]::new);
                    Vector3f position = MiscUtils.getVector3f(config.getOrDefault("position", "0"));
                    float width = MiscUtils.getAsFloat(config.getOrDefault("width", "1"));
                    float height = MiscUtils.getAsFloat(config.getOrDefault("height", "1"));
                    HitBox hitBox = new HitBox(
                            position,
                            new Vector3f(width, height, width),
                            seatArray,
                            (boolean) config.getOrDefault("interactive", true)
                    );
                    hitboxes.add(hitBox);
                }
                if (hitboxes.isEmpty()) {
                    hitboxes.add(new HitBox(
                            new Vector3f(),
                            new Vector3f(1,1,1),
                            new Seat[0],
                            true
                    ));
                }
                Map<String, Object> ruleSection = MiscUtils.castToMap(placementArguments.get("rules"), true);
                if (ruleSection != null) {
                    RotationRule rotationRule = Optional.ofNullable((String) ruleSection.get("rotation"))
                            .map(it -> RotationRule.valueOf(it.toUpperCase(Locale.ENGLISH)))
                            .orElse(RotationRule.ANY);
                    AlignmentRule alignmentRule = Optional.ofNullable((String) ruleSection.get("alignment"))
                            .map(it -> AlignmentRule.valueOf(it.toUpperCase(Locale.ENGLISH)))
                            .orElse(AlignmentRule.CENTER);
                    placements.put(anchorType, new FurniturePlacement(
                            elements.toArray(new FurnitureElement[0]),
                            hitboxes.toArray(new HitBox[0]),
                            rotationRule,
                            alignmentRule
                    ));
                } else {
                    placements.put(anchorType, new FurniturePlacement(
                            elements.toArray(new FurnitureElement[0]),
                            hitboxes.toArray(new HitBox[0]),
                            RotationRule.ANY,
                            AlignmentRule.CENTER
                    ));
                }
            }
            return new FurnitureItemBehavior(
                    id,
                    BlockSounds.fromMap(soundMap),
                    placements,
                    lootMap == null ? null : LootTable.fromMap(lootMap)
            );
        }
    }
}
