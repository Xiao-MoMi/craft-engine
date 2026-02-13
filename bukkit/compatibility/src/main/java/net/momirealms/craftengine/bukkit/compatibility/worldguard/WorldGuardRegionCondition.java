package net.momirealms.craftengine.bukkit.compatibility.worldguard;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.condition.ConditionFactory;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import net.momirealms.craftengine.core.world.WorldPosition;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Predicate;

public final class WorldGuardRegionCondition<CTX extends Context> implements Condition<CTX> {
    private final MatchMode mode;
    private final List<String> regions;

    public WorldGuardRegionCondition(MatchMode mode, List<String> regions) {
        this.mode = mode;
        this.regions = regions;
    }

    public static <CTX extends Context> ConditionFactory<CTX, WorldGuardRegionCondition<CTX>> factory() {
        return new Factory<>();
    }

    @Override
    public boolean test(CTX ctx) {
        if (this.regions.isEmpty()) return false;
        Optional<WorldPosition> optionalPos = ctx.getOptionalParameter(DirectContextParameters.POSITION);
        if (optionalPos.isEmpty()) {
            return false;
        }
        WorldPosition position = optionalPos.get();
        RegionManager regionManager = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt((World) position.world().platformWorld()));
        if (regionManager != null) {
            ApplicableRegionSet set = regionManager.getApplicableRegions(BlockVector3.at(position.x(), position.y(), position.z()));
            List<String> regionsAtThisPos = new ArrayList<>(set.size());
            for (ProtectedRegion region : set) {
                String id = region.getId();
                regionsAtThisPos.add(id);
            }
            Predicate<String> predicate = regionsAtThisPos::contains;
            return this.mode.matcher.apply(predicate, this.regions);
        }
        return false;
    }

    public enum MatchMode {
        ANY((p, regions) -> {
            for (String region : regions) {
                if (p.test(region)) {
                    return true;
                }
            }
            return false;
        }),
        ALL((p, regions) -> {
            for (String region : regions) {
                if (!p.test(region)) {
                    return false;
                }
            }
            return true;
        });

        private final BiFunction<Predicate<String>, List<String>, Boolean> matcher;

        MatchMode(BiFunction<Predicate<String>, List<String>, Boolean> matcher) {
            this.matcher = matcher;
        }
    }

    private static class Factory<CTX extends Context> implements ConditionFactory<CTX, WorldGuardRegionCondition<CTX>> {

        @Override
        public WorldGuardRegionCondition<CTX> create(Map<String, Object> arguments) {
            int mode = ResourceConfigUtils.getAsInt(arguments.getOrDefault("mode", 1), "mode") - 1;
            MatchMode matchMode = MatchMode.values()[mode];
            List<String> regions = MiscUtils.getAsStringList(arguments.get("regions"));
            return new WorldGuardRegionCondition<>(matchMode, regions);
        }
    }
}
