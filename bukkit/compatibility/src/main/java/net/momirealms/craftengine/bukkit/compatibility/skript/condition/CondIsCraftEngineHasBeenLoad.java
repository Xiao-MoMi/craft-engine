package net.momirealms.craftengine.bukkit.compatibility.skript.condition;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import net.momirealms.craftengine.bukkit.compatibility.skript.event.EvtCraftEngineReload;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.addon.SkriptAddon;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name("CraftEngine has been load")
@Description({"Checks CraftEngine has been load."})
@Since("1.0")
public final class CondIsCraftEngineHasBeenLoad extends Condition {

    public static void register(SkriptAddon addon) {
        SyntaxInfo<CondIsCraftEngineHasBeenLoad> condition = SyntaxInfo.builder(CondIsCraftEngineHasBeenLoad.class)
                .addPattern("(ce|craft[-]engine) (has been|is) load[ed]")
                .addPattern("(ce|craft[-]engine) (has not been|is not) load[ed] [yet]")
                .addPattern("(ce|craft[-]engine) (hasn't been|isn't) load[ed] [yet]")
                .build();
        addon.registry(SyntaxRegistry.class).register(SyntaxRegistry.CONDITION, condition);
    }

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        setNegated(matchedPattern >= 1);
        return true;
    }

    @Override
    public boolean check(Event event) {
        return isNegated() != EvtCraftEngineReload.hasBeenLoad();
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "craft-engine " + (isNegated() ? "is not" : "is") + " loaded";
    }
}
