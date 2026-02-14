package net.momirealms.craftengine.bukkit.item.behavior;

import net.momirealms.craftengine.bukkit.entity.furniture.BukkitFurnitureManager;
import net.momirealms.craftengine.bukkit.util.DirectionUtils;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.core.entity.furniture.AlignmentRule;
import net.momirealms.craftengine.core.entity.furniture.AnchorType;
import net.momirealms.craftengine.core.entity.furniture.RotationRule;
import net.momirealms.craftengine.core.entity.player.InteractionHand;
import net.momirealms.craftengine.core.entity.player.InteractionResult;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.behavior.ItemBehaviorFactory;
import net.momirealms.craftengine.core.pack.Pack;
import net.momirealms.craftengine.core.pack.PendingConfigSection;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.locale.LocalizedResourceConfigException;
import net.momirealms.craftengine.core.plugin.logger.Debugger;
import net.momirealms.craftengine.core.util.Direction;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import net.momirealms.craftengine.core.world.BlockHitResult;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.Vec3d;
import net.momirealms.craftengine.core.world.World;
import net.momirealms.craftengine.core.world.context.UseOnContext;
import net.momirealms.craftengine.proxy.minecraft.core.Vec3iProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.ItemProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.BlockGetterProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.ClipContextProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.material.FluidStateProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.material.FluidsProxy;
import net.momirealms.craftengine.proxy.minecraft.world.phys.BlockHitResultProxy;
import net.momirealms.craftengine.proxy.minecraft.world.phys.HitResultProxy;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.*;

public final class LiquidCollisionFurnitureItemBehavior extends FurnitureItemBehavior {
    public static final ItemBehaviorFactory<LiquidCollisionFurnitureItemBehavior> FACTORY = new Factory();
    private final List<String> liquidTypes;
    private final boolean sourceOnly;

    private LiquidCollisionFurnitureItemBehavior(Key id, Map<AnchorType, Rule> rules, boolean ignorePlacer, boolean ignoreEntities, boolean sourceOnly, List<String> liquidTypes) {
        super(id, rules, ignorePlacer, ignoreEntities);
        this.liquidTypes = liquidTypes;
        this.sourceOnly = sourceOnly;
    }

    @Override
    public InteractionResult useOnBlock(UseOnContext context) {
        return use(context.getLevel(), context.getPlayer(), context.getHand());
    }

    @Override
    public InteractionResult use(World world, @Nullable Player player, InteractionHand hand) {
        try {
            if (player == null) return InteractionResult.FAIL;
            Object blockHitResult = ItemProxy.INSTANCE.getPlayerPOVHitResult(world.serverWorld(), player.serverPlayer(), ClipContextProxy.FluidProxy.ANY);
            Object blockPos = BlockHitResultProxy.INSTANCE.getBlockPos(blockHitResult);
            BlockPos above = new BlockPos(Vec3iProxy.INSTANCE.getX(blockPos), Vec3iProxy.INSTANCE.getY(blockPos), Vec3iProxy.INSTANCE.getZ(blockPos));
            Direction direction = DirectionUtils.fromNMSDirection(BlockHitResultProxy.INSTANCE.getDirection(blockHitResult));
            boolean miss = BlockHitResultProxy.INSTANCE.isMiss(blockHitResult);
            Vec3d hitPos = LocationUtils.fromVec(HitResultProxy.INSTANCE.getLocation(blockHitResult));
            Object fluidType = FluidStateProxy.INSTANCE.getType(BlockGetterProxy.INSTANCE.getFluidState(world.serverWorld(), blockPos));
            if (fluidType == FluidsProxy.EMPTY) {
                return InteractionResult.PASS;
            }
            String liquid = null;
            if (fluidType == FluidsProxy.LAVA) {
                liquid = "lava";
            } else if (fluidType == FluidsProxy.WATER) {
                liquid = "water";
            } else if (fluidType == FluidsProxy.FLOWING_LAVA) {
                if (this.sourceOnly) return InteractionResult.PASS;
                liquid = "lava";
            } else if (fluidType == FluidsProxy.FLOWING_WATER) {
                if (this.sourceOnly) return InteractionResult.PASS;
                liquid = "water";
            }
            if (!this.liquidTypes.contains(liquid)) {
                return InteractionResult.PASS;
            }
            if (miss) {
                return super.useOnBlock(new UseOnContext(player, hand, BlockHitResult.miss(hitPos, direction, above)));
            } else {
                boolean inside = BlockHitResultProxy.INSTANCE.isInside(blockHitResult);
                return super.useOnBlock(new UseOnContext(player, hand, new BlockHitResult(hitPos, direction, above, inside)));
            }
        } catch (Exception e) {
            CraftEngine.instance().logger().warn("Error handling use", e);
            return InteractionResult.FAIL;
        }
    }

    private static class Factory implements ItemBehaviorFactory<LiquidCollisionFurnitureItemBehavior> {

        @SuppressWarnings("DuplicatedCode")
        @Override
        public LiquidCollisionFurnitureItemBehavior create(Pack pack, Path path, String node, Key key, Map<String, Object> arguments) {
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
            return new LiquidCollisionFurnitureItemBehavior(furnitureId, rules,
                    ResourceConfigUtils.getAsBoolean(arguments.get("ignore-placer"), "ignore-placer"),
                    ResourceConfigUtils.getAsBoolean(arguments.get("ignore-entities"), "ignore-entities"),
                    ResourceConfigUtils.getAsBoolean(arguments.getOrDefault("source-only", true), "source-only"),
                    MiscUtils.getAsStringList(arguments.get("liquid-type"))
            );
        }
    }
}
