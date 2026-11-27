package net.momirealms.craftengine.core.pack;

import net.momirealms.craftengine.core.plugin.config.KnownResourceException;

import java.util.*;

final class PackSelection {
    private final Map<String, Boolean> defaults;
    private final Map<String, Set<String>> dependencies = new LinkedHashMap<>();
    private final Map<String, Set<String>> conflicts = new LinkedHashMap<>();

    PackSelection(Map<String, Boolean> defaults, Map<String, List<String>> dependencies, Map<String, List<String>> conflicts) {
        this.defaults = Collections.unmodifiableMap(new LinkedHashMap<>(defaults));
        for (String pack : defaults.keySet()) {
            this.conflicts.put(pack, new HashSet<>());
        }
        for (String pack : defaults.keySet()) {
            for (String conflict : conflicts.getOrDefault(pack, List.of())) {
                if (!this.defaults.containsKey(conflict)) {
                    throw new KnownResourceException("resource_pack.unknown_reference", "resource-pack.packs." + pack + ".conflicts", pack, "conflicts", conflict);
                }
                this.conflicts.get(pack).add(conflict);
                this.conflicts.get(conflict).add(pack);
            }
            collectDependencies(pack, dependencies, new LinkedHashSet<>());
        }
        for (String pack : defaults.keySet()) {
            Set<String> closure = closure(pack);
            for (String member : closure) {
                if (!Collections.disjoint(closure, this.conflicts.get(member))) {
                    throw new KnownResourceException("resource_pack.conflicting_dependencies", "resource-pack.packs." + pack, pack);
                }
            }
        }
    }

    private Set<String> collectDependencies(String pack, Map<String, List<String>> configured, Set<String> visiting) {
        Set<String> cached = this.dependencies.get(pack);
        if (cached != null) return cached;
        if (!visiting.add(pack)) {
            throw new KnownResourceException("resource_pack.circular_dependencies", "resource-pack.packs." + pack + ".dependencies", String.join(" -> ", visiting) + " -> " + pack);
        }
        Set<String> result = new LinkedHashSet<>();
        for (String dependency : configured.getOrDefault(pack, List.of())) {
            if (!this.defaults.containsKey(dependency)) {
                throw new KnownResourceException("resource_pack.unknown_reference", "resource-pack.packs." + pack + ".dependencies", pack, "dependencies", dependency);
            }
            result.add(dependency);
            result.addAll(collectDependencies(dependency, configured, visiting));
        }
        visiting.remove(pack);
        this.dependencies.put(pack, Set.copyOf(result));
        return result;
    }

    private Set<String> closure(String pack) {
        Set<String> result = new LinkedHashSet<>(this.dependencies.get(pack));
        result.add(pack);
        return result;
    }

    private void enable(Set<String> selected, String pack) {
        Set<String> additions = closure(pack);
        Set<String> excluded = new HashSet<>();
        additions.forEach(member -> excluded.addAll(this.conflicts.get(member)));
        selected.removeIf(member -> excluded.contains(member)
                || !Collections.disjoint(this.dependencies.get(member), excluded));
        selected.addAll(additions);
    }

    private void disable(Set<String> selected, String pack) {
        selected.removeIf(member -> member.equals(pack) || this.dependencies.get(member).contains(pack));
    }

    List<String> resolve(Map<String, Boolean> preferences) {
        Set<String> selected = new HashSet<>();
        this.defaults.forEach((pack, enabled) -> {
            if (!preferences.containsKey(pack) && enabled) enable(selected, pack);
        });
        this.defaults.keySet().forEach(pack -> {
            if (Boolean.TRUE.equals(preferences.get(pack))) enable(selected, pack);
        });
        return ordered(selected);
    }

    List<String> resolve(Collection<String> requested) {
        Set<String> selected = new HashSet<>();
        this.defaults.keySet().forEach(pack -> {
            if (requested.contains(pack)) enable(selected, pack);
        });
        return ordered(selected);
    }

    private List<String> ordered(Set<String> selected) {
        return this.defaults.keySet().stream().filter(selected::contains).toList();
    }

    Map<String, Boolean> expandUpdates(Map<String, Boolean> previous, Map<String, Boolean> updates) {
        if (Collections.disjoint(updates.keySet(), this.defaults.keySet())) return new HashMap<>(updates);
        Map<String, Boolean> next = new HashMap<>(previous);
        updates.forEach((pack, enabled) -> {
            if (enabled == null) next.remove(pack);
            else next.put(pack, enabled);
        });
        Set<String> selected = new HashSet<>(resolve(previous));
        this.defaults.forEach((pack, enabled) -> {
            if (updates.containsKey(pack) && !next.getOrDefault(pack, enabled)) disable(selected, pack);
        });
        this.defaults.forEach((pack, enabled) -> {
            if (updates.containsKey(pack) && next.getOrDefault(pack, enabled)) enable(selected, pack);
        });
        Map<String, Boolean> expanded = new HashMap<>(updates);
        this.defaults.forEach((pack, enabled) -> {
            boolean actual = selected.contains(pack);
            if (next.getOrDefault(pack, enabled) != actual) expanded.put(pack, actual);
        });
        return expanded;
    }

    boolean dependenciesAvailable(String pack, Set<String> available) {
        return available.containsAll(this.dependencies.get(pack));
    }
}
