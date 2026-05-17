package net.momirealms.craftengine.bukkit.item.behavior;

import net.momirealms.craftengine.bukkit.entity.furniture.BukkitFurnitureManager;
import net.momirealms.craftengine.bukkit.util.DirectionUtils;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.core.entity.furniture.AlignmentRule;
import net.momirealms.craftengine.core.entity.furniture.RotationRule;
import net.momirealms.craftengine.core.entity.player.InteractionHand;
import net.momirealms.craftengine.core.entity.player.InteractionResult;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.behavior.ItemBehaviorFactory;
import net.momirealms.craftengine.core.pack.Pack;
import net.momirealms.craftengine.core.pack.PendingConfigSection;
import net.momirealms.craftengine.core.plugin.config.ConfigConstants;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.util.Direction;
import net.momirealms.craftengine.core.util.Key;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class LiquidCollisionFurnitureItemBehavior extends FurnitureItemBehavior {
    public static final ItemBehaviorFactory<LiquidCollisionFurnitureItemBehavior> FACTORY = new Factory();
    private final List<String> liquidTypes;
    private final boolean sourceOnly;

    private LiquidCollisionFurnitureItemBehavior(Key id, Map<String, Rule> rules, boolean ignorePlacer, boolean ignoreEntities, boolean sourceOnly, List<String> liquidTypes) {
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
        if (player == null) return InteractionResult.FAIL;
        Object blockHitResult = ItemProxy.INSTANCE.getPlayerPOVHitResult(world.minecraftWorld(), player.serverPlayer(), ClipContextProxy.FluidProxy.ANY);
        Object blockPos = BlockHitResultProxy.INSTANCE.getBlockPos(blockHitResult);
        BlockPos above = new BlockPos(Vec3iProxy.INSTANCE.getX(blockPos), Vec3iProxy.INSTANCE.getY(blockPos), Vec3iProxy.INSTANCE.getZ(blockPos));
        Direction direction = DirectionUtils.fromNMSDirection(BlockHitResultProxy.INSTANCE.getDirection(blockHitResult));
        boolean miss = BlockHitResultProxy.INSTANCE.isMiss(blockHitResult);
        Vec3d hitPos = LocationUtils.fromVec(HitResultProxy.INSTANCE.getLocation(blockHitResult));
        Object fluidType = FluidStateProxy.INSTANCE.getType(BlockGetterProxy.INSTANCE.getFluidState(world.minecraftWorld(), blockPos));
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
    }

    private static class Factory implements ItemBehaviorFactory<LiquidCollisionFurnitureItemBehavior> {

        @SuppressWarnings("DuplicatedCode")
        @Override
        public LiquidCollisionFurnitureItemBehavior create(Pack pack, Path path, Key key, ConfigSection section) {
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
            return new LiquidCollisionFurnitureItemBehavior(
                    furnitureId,
                    rules,
                    section.getBoolean(IGNORE_PLACER),
                    section.getBoolean(IGNORE_ENTITIES),
                    section.getBoolean(SOURCE_ONLY, true),
                    section.getStringList(LIQUID_TYPE)
            );
        }

        private static final String[] IGNORE_PLACER = new String[]{"ignore_placer", "ignore-placer"};
        private static final String[] IGNORE_ENTITIES = new String[]{"ignore_entities", "ignore-entities"};
        private static final String[] SOURCE_ONLY = new String[]{"source_only", "source-only"};
        private static final String[] LIQUID_TYPE = new String[]{"liquid_type", "liquid-type"};
    }
}
