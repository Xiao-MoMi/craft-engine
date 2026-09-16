package net.momirealms.craftengine.core.plugin.storage;

import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.dependency.Dependencies;

import java.util.List;

public final class MongoStorageFactory implements StorageFactory<MongoStorage> {
    public static final MongoStorageFactory INSTANCE = new MongoStorageFactory();

    private MongoStorageFactory() {}

    @Override
    public MongoStorage create(ConfigSection section) {
        StorageConfig.Mongo settings = StorageConfig.Mongo.fromConfig(section);
        CraftEngine.instance().dependencyManager().loadDependencies(List.of(
                Dependencies.BSON_RECORD_CODEC,
                Dependencies.BSON,
                Dependencies.MONGODB_DRIVER_CORE,
                Dependencies.MONGODB_DRIVER_SYNC
        ));
        return new MongoStorage(settings);
    }
}
