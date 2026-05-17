package net.momirealms.craftengine.bukkit.compatibility.skript.expression;

import ch.njol.skript.expressions.base.SimplePropertyExpression;
import net.momirealms.craftengine.bukkit.entity.furniture.BukkitFurnitureManager;
import org.bukkit.entity.Entity;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.addon.SkriptAddon;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxRegistry;

public final class ExprEntityFurnitureID extends SimplePropertyExpression<Object, String> {

    public static void register(SkriptAddon addon) {
        DefaultSyntaxInfos.Expression<ExprEntityFurnitureID, String> expression = infoBuilder(
                ExprEntityFurnitureID.class, String.class,
                "[(custom|ce|craft-engine)] furniture [namespace] id",
                "entities",
                false
        ).build();
        addon.registry(SyntaxRegistry.class).register(SyntaxRegistry.EXPRESSION, expression);
    }

    @Override
    public @Nullable String convert(Object object) {
        if (object instanceof Entity entity) {
            return entity.getPersistentDataContainer().get(BukkitFurnitureManager.FURNITURE_KEY, PersistentDataType.STRING);
        }
        return null;
    }

    @Override
    protected String getPropertyName() {
        return "furniture id";
    }

    @Override
    public Class<? extends String> getReturnType() {
        return String.class;
    }
}
