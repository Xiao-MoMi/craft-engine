package net.momirealms.craftengine.core.pack.model.definition.tint;

import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Either;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.plugin.config.KnownResourceException;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Registries;
import net.momirealms.craftengine.core.registry.WritableRegistry;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.ResourceKey;

import java.util.ArrayList;
import java.util.List;

public final class Tints {
    public static final TintType<ConstantTint> CONSTANT = register(Key.of("constant"), ConstantTint.FACTORY, ConstantTint.READER);
    public static final TintType<CustomModelDataTint> CUSTOM_MODEL_DATA = register(Key.of("custom_model_data"), CustomModelDataTint.FACTORY, CustomModelDataTint.READER);
    public static final TintType<SimpleDefaultTint> DYE = register(Key.of("dye"), SimpleDefaultTint.FACTORY, SimpleDefaultTint.READER);
    public static final TintType<SimpleDefaultTint> FIREWORK = register(Key.of("firework"), SimpleDefaultTint.FACTORY, SimpleDefaultTint.READER);
    public static final TintType<SimpleDefaultTint> MAP_COLOR = register(Key.of("map_color"), SimpleDefaultTint.FACTORY, SimpleDefaultTint.READER);
    public static final TintType<SimpleDefaultTint> POTION = register(Key.of("potion"), SimpleDefaultTint.FACTORY, SimpleDefaultTint.READER);
    public static final TintType<SimpleDefaultTint> TEAM = register(Key.of("team"), SimpleDefaultTint.FACTORY, SimpleDefaultTint.READER);
    public static final TintType<GrassTint> GRASS = register(Key.of("grass"), GrassTint.FACTORY, GrassTint.READER);

    private Tints() {}

    public static <T extends Tint> TintType<T> register(Key id, TintFactory<T> factory, TintReader<T> reader) {
        TintType<T> type = new TintType<>(id, factory, reader);
        ((WritableRegistry<TintType<? extends Tint>>) BuiltInRegistries.TINT_TYPE)
                .register(ResourceKey.create(Registries.TINT_TYPE.location(), id), type);
        return type;
    }

    public static Tint fromConfig(ConfigValue value) {
        return fromConfig(value.getAsSection());
    }

    public static Tint fromConfig(ConfigSection section) {
        String type = section.getNonNullString("type");
        Key key = Key.withDefaultNamespace(type, "minecraft");
        TintType<? extends Tint> tintType = BuiltInRegistries.TINT_TYPE.getValue(key);
        if (tintType == null) {
            throw new KnownResourceException("resource.item.model_definition.tint.unknown_type", section.assemblePath("type"), key.asString());
        }
        return tintType.factory().create(section);
    }

    public static Either<Integer, List<Float>> getTintValue(ConfigValue value) {
        if (value.is(Number.class)) {
            return Either.left(value.getAsInt());
        } else if (value.is(List.class)) {
            List<String> colorList = value.getAsStringList();
            boolean hasDot = false;
            for (String color : colorList) {
                if (color.contains(".")) {
                    hasDot = true;
                    break;
                }
            }
            List<Float> fList = new ArrayList<>();
            for (String color : colorList) {
                if (hasDot) {
                    fList.add(MiscUtils.clamp(Float.parseFloat(color), 0f, 1f));
                } else {
                    fList.add(MiscUtils.clamp(Float.parseFloat(color) / 255f, 0f, 1f));
                }
            }
            return Either.right(fList);
        } else {
            return Either.left(value.getAsColor().color());
        }
    }

    public static Tint fromJson(JsonObject json) {
        String type = json.get("type").getAsString();
        Key key = Key.withDefaultNamespace(type, "minecraft");
        TintType<? extends Tint> tintType = BuiltInRegistries.TINT_TYPE.getValue(key);
        if (tintType == null) {
            throw new IllegalArgumentException("Invalid tint type: " + type);
        }
        return tintType.reader().read(json);
    }
}
