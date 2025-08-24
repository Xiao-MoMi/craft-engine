package net.momirealms.craftengine.bukkit.compatibility.skript;

import net.momirealms.craftengine.bukkit.compatibility.skript.clazz.CraftEngineClasses;
import net.momirealms.craftengine.bukkit.compatibility.skript.condition.CondIsCraftEngineHasBeenLoad;
import net.momirealms.craftengine.bukkit.compatibility.skript.condition.CondIsCustomBlock;
import net.momirealms.craftengine.bukkit.compatibility.skript.condition.CondIsCustomItem;
import net.momirealms.craftengine.bukkit.compatibility.skript.condition.CondIsFurniture;
import net.momirealms.craftengine.bukkit.compatibility.skript.effect.EffPlaceCustomBlock;
import net.momirealms.craftengine.bukkit.compatibility.skript.effect.EffPlaceFurniture;
import net.momirealms.craftengine.bukkit.compatibility.skript.effect.EffRemoveFurniture;
import net.momirealms.craftengine.bukkit.compatibility.skript.event.EvtCraftEngineReload;
import net.momirealms.craftengine.bukkit.compatibility.skript.event.EvtCustomBlock;
import net.momirealms.craftengine.bukkit.compatibility.skript.event.EvtCustomClick;
import net.momirealms.craftengine.bukkit.compatibility.skript.event.EvtCustomFurniture;
import net.momirealms.craftengine.bukkit.compatibility.skript.expression.*;

public class SkriptHook {

    public static void register() {
        CraftEngineClasses.register();
        EvtCraftEngineReload.register();
        EvtCustomBlock.register();
        EvtCustomFurniture.register();
        EvtCustomClick.register();
        CondIsCraftEngineHasBeenLoad.register();
        CondIsCustomBlock.register();
        CondIsFurniture.register();
        CondIsCustomItem.register();
        ExprBlockCustomBlockID.register();
        ExprItemCustomItemID.register();
        ExprBlockCustomBlockState.register();
        ExprCustomItem.register();
        ExprEntityFurnitureID.register();
        EffPlaceCustomBlock.register();
        EffPlaceFurniture.register();
        EffRemoveFurniture.register();
    }
}
