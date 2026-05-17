package net.momirealms.craftengine.core.loot;

import net.momirealms.craftengine.core.pack.Pack;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.IdSectionConfigParser;
import net.momirealms.craftengine.core.plugin.config.lifecycle.LoadingStage;
import net.momirealms.craftengine.core.plugin.config.lifecycle.LoadingStages;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractLootManager implements LootManager {
    protected final LootParser lootParser;
    protected final Map<Integer, VanillaLoot> blockLoots = new ConcurrentHashMap<>();
    // TODO 实现一个基于entity data的生物战利品系统
    protected final Map<Key, VanillaLoot> entityLoots = new ConcurrentHashMap<>();
    protected final Map<Key, Loot> lootTables = new ConcurrentHashMap<>();

    public AbstractLootManager() {
        this.lootParser = new LootParser();
    }

    @Override
    public void unload() {
        this.blockLoots.clear();
        this.entityLoots.clear();
    }

    @Override
    public Optional<VanillaLoot> getBlockLoot(int vanillaBlockState) {
        return Optional.ofNullable(this.blockLoots.get(vanillaBlockState));
    }

    @Override
    public Optional<VanillaLoot> getEntityLoot(Key entity) {
        return Optional.ofNullable(this.entityLoots.get(entity));
    }

    @Override
    public Optional<Loot> getLoot(Key key) {
        return Optional.ofNullable(this.lootTables.get(key));
    }

    private final class LootParser extends IdSectionConfigParser {
        public static final String[] CONFIG_SECTION_NAME = new String[] {"loot", "loots"};
        private int count;

        @Override
        public String[] sectionId() {
            return CONFIG_SECTION_NAME;
        }

        @Override
        public LoadingStage loadingStage() {
            return LoadingStages.LOOT_TABLE;
        }

        @Override
        public List<LoadingStage> dependencies() {
            return List.of(LoadingStages.TEMPLATE);
        }

        @Override
        public int count() {
            return count;
        }

        @Override
        public boolean async() {
            return Config.multiThreadedConfigLoad();
        }

        @Override
        protected void parseSection(@NotNull Pack pack, @NotNull Path path, @NotNull Key id, @NotNull ConfigSection section) {
            Loot loot = section.toValue().getAsLoot();
            lootTables.put(id, loot);
            this.count++;
        }
    }
}
