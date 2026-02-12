package net.momirealms.craftengine.bukkit.item.behavior;

import net.momirealms.antigrieflib.Flag;
import net.momirealms.craftengine.bukkit.api.event.FurnitureAttemptPlaceEvent;
import net.momirealms.craftengine.bukkit.api.event.FurniturePlaceEvent;
import net.momirealms.craftengine.bukkit.entity.furniture.BukkitFurniture;
import net.momirealms.craftengine.bukkit.entity.furniture.BukkitFurnitureManager;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.util.EventUtils;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.core.entity.furniture.*;
import net.momirealms.craftengine.core.entity.furniture.hitbox.FurnitureHitBoxConfig;
import net.momirealms.craftengine.core.entity.player.InteractionResult;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.behavior.ItemBehavior;
import net.momirealms.craftengine.core.item.behavior.ItemBehaviorFactory;
import net.momirealms.craftengine.core.pack.Pack;
import net.momirealms.craftengine.core.pack.PendingConfigSection;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.context.ContextHolder;
import net.momirealms.craftengine.core.plugin.context.EventTrigger;
import net.momirealms.craftengine.core.plugin.context.PlayerOptionalContext;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.plugin.locale.LocalizedResourceConfigException;
import net.momirealms.craftengine.core.plugin.logger.Debugger;
import net.momirealms.craftengine.core.util.*;
import net.momirealms.craftengine.core.world.Vec3d;
import net.momirealms.craftengine.core.world.WorldPosition;
import net.momirealms.craftengine.core.world.collision.AABB;
import net.momirealms.craftengine.core.world.context.UseOnContext;
import net.momirealms.sparrow.nbt.CompoundTag;
import org.bukkit.Location;
import org.bukkit.World;

import java.nio.file.Path;
import java.util.*;
import java.util.function.Predicate;

public class FurnitureItemBehavior extends ItemBehavior {
    public static final ItemBehaviorFactory<FurnitureItemBehavior> FACTORY = new Factory();
    static final Set<String> ALLOWED_ANCHOR_TYPES = Set.of("wall", "ceiling", "ground");
    private final Key id;
    private final Map<AnchorType, Rule> rules;
    private final boolean ignorePlacer;
    private final boolean ignoreEntities;

    protected FurnitureItemBehavior(Key id, Map<AnchorType, Rule> rules, boolean ignorePlacer, boolean ignoreEntities) {
        this.id = id;
        this.rules = rules;
        this.ignorePlacer = ignorePlacer;
        this.ignoreEntities = ignoreEntities;
    }

    public Key furnitureId() {
        return this.id;
    }

    public Map<AnchorType, Rule> rules() {
        return this.rules;
    }

    public boolean ignorePlacer() {
        return this.ignorePlacer;
    }

    public boolean ignoreEntities() {
        return this.ignoreEntities;
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

        Direction clickedFace = context.getClickedFace();
        AnchorType anchorType = switch (clickedFace) {
            case EAST, WEST, NORTH, SOUTH -> AnchorType.WALL;
            case UP -> AnchorType.GROUND;
            case DOWN -> AnchorType.CEILING;
        };

        CustomFurniture customFurniture = optionalCustomFurniture.get();
        FurnitureVariant variant = customFurniture.getVariant(anchorType.variantName());
        if (variant == null) {
            return InteractionResult.FAIL;
        }

        Rule rule = this.rules.get(anchorType);
        if (rule == null) {
            rule = Rule.DEFAULT;
        }

        Player player = context.getPlayer();
        if (player != null && player.isAdventureMode()) {
            return InteractionResult.FAIL;
        }

        Vec3d clickedPosition = context.getClickedLocation();

        // get position and rotation for placement
        Vec3d finalPlacePosition;
        double furnitureYaw;
        if (anchorType == AnchorType.WALL) {
            furnitureYaw = Direction.getYaw(clickedFace);
            if (clickedFace == Direction.EAST || clickedFace == Direction.WEST) {
                Pair<Double, Double> xz = rule.alignmentRule().apply(Pair.of(clickedPosition.y(), clickedPosition.z()));
                finalPlacePosition = new Vec3d(clickedPosition.x(), xz.left(), xz.right());
            } else {
                Pair<Double, Double> xz = rule.alignmentRule().apply(Pair.of(clickedPosition.x(), clickedPosition.y()));
                finalPlacePosition = new Vec3d(xz.left(), xz.right(), clickedPosition.z());
            }
        } else {
            furnitureYaw = rule.rotationRule().apply(180 + (player != null ? player.yRot() : 0));
            Pair<Double, Double> xz = rule.alignmentRule().apply(Pair.of(clickedPosition.x(), clickedPosition.z()));
            finalPlacePosition = new Vec3d(xz.left(), clickedPosition.y(), xz.right());
        }

        // trigger event
        org.bukkit.entity.Player bukkitPlayer = player != null ? (org.bukkit.entity.Player) player.platformPlayer() : null;
        World world = (World) context.getLevel().platformWorld();
        Location furnitureLocation = new Location(world, finalPlacePosition.x(), finalPlacePosition.y(), finalPlacePosition.z(), (float) furnitureYaw, 0);
        WorldPosition furniturePos = LocationUtils.toWorldPosition(furnitureLocation);
        List<AABB> aabbs = new ArrayList<>();
        // 收集阻挡的碰撞箱
        for (FurnitureHitBoxConfig<?> hitBoxConfig : variant.hitBoxConfigs()) {
            hitBoxConfig.prepareBoundingBox(furniturePos, aabbs::add, false);
        }
        // 检查方块、实体阻挡
        if (!aabbs.isEmpty()) {
            Predicate<Object> entityPredicate;
            if (this.ignoreEntities) {
                entityPredicate = (o) -> false;
            } else if (this.ignorePlacer) {
                entityPredicate = player != null ? (o) -> o != player.serverPlayer() : (o) -> true;
            } else {
                entityPredicate = (o) -> true;
            }
            if (!FastNMS.INSTANCE.checkEntityCollision(context.getLevel().serverWorld(), aabbs.stream().map(it -> FastNMS.INSTANCE.constructor$AABB(it.minX, it.minY, it.minZ, it.maxX, it.maxY, it.maxZ)).toList(), entityPredicate)) {
                if (player != null && player.enableFurnitureDebug() && VersionHelper.isPaper()) {
                    player.playSound(Key.of("minecraft:entity.villager.no"));
                    Key flame = Key.of("flame");
                    for (AABB aabb : aabbs) {
                        List<Vec3d> edgePoints = aabb.getEdgePoints(0.125);
                        for (Vec3d edgePoint : edgePoints) {
                            player.playParticle(flame, edgePoint.x(), edgePoint.y(), edgePoint.z());
                        }
                    }
                }
                return InteractionResult.FAIL;
            }
        }
        // 检查其他插件兼容性
        if (!BukkitCraftEngine.instance().antiGriefProvider().test(bukkitPlayer, Flag.PLACE, furnitureLocation)) {
            return InteractionResult.FAIL;
        }
        ContextHolder.Builder contextBuilder = ContextHolder.builder();
        // 触发尝试放置的事件
        if (player != null) {
            FurnitureAttemptPlaceEvent attemptPlaceEvent = new FurnitureAttemptPlaceEvent(bukkitPlayer, customFurniture, variant, furnitureLocation.clone(), context.getHand(), world.getBlockAt(context.getClickedPos().x(), context.getClickedPos().y(), context.getClickedPos().z()), contextBuilder);
            if (EventUtils.fireAndCheckCancel(attemptPlaceEvent)) {
                return InteractionResult.FAIL;
            }
        }
        Item<?> item = context.getItem();
        if (ItemUtils.isEmpty(item)) return InteractionResult.FAIL;
        // 获取家具物品的一些属性
        FurnitureDataAccessor dataAccessor = FurnitureDataAccessor.of(new CompoundTag());
        dataAccessor.setVariant(variant.name());
        dataAccessor.setItem(item.copyWithCount(1));
        dataAccessor.setDyedColor(item.dyedColor().orElse(null));
        dataAccessor.setFireworkExplosionColors(item.fireworkExplosion().map(explosion -> explosion.colors().toIntArray()).orElse(null));
        // 放置家具
        BukkitFurniture bukkitFurniture = BukkitFurnitureManager.instance().place(furnitureLocation.clone(), customFurniture, dataAccessor, false);
        // 触发放置事件
        if (player != null) {
            FurniturePlaceEvent placeEvent = new FurniturePlaceEvent(bukkitPlayer, bukkitFurniture, furnitureLocation, context.getHand(), contextBuilder);
            if (EventUtils.fireAndCheckCancel(placeEvent)) {
                bukkitFurniture.destroy();
                return InteractionResult.FAIL;
            }
        }
        // 触发ce事件
        Cancellable dummy = Cancellable.dummy();
        PlayerOptionalContext functionContext = PlayerOptionalContext.of(player,
                contextBuilder
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
        // 后续处理
        if (player != null) {
            if (!player.canInstabuild()) {
                item.count(item.count() - 1);
            }
            player.swingHand(context.getHand());
        }
        context.getLevel().playBlockSound(finalPlacePosition, customFurniture.settings().sounds().placeSound());
        return InteractionResult.SUCCESS;
    }

    private static class Factory implements ItemBehaviorFactory<FurnitureItemBehavior> {

        @SuppressWarnings("DuplicatedCode")
        @Override
        public FurnitureItemBehavior create(Pack pack, Path path, String node, Key key, Map<String, Object> arguments) {
            Object id = arguments.get("furniture");
            if (id == null) {
                throw new LocalizedResourceConfigException("warning.config.item.behavior.furniture.missing_furniture", new IllegalArgumentException("Missing required parameter 'furniture' for furniture_item behavior"));
            }
            Map<String, Object> rulesMap = ResourceConfigUtils.getAsMapOrNull(arguments.get("rules"), "rules");
            Key furnitureId;
            if (id instanceof Map<?,?> map) {
                Map<String, Object> furnitureSection;
                if (map.containsKey(key.toString())) {
                    // 防呆
                    furnitureSection = MiscUtils.castToMap(map.get(key.toString()), false);
                    BukkitFurnitureManager.instance().parser().addPendingConfigSection(new PendingConfigSection(pack, path, node, key, furnitureSection));
                } else {
                    furnitureSection = MiscUtils.castToMap(map, false);
                    BukkitFurnitureManager.instance().parser().addPendingConfigSection(new PendingConfigSection(pack, path, node, key, furnitureSection));
                }
                furnitureId = key;
                // 兼容老版本
                if (rulesMap == null) {
                    Map<String, Object> placementSection = ResourceConfigUtils.getAsMapOrNull(furnitureSection.get("placement"), "placement");
                    if (placementSection != null) {
                        rulesMap = new HashMap<>();
                        for (Map.Entry<String, Object> entry : placementSection.entrySet()) {
                            if (entry.getValue() instanceof Map<?, ?> innerMap) {
                                if (innerMap.containsKey("rules")) {
                                    Map<String, Object> rules = ResourceConfigUtils.getAsMap(innerMap.get("rules"), "rules");
                                    if (ALLOWED_ANCHOR_TYPES.contains(entry.getKey())) {
                                        rulesMap.put(entry.getKey(), rules);
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                furnitureId = Key.of(id.toString());
            }
            Map<AnchorType, Rule> rules = new EnumMap<>(AnchorType.class);
            if (rulesMap != null) {
                for (Map.Entry<String, Object> entry : rulesMap.entrySet()) {
                    try {
                        AnchorType type = AnchorType.valueOf(entry.getKey().toUpperCase(Locale.ROOT));
                        Map<String, Object> ruleSection = MiscUtils.castToMap(entry.getValue(), true);
                        rules.put(type, new Rule(
                                ResourceConfigUtils.getAsEnum(ruleSection.get("alignment"), AlignmentRule.class, AlignmentRule.ANY),
                                ResourceConfigUtils.getAsEnum(ruleSection.get("rotation"), RotationRule.class, RotationRule.ANY)
                        ));
                    } catch (IllegalArgumentException ignored) {
                        Debugger.FURNITURE.debug(() -> "Invalid anchor type: " + entry.getKey());
                    }
                }
            }
            return new FurnitureItemBehavior(furnitureId, rules,
                    ResourceConfigUtils.getAsBoolean(arguments.get("ignore-placer"), "ignore-placer"),
                    ResourceConfigUtils.getAsBoolean(arguments.get("ignore-entities"), "ignore-entities")
            );
        }
    }

    public record Rule(AlignmentRule alignmentRule, RotationRule rotationRule) {
        public static final Rule DEFAULT = new Rule(AlignmentRule.ANY, RotationRule.ANY);
    }
}
