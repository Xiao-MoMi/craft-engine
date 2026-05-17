package net.momirealms.craftengine.core.pack.model.definition.special;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.KnownResourceException;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Registries;
import net.momirealms.craftengine.core.registry.WritableRegistry;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceKey;

public final class SpecialModels {
    public static final SpecialModelType<BannerSpecialModel> BANNER = register(Key.of("banner"), BannerSpecialModel.FACTORY, BannerSpecialModel.READER);
    public static final SpecialModelType<BedSpecialModel> BED = register(Key.of("bed"), BedSpecialModel.FACTORY, BedSpecialModel.READER);
    public static final SpecialModelType<ChestSpecialModel> CHEST = register(Key.of("chest"), ChestSpecialModel.FACTORY, ChestSpecialModel.READER);
    public static final SpecialModelType<SimpleSpecialModel> CONDUIT = register(Key.of("conduit"), SimpleSpecialModel.FACTORY, SimpleSpecialModel.READER);
    public static final SpecialModelType<CopperGolemStatueSpecialModel> COPPER_GOLEM_STATUE = register(Key.of("copper_golem_statue"), CopperGolemStatueSpecialModel.FACTORY, CopperGolemStatueSpecialModel.READER);
    public static final SpecialModelType<SimpleSpecialModel> DECORATED_POT = register(Key.of("decorated_pot"), SimpleSpecialModel.FACTORY, SimpleSpecialModel.READER);
    public static final SpecialModelType<HeadSpecialModel> HEAD = register(Key.of("head"), HeadSpecialModel.FACTORY, HeadSpecialModel.READER);
    public static final SpecialModelType<PlayerHeadSpecialModel> PLAYER_HEAD = register(Key.of("player_head"), PlayerHeadSpecialModel.FACTORY, PlayerHeadSpecialModel.READER);
    public static final SpecialModelType<SimpleSpecialModel> SHIELD = register(Key.of("shield"), SimpleSpecialModel.FACTORY, SimpleSpecialModel.READER);
    public static final SpecialModelType<ShulkerBoxSpecialModel> SHULKER_BOX = register(Key.of("shulker_box"), ShulkerBoxSpecialModel.FACTORY, ShulkerBoxSpecialModel.READER);
    public static final SpecialModelType<SignSpecialModel> STANDING_SIGN = register(Key.of("standing_sign"), SignSpecialModel.FACTORY, SignSpecialModel.READER);
    public static final SpecialModelType<SignSpecialModel> HANGING_SIGN = register(Key.of("hanging_sign"), SignSpecialModel.FACTORY, SignSpecialModel.READER);
    public static final SpecialModelType<SimpleSpecialModel> TRIDENT = register(Key.of("trident"), SimpleSpecialModel.FACTORY, SimpleSpecialModel.READER);
    public static final SpecialModelType<SimpleSpecialModel> BELL = register(Key.of("bell"), SimpleSpecialModel.FACTORY, SimpleSpecialModel.READER);
    public static final SpecialModelType<BookSpecialModel> BOOK = register(Key.of("book"), BookSpecialModel.FACTORY, BookSpecialModel.READER);
    public static final SpecialModelType<EndCubeSpecialModel> END_CUBE = register(Key.of("end_cube"), EndCubeSpecialModel.FACTORY, EndCubeSpecialModel.READER);

    private SpecialModels() {}

    public static <T extends SpecialModel> SpecialModelType<T> register(Key id, SpecialModelFactory<T> factory, SpecialModelReader<T> reader) {
        SpecialModelType<T> type = new SpecialModelType<>(id, factory, reader);
        ((WritableRegistry<SpecialModelType<? extends SpecialModel>>) BuiltInRegistries.SPECIAL_MODEL_TYPE)
                .register(ResourceKey.create(Registries.SPECIAL_MODEL_TYPE.location(), id), type);
        return type;
    }

    public static SpecialModel fromConfig(ConfigSection section) {
        String typeName = section.getNonEmptyString("type");
        Key type = Key.minecraft(typeName);
        SpecialModelType<? extends SpecialModel> specialModelType = BuiltInRegistries.SPECIAL_MODEL_TYPE.getValue(type);
        if (specialModelType == null) {
            throw new KnownResourceException("resource.item.model_definition.special.unknown_type", section.assemblePath("property"), type.asString());
        }
        return specialModelType.factory().create(section);
    }

    public static SpecialModel fromJson(JsonObject json) {
        String type = json.get("type").getAsString();
        Key key = Key.minecraft(type);
        SpecialModelType<? extends SpecialModel> specialModelType = BuiltInRegistries.SPECIAL_MODEL_TYPE.getValue(key);
        if (specialModelType == null) {
            throw new IllegalArgumentException("Invalid special model type: " + key);
        }
        return specialModelType.reader().read(json);
    }
}
