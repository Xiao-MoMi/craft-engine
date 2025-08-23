package net.momirealms.craftengine.bukkit.compatibility.skript.expression;

import ch.njol.skript.expressions.base.SimplePropertyExpression;
import net.momirealms.craftengine.bukkit.api.CraftEngineBlocks;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.jetbrains.annotations.Nullable;

public class ExprBlockCustomBlockState extends SimplePropertyExpression<Object, ImmutableBlockState> {

    public static void register() {
        register(ExprBlockCustomBlockState.class, ImmutableBlockState.class, "(custom|ce|craft-engine) block[ ]state", "blocks/blockdata");
    }

    @Override
    public @Nullable ImmutableBlockState convert(Object object) {
        if (object instanceof Block block)
            return CraftEngineBlocks.getCustomBlockState(block);
        if (object instanceof BlockData blockData)
            return CraftEngineBlocks.getCustomBlockState(blockData);
        return null;
    }

    @Override
    protected String getPropertyName() {
        return "custom block state";
    }

    @Override
    public Class<? extends ImmutableBlockState> getReturnType() {
        return ImmutableBlockState.class;
    }
}
