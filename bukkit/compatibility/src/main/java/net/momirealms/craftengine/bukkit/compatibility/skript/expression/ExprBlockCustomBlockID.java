package net.momirealms.craftengine.bukkit.compatibility.skript.expression;

import ch.njol.skript.expressions.base.SimplePropertyExpression;
import net.momirealms.craftengine.bukkit.api.CraftEngineBlocks;
import net.momirealms.craftengine.core.block.BlockDefinition;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.addon.SkriptAddon;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxRegistry;

import java.util.Optional;

public final class ExprBlockCustomBlockID extends SimplePropertyExpression<Object, String> {

    public static void register(SkriptAddon addon) {
        DefaultSyntaxInfos.Expression<ExprBlockCustomBlockID, String> expression = infoBuilder(
                ExprBlockCustomBlockID.class, String.class,
                "(custom|ce|craft-engine) block [namespace] id", "blocks/blockdata/customblockstates",
                false
        ).build();
        addon.registry(SyntaxRegistry.class).register(SyntaxRegistry.EXPRESSION, expression);
    }

    @Override
    public @Nullable String convert(Object object) {
        if (object instanceof ImmutableBlockState immutableBlockState)
            return immutableBlockState.owner().value().id().toString();
        if (object instanceof BlockDefinition blockDefinition)
            return blockDefinition.id().toString();
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
