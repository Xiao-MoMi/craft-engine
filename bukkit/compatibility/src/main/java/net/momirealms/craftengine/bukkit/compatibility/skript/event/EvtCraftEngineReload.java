package net.momirealms.craftengine.bukkit.compatibility.skript.event;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser;
import net.momirealms.craftengine.bukkit.api.event.CraftEngineReloadEvent;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("On CraftEngine Reload")
@Description({"Fires when CraftEngine reload"})
@Since("1.0")
public class EvtCraftEngineReload extends SkriptEvent {

    public static void register() {
        Skript.registerEvent("CraftEngine Loaded", EvtCraftEngineReload.class, CraftEngineReloadEvent.class, "(ce|craft(engine|-engine)) [first] (load[ed]|reload)")
                .description("Called when Craft-Engine resource loaded.");
    }

    private boolean onlyCheckFirstCall;
    private static boolean hasBeenCalled = false;

    @Override
    public boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parser) {
        // 检查是否包含 "first" 关键词
        String expr = parser.expr;
        this.onlyCheckFirstCall = expr.contains("first");
        return true;
    }

    @Override
    public boolean check(Event event) {
        if (!(event instanceof CraftEngineReloadEvent reloadEvent)) {
            return false;
        }
        if (onlyCheckFirstCall) {
            if (hasBeenCalled) return false; // 如果 hasBeenCalled 已经为 true，代表已经调用过了, 故返回 false。
            hasBeenCalled = true;
            return true;
        }
        hasBeenCalled = true;
        return true;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return onlyCheckFirstCall ? "craftengine first load" : "craftengine reload";
    }

    public static boolean hasBeenLoad() {
        return hasBeenCalled;
    }
}