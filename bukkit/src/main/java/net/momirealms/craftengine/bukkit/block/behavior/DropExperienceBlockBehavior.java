package net.momirealms.craftengine.bukkit.block.behavior;

import net.momirealms.craftengine.bukkit.item.BukkitItemManager;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.ItemStackUtils;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.bukkit.world.BukkitWorldManager;
import net.momirealms.craftengine.core.block.BlockDefinition;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory;
import net.momirealms.craftengine.core.block.setting.BlockSettings;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.loot.LootContext;
import net.momirealms.craftengine.core.plugin.config.ConfigConstants;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.context.CommonConditions;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.ContextHolder;
import net.momirealms.craftengine.core.plugin.context.number.NumberProvider;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.Vec3d;
import net.momirealms.craftengine.core.world.World;
import net.momirealms.craftengine.core.world.WorldPosition;
import net.momirealms.craftengine.proxy.minecraft.world.item.ItemStackProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelProxy;

import java.util.function.Predicate;

public final class DropExperienceBlockBehavior extends BukkitBlockBehavior {
    public static final BlockBehaviorFactory<DropExperienceBlockBehavior> FACTORY = new Factory();
    public final NumberProvider amount;
    public final Predicate<Context> condition;

    private DropExperienceBlockBehavior(BlockDefinition blockDefinition,
                                        NumberProvider amount,
                                        Predicate<Context> condition) {
        super(blockDefinition);
        this.amount = amount;
        this.condition = condition;
    }

    @Override
    public void spawnAfterBreak(Object thisBlock, Object[] args) {
        boolean dropExperience = (boolean) args[4]; // 通常来说是 false
        Item item = BukkitItemManager.instance().wrap(ItemStackUtils.getBukkitStack(args[3]));
        if (!dropExperience) {
            ImmutableBlockState state = BlockStateUtils.getOptionalCustomBlockState(args[0]).orElse(null);
            if (state == null) {
                return;
            }
            BlockSettings settings = state.settings();
            if (settings.requireCorrectTool()) {
                if (item.isEmpty()) {
                    return;
                }
                boolean cannotBreak = !settings.isCorrectTool(item.id())
                        && (!settings.respectToolComponent()
                        || !ItemStackProxy.INSTANCE.isCorrectToolForDrops(args[3], state.customBlockState().minecraftState()));
                if (cannotBreak) {
                    return;
                }
            }
        }
        World world = BukkitWorldManager.instance().wrap(LevelProxy.INSTANCE.getWorld(args[1]));
        BlockPos pos = LocationUtils.fromBlockPos(args[2]);
        tryDropExperience(world, pos, item);
    }

    private void tryDropExperience(World world, BlockPos pos, Item item) {
        Vec3d dropPos = Vec3d.atCenterOf(pos);
        ContextHolder holder = ContextHolder.builder()
                .withParameter(DirectContextParameters.POSITION, new WorldPosition(world, dropPos))
                .withParameter(DirectContextParameters.ITEM_IN_HAND, item)
                .build();
        LootContext context = new LootContext(world, null, 1.0f, holder);
        if (!this.condition.test(context)) {
            return;
        }
        int finalAmount = this.amount.getInt(context);
        if (finalAmount <= 0) {
            return;
        }
        world.dropExp(dropPos, finalAmount);
    }

    private static class Factory implements BlockBehaviorFactory<DropExperienceBlockBehavior> {
        private static final String[] AMOUNT = new String[] {"amount", "count"};
        private static final String[] CONDITIONS = new String[] {"conditions", "condition"};

        @Override
        public DropExperienceBlockBehavior create(BlockDefinition block, ConfigSection section) {
            return new DropExperienceBlockBehavior(
                    block,
                    section.getNumber(AMOUNT, ConfigConstants.CONSTANT_ZERO),
                    MiscUtils.allOf(section.getSectionList(CONDITIONS, CommonConditions::fromConfig))
            );
        }
    }
}
