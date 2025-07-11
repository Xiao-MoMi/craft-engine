package net.momirealms.craftengine.core.pack.model.special;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.plugin.locale.LocalizedResourceConfigException;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Registries;
import net.momirealms.craftengine.core.registry.WritableRegistry;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import net.momirealms.craftengine.core.util.ResourceKey;

import java.util.Map;

public class SpecialModels {
    public static final Key BANNER = Key.of("minecraft:banner");
    public static final Key BED = Key.of("minecraft:bed");
    public static final Key CONDUIT = Key.of("minecraft:conduit");
    public static final Key CHEST = Key.of("minecraft:chest");
    public static final Key DECORATED_POT = Key.of("minecraft:decorated_pot");
    public static final Key HANGING_SIGN = Key.of("minecraft:hanging_sign");
    public static final Key HEAD = Key.of("minecraft:head");
    public static final Key SHIELD = Key.of("minecraft:shield");
    public static final Key SHULKER_BOX = Key.of("minecraft:shulker_box");
    public static final Key STANDING_SIGN = Key.of("minecraft:standing_sign");
    public static final Key TRIDENT = Key.of("minecraft:trident");
    public static final Key PLAYER_HEAD = Key.of("minecraft:player_head");

    static {
        registerFactory(TRIDENT, SimpleSpecialModel.FACTORY);
        registerReader(TRIDENT, SimpleSpecialModel.READER);
        registerFactory(DECORATED_POT, SimpleSpecialModel.FACTORY);
        registerReader(DECORATED_POT, SimpleSpecialModel.READER);
        registerFactory(CONDUIT, SimpleSpecialModel.FACTORY);
        registerReader(CONDUIT, SimpleSpecialModel.READER);
        registerFactory(SHIELD, SimpleSpecialModel.FACTORY);
        registerReader(SHIELD, SimpleSpecialModel.READER);
        registerFactory(HANGING_SIGN, SignSpecialModel.FACTORY);
        registerReader(HANGING_SIGN, SignSpecialModel.READER);
        registerFactory(STANDING_SIGN, SignSpecialModel.FACTORY);
        registerReader(STANDING_SIGN, SignSpecialModel.READER);
        registerFactory(PLAYER_HEAD, PlayerHeadSpecialModel.FACTORY);
        registerReader(PLAYER_HEAD, PlayerHeadSpecialModel.READER);
        registerFactory(CHEST, ChestSpecialModel.FACTORY);
        registerReader(CHEST, ChestSpecialModel.READER);
        registerFactory(BANNER, BannerSpecialModel.FACTORY);
        registerReader(BANNER, BannerSpecialModel.READER);
        registerFactory(BED, BedSpecialModel.FACTORY);
        registerReader(BED, BedSpecialModel.READER);
        registerFactory(HEAD, HeadSpecialModel.FACTORY);
        registerReader(HEAD, HeadSpecialModel.READER);
        registerFactory(SHULKER_BOX, ShulkerBoxSpecialModel.FACTORY);
        registerReader(SHULKER_BOX, ShulkerBoxSpecialModel.READER);
    }

    public static void registerFactory(Key key, SpecialModelFactory factory) {
        ((WritableRegistry<SpecialModelFactory>) BuiltInRegistries.SPECIAL_MODEL_FACTORY)
                .register(ResourceKey.create(Registries.SPECIAL_MODEL_FACTORY.location(), key), factory);
    }

    public static void registerReader(Key key, SpecialModelReader reader) {
        ((WritableRegistry<SpecialModelReader>) BuiltInRegistries.SPECIAL_MODEL_READER)
                .register(ResourceKey.create(Registries.SPECIAL_MODEL_READER.location(), key), reader);
    }

    public static SpecialModel fromMap(Map<String, Object> map) {
        String type = ResourceConfigUtils.requireNonEmptyStringOrThrow(map.get("type"), "warning.config.item.model.special.missing_type");
        Key key = Key.withDefaultNamespace(type, "minecraft");
        SpecialModelFactory factory = BuiltInRegistries.SPECIAL_MODEL_FACTORY.getValue(key);
        if (factory == null) {
            throw new LocalizedResourceConfigException("warning.config.item.model.special.invalid_type", type);
        }
        return factory.create(map);
    }

    public static SpecialModel fromJson(JsonObject json) {
        String type = json.get("type").getAsString();
        Key key = Key.withDefaultNamespace(type, "minecraft");
        SpecialModelReader reader = BuiltInRegistries.SPECIAL_MODEL_READER.getValue(key);
        if (reader == null) {
            throw new IllegalArgumentException("Invalid special model type: " + key);
        }
        return reader.read(json);
    }
}
