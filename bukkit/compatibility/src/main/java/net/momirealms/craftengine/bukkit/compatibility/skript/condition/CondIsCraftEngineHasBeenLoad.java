package net.momirealms.craftengine.bukkit.compatibility.skript.condition;

import ch.njol.skript.Skript;
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

@Name("CraftEngine has been load")
@Description({"Checks CraftEngine has been load."})
@Since("1.0")
public class CondIsCraftEngineHasBeenLoad extends Condition {

    public static void register() {
        Skript.registerCondition(CondIsCraftEngineHasBeenLoad.class,
                "(ce|craft[-]engine) (has been|is) load[ed]",
                "(ce|craft[-]engine) (has not been|is not) load[ed] [yet]",
                "(ce|craft[-]engine) (hasn't been|isn't) load[ed] [yet]"
        );
    }

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        setNegated(matchedPattern >= 1);
        return true;
    }

    @Override
    public boolean check(Event event) {
        boolean beenLoad = EvtCraftEngineReload.hasBeenLoad();
        return isNegated() ? !beenLoad : beenLoad;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "craft-engine " + (isNegated() ? "is not" : "is") + " loaded";
    }


}
