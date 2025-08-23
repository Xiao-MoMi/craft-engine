package net.momirealms.craftengine.bukkit.compatibility.skript.expression;

import ch.njol.skript.expressions.base.SimplePropertyExpression;
import net.momirealms.craftengine.bukkit.api.CraftEngineBlocks;
import net.momirealms.craftengine.core.block.CustomBlock;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class ExprBlockCustomBlockID extends SimplePropertyExpression<Object, String> {

    public static void register() {
        register(ExprBlockCustomBlockID.class, String.class, "(custom|ce|craft-engine) block [namespace] id", "blocks/blockdata/customblockstates");
    }

    @Override
    public @Nullable String convert(Object object) {
        if (object instanceof ImmutableBlockState immutableBlockState)
            return immutableBlockState.owner().value().id().toString();
        if (object instanceof CustomBlock customBlock)
            return customBlock.id().toString();
        if (object instanceof Block block)
            return Optional.ofNullable(CraftEngineBlocks.getCustomBlockState(block)).map(it -> it.owner().value().id().toString()).orElse(null);
        if (object instanceof BlockData blockData)
            return Optional.ofNullable(CraftEngineBlocks.getCustomBlockState(blockData)).map(it -> it.owner().value().id().toString()).orElse(null);
        return null;
    }

    @Override
    protected String getPropertyName() {
        return "custom block id";
    }

    @Override
    public Class<? extends String> getReturnType() {
        return String.class;
    }
}
