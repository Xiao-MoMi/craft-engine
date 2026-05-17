package net.momirealms.craftengine.bukkit.item.behavior;

import net.momirealms.antigrieflib.Flag;
import net.momirealms.craftengine.bukkit.api.event.FurnitureAttemptPlaceEvent;
import net.momirealms.craftengine.bukkit.api.event.FurniturePlaceEvent;
import net.momirealms.craftengine.bukkit.entity.furniture.BukkitFurniture;
import net.momirealms.craftengine.bukkit.entity.furniture.BukkitFurnitureManager;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.util.CollisionUtils;
import net.momirealms.craftengine.bukkit.util.EventUtils;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.core.entity.furniture.*;
import net.momirealms.craftengine.core.entity.furniture.hitbox.FurnitureHitBoxConfig;
import net.momirealms.craftengine.core.entity.player.InteractionResult;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.behavior.FurnitureItem;
import net.momirealms.craftengine.core.item.behavior.ItemBehavior;
import net.momirealms.craftengine.core.item.behavior.ItemBehaviorFactory;
import net.momirealms.craftengine.core.pack.Pack;
import net.momirealms.craftengine.core.pack.PendingConfigSection;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.ConfigConstants;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.plugin.context.ContextHolder;
import net.momirealms.craftengine.core.plugin.context.EventTrigger;
import net.momirealms.craftengine.core.plugin.context.PlayerOptionalContext;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.util.*;
import net.momirealms.craftengine.core.world.Vec3d;
import net.momirealms.craftengine.core.world.WorldPosition;
import net.momirealms.craftengine.core.world.collision.AABB;
import net.momirealms.craftengine.core.world.context.UseOnContext;
import net.momirealms.craftengine.proxy.minecraft.world.entity.EntityProxy;
import net.momirealms.craftengine.proxy.minecraft.world.phys.AABBProxy;
import net.momirealms.sparrow.nbt.CompoundTag;
import org.bukkit.Location;
import org.bukkit.World;

import java.nio.file.Path;
import java.util.*;
import java.util.function.Predicate;

public class FurnitureItemBehavior extends ItemBehavior implements FurnitureItem {
    public static final ItemBehaviorFactory<FurnitureItemBehavior> FACTORY = new Factory();
    static final Set<String> ALLOWED_ANCHOR_TYPES = Set.of("wall", "ceiling", "ground");
    private final Key id;
    private final Map<String, Rule> rules;
    private final boolean ignorePlacer;
    private final boolean ignoreEntities;

    protected FurnitureItemBehavior(Key id,
                                    Map<String, Rule> rules,
                                    boolean ignorePlacer,
                                    boolean ignoreEntities) {
        this.id = id;
        this.rules = rules;
        this.ignorePlacer = ignorePlacer;
        this.ignoreEntities = ignoreEntities;
    }

    @Override
    public Key furniture() {
        return this.id;
    }

    public Map<String, Rule> rules() {
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
        Optional<FurnitureDefinition> optionalCustomFurniture = BukkitFurnitureManager.instance().furnitureById(this.id);
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

        FurnitureDefinition furnitureDefinition = optionalCustomFurniture.get();
        FurnitureVariant variant = furnitureDefinition.getVariant(anchorType.variantName());
        if (variant == null) {
            return InteractionResult.FAIL;
        }

        Rule rule = this.rules.get(anchorType.variantName());
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
                entityPredicate = player != null ? (o) -> o != player.serverPlayer() && EntityProxy.INSTANCE.getBlocksBuilding(o) : EntityProxy.INSTANCE::getBlocksBuilding;
            } else {
                entityPredicate = EntityProxy.INSTANCE::getBlocksBuilding;
            }
            if (!CollisionUtils.test(context.getLevel().minecraftWorld(), aabbs.stream().map(it -> AABBProxy.INSTANCE.newInstance(it.minX, it.minY, it.minZ, it.maxX, it.maxY, it.maxZ)).toList(), entityPredicate)) {
                if (player != null && player.enableFurnitureDebug() && VersionHelper.isPaper) {
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
            FurnitureAttemptPlaceEvent attemptPlaceEvent = new FurnitureAttemptPlaceEvent(bukkitPlayer, furnitureDefinition, variant, furnitureLocation.clone(), context.getHand(), world.getBlockAt(context.getClickedPos().x(), context.getClickedPos().y(), context.getClickedPos().z()), contextBuilder);
            if (EventUtils.fireAndCheckCancel(attemptPlaceEvent)) {
                return InteractionResult.FAIL;
            }
        }
        Item item = context.getItem();
        if (ItemUtils.isEmpty(item)) return InteractionResult.FAIL;
        // 获取家具物品的一些属性
        FurniturePersistentData dataAccessor = FurniturePersistentData.of(new CompoundTag());
        dataAccessor.setVariant(variant.name());
        dataAccessor.setItem(item.copyWithCount(1));
        // 放置家具
        BukkitFurniture bukkitFurniture = BukkitFurnitureManager.instance().place(furnitureLocation.clone(), furnitureDefinition, dataAccessor, false, player);
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
        furnitureDefinition.execute(functionContext, EventTrigger.PLACE);
        if (dummy.isCancelled()) {
            bukkitFurniture.destroy();
            return InteractionResult.SUCCESS_AND_CANCEL;
        }

        // 让家具加载物品
        bukkitFurniture.controller.loadCustomDataFromItem(item);

        // 后续处理
        if (player != null) {
            if (!player.canInstabuild()) {
                item.shrink(1);
            }
            player.swingHand(context.getHand());
        }

        context.getLevel().playBlockSound(finalPlacePosition, furnitureDefinition.settings().sounds().placeSound());

        return InteractionResult.SUCCESS;
    }

    private static class Factory implements ItemBehaviorFactory<FurnitureItemBehavior> {
        private static final String[] IGNORE_PLACER = new String[]{"ignore_placer", "ignore-placer"};
        private static final String[] IGNORE_ENTITIES = new String[]{"ignore_entities", "ignore-entities"};

        @SuppressWarnings("DuplicatedCode")
        @Override
        public FurnitureItemBehavior create(Pack pack, Path path, Key key, ConfigSection section) {
            ConfigValue furnitureValue = section.getNonNullValue("furniture", ConfigConstants.ARGUMENT_SECTION);
            ConfigSection rulesSection = section.getValue("rules", ConfigValue::getAsSection);

            Map<String, Rule> rules = new HashMap<>();
            Key furnitureId;
            if (furnitureValue.is(Map.class)) {
                ConfigSection furnitureSection = furnitureValue.getAsSection();
                BukkitFurnitureManager.instance().parser().addPendingConfigSection(new PendingConfigSection(pack, path, key, furnitureSection));
                furnitureId = key;
                // 以下代码是兼容老版本配置，旧版配置放置规则位于furniture下
                if (rulesSection == null) {
                    ConfigSection placementSection = furnitureSection.getSection("placement");
                    if (placementSection != null) {
                        for (String anchorType : placementSection.keySet()) {
                            if (ALLOWED_ANCHOR_TYPES.contains(anchorType)) {
                                ConfigSection varSection = placementSection.getNonNullSection(anchorType);
                                ConfigSection ruleSection = varSection.getSection("rules");
                                if (ruleSection != null) {
                                    AlignmentRule alignmentRule = ruleSection.getEnum("alignment", AlignmentRule.class, AlignmentRule.ANY);
                                    RotationRule rotationRule = ruleSection.getEnum("rotation", RotationRule.class, RotationRule.ANY);
                                    rules.put(anchorType, new Rule(alignmentRule, rotationRule));
                                }
                            }
                        }
                    }
                }
            } else {
                furnitureId = furnitureValue.getAsIdentifier();
            }
            if (rulesSection != null) {
                for (String variant : rulesSection.keySet()) {
                    ConfigSection ruleSection = rulesSection.getNonNullSection(variant);
                    AlignmentRule alignmentRule = ruleSection.getEnum("alignment", AlignmentRule.class, AlignmentRule.ANY);
                    RotationRule rotationRule = ruleSection.getEnum("rotation", RotationRule.class, RotationRule.ANY);
                    rules.put(variant, new Rule(alignmentRule, rotationRule));
                }
            }
            return new FurnitureItemBehavior(
                    furnitureId,
                    rules,
                    section.getBoolean(IGNORE_PLACER),
                    section.getBoolean(IGNORE_ENTITIES)
            );
        }
    }

    public record Rule(AlignmentRule alignmentRule, RotationRule rotationRule) {
        public static final Rule DEFAULT = new Rule(AlignmentRule.ANY, RotationRule.ANY);
    }
}
