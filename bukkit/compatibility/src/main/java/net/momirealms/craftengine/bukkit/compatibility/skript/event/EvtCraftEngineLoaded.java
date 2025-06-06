package net.momirealms.craftengine.bukkit.compatibility.skript.event;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser;
import net.momirealms.craftengine.bukkit.api.event.CraftEngineReloadEvent;
import net.momirealms.craftengine.bukkit.api.event.FurnitureBreakEvent;
import net.momirealms.craftengine.bukkit.api.event.FurniturePlaceEvent;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

@SuppressWarnings({"unchecked"})
public class EvtCraftEngineLoaded extends SkriptEvent {

    public static void register() {
        Skript.registerEvent("CraftEngine Loaded", EvtCraftEngineLoaded.class, CraftEngineReloadEvent.class, "(ce|craft(engine|-engine)) [first] (load[ed]|reload)")
                .description("Called when Craft-Engine resource loaded.");
    }

    private boolean checkFirstLoad;

    @Override
    public boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parser) {
        // 检查是否包含 "first" 关键词
        String expr = parser.expr;
        this.checkFirstLoad = expr.contains("first");
        return true;
    }

    @Override
    public boolean check(Event event) {
        if (!(event instanceof CraftEngineReloadEvent reloadEvent)) {
            return false;
        }
        if (checkFirstLoad) {
            return reloadEvent.isFirstLoad();
        }
        return true;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return checkFirstLoad ? "craftengine first load" : "craftengine reload";
    }
}
