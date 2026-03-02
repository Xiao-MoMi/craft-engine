package net.momirealms.craftengine.core.plugin.config;

import com.mojang.datafixers.util.Either;
import net.momirealms.craftengine.core.entity.EquipmentSlot;
import net.momirealms.craftengine.core.entity.display.Billboard;
import net.momirealms.craftengine.core.entity.display.ItemDisplayContext;
import net.momirealms.craftengine.core.entity.projectile.ProjectileMeta;
import net.momirealms.craftengine.core.entity.seat.SeatConfig;
import net.momirealms.craftengine.core.item.BuildableItem;
import net.momirealms.craftengine.core.item.CustomItem;
import net.momirealms.craftengine.core.item.ItemManager;
import net.momirealms.craftengine.core.item.recipe.Ingredient;
import net.momirealms.craftengine.core.item.recipe.IngredientElement;
import net.momirealms.craftengine.core.item.recipe.remainder.CompositeCraftRemainder;
import net.momirealms.craftengine.core.item.recipe.remainder.CraftRemainder;
import net.momirealms.craftengine.core.item.recipe.remainder.CraftRemainders;
import net.momirealms.craftengine.core.item.recipe.remainder.FixedCraftRemainder;
import net.momirealms.craftengine.core.item.recipe.result.CustomRecipeResult;
import net.momirealms.craftengine.core.item.recipe.result.PostProcessor;
import net.momirealms.craftengine.core.item.recipe.result.PostProcessors;
import net.momirealms.craftengine.core.item.setting.EquipmentData;
import net.momirealms.craftengine.core.item.setting.FoodData;
import net.momirealms.craftengine.core.pack.Identifier;
import net.momirealms.craftengine.core.pack.model.definition.BaseItemModel;
import net.momirealms.craftengine.core.pack.model.definition.ItemModel;
import net.momirealms.craftengine.core.pack.model.definition.ItemModels;
import net.momirealms.craftengine.core.pack.model.generation.display.DisplayMeta;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.context.number.*;
import net.momirealms.craftengine.core.plugin.context.text.TextProvider;
import net.momirealms.craftengine.core.plugin.context.text.TextProviders;
import net.momirealms.craftengine.core.sound.SoundData;
import net.momirealms.craftengine.core.util.*;
import net.momirealms.craftengine.core.world.Vec3i;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

public record ConfigValue(String path, @NotNull Object value) {

    public Object value() {
        return this.value;
    }

    public boolean is(Class<?> type) {
        return type.isAssignableFrom(this.value.getClass());
    }

    public String assemblePath(String path) {
        return this.path + "." + path;
    }

    public String assemblePath(String path, int index) {
        return this.path + "." + path + "[" + index + "]";
    }

    public String assemblePath(int index) {
        return this.path + "[" + index + "]";
    }

    public String getAsString() {
        return this.value.toString();
    }

    public TextProvider getAsTextProvider() {
        return TextProviders.fromString(getAsString());
    }

    public SoundData.SoundValue getAsSoundValue() {
        if (this.value instanceof Number number) {
            return SoundData.SoundValue.fixed(number.floatValue());
        } else {
            String volumeString = getAsString();
            if (volumeString.contains("~")) {
                String[] split = volumeString.split("~", 2);
                float min;
                try {
                    min = Float.parseFloat(split[0]);
                } catch (NumberFormatException e) {
                    throw new KnownResourceException(ConfigConstants.PARSE_FLOAT_FAILED, this.path, split[0]);
                }
                float max;
                try {
                    max = Float.parseFloat(split[1]);
                } catch (NumberFormatException e) {
                    throw new KnownResourceException(ConfigConstants.PARSE_FLOAT_FAILED, this.path, split[1]);
                }
                return SoundData.SoundValue.ranged(min, max);
            } else {
                try {
                    return SoundData.SoundValue.fixed(Float.parseFloat(volumeString));
                } catch (NumberFormatException e) {
                    throw new KnownResourceException(ConfigConstants.PARSE_FLOAT_FAILED, this.path, volumeString);
                }
            }
        }
    }

    public SoundData getAsSoundData(SoundData.SoundValue volume, SoundData.SoundValue pitch) {
        if (this.value instanceof Map<?,?>) {
            ConfigSection section = getAsSection();
            Key soundId = section.getIdentifier("id");
            volume = section.getValueOrDefault(ConfigValue::getAsSoundValue, volume, "volume");
            pitch = section.getValueOrDefault(ConfigValue::getAsSoundValue, pitch, "pitch");
            return new SoundData(soundId, volume, pitch);
        } else {
            return new SoundData(getAsIdentifier(), volume, pitch);
        }
    }

    public int getAsInt() {
        switch (this.value) {
            case Integer i -> { return i; }
            case Number n -> { return n.intValue(); }
            case String s -> {
                try {
                    return Integer.parseInt(s.replace("_", ""));
                } catch (NumberFormatException e) {
                    throw new KnownResourceException(ConfigConstants.PARSE_INT_FAILED, this.path, s);
                }
            }
            case Boolean b -> { return b ? 1 : 0; }
            default -> throw new KnownResourceException(ConfigConstants.PARSE_INT_FAILED, this.path, this.value.toString());
        }
    }

    public float getAsFloat() {
        switch (this.value) {
            case Float f -> { return f; }
            case Number n -> { return n.floatValue(); }
            case String s -> {
                try {
                    return Float.parseFloat(s.replace("_", ""));
                } catch (NumberFormatException e) {
                    throw new KnownResourceException(ConfigConstants.PARSE_FLOAT_FAILED, this.path, s);
                }
            }
            case Boolean b -> { return b ? 1.0f : 0.0f; }
            default -> throw new KnownResourceException(ConfigConstants.PARSE_FLOAT_FAILED, this.path, this.value.toString());
        }
    }

    public double getAsDouble() {
        switch (this.value) {
            case Double d -> { return d; }
            case Number n -> { return n.doubleValue(); }
            case String s -> {
                try {
                    return Double.parseDouble(s.replace("_", ""));
                } catch (NumberFormatException e) {
                    throw new KnownResourceException(ConfigConstants.PARSE_DOUBLE_FAILED, this.path, s);
                }
            }
            case Boolean b -> { return b ? 1.0 : 0.0; }
            default -> throw new KnownResourceException(ConfigConstants.PARSE_DOUBLE_FAILED, this.path, this.value.toString());
        }
    }

    public <T extends Enum<T>> T getAsEnum(Class<T> clazz) {
        String enumString = value.toString();
        try {
            return Enum.valueOf(clazz, enumString.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new KnownResourceException(ConfigConstants.PARSE_ENUM_FAILED, this.path, enumString, EnumUtils.toString(clazz.getEnumConstants()));
        }
    }

    public NumberProvider getAsNumber() {
        switch (this.value) {
            case Number number -> {
                return ConstantNumberProvider.constant(number.doubleValue());
            }
            case Boolean bool -> {
                return ConstantNumberProvider.constant(bool ? 1 : 0);
            }
            case Map<?, ?> ignored -> {
                return NumberProviders.fromConfig(getAsSection());
            }
            default -> {
                String string = getAsString();
                if (string.contains("~")) {
                    String[] split = string.split("~", 2);
                    double min;
                    try {
                        min = Double.parseDouble(split[0]);
                    } catch (NumberFormatException e) {
                        throw new KnownResourceException(ConfigConstants.PARSE_DOUBLE_FAILED, this.path, split[0]);
                    }
                    double max;
                    try {
                        max = Double.parseDouble(split[1]);
                    } catch (NumberFormatException e) {
                        throw new KnownResourceException(ConfigConstants.PARSE_DOUBLE_FAILED, this.path, split[1]);
                    }
                    return new UniformNumberProvider(ConstantNumberProvider.constant(min), ConstantNumberProvider.constant(max));
                } else if (string.contains("<") && string.contains(">")) {
                    return ExpressionNumberProvider.expression(string);
                } else {
                    try {
                        return ConstantNumberProvider.constant(Double.parseDouble(string));
                    } catch (NumberFormatException e) {
                        throw new KnownResourceException(ConfigConstants.PARSE_DOUBLE_FAILED, this.path, string);
                    }
                }
            }
        }
    }

    public SeatConfig getAsSeat() {
        return SeatConfig.fromString(this.path, getAsString());
    }

    public SeatConfig[] getAsSeats() {
        List<String> seatsStr = getAsStringList();
        List<SeatConfig> seats = new ArrayList<>();
        for (String seatStr : seatsStr) {
            seats.add(SeatConfig.fromString(seatStr, getAsString()));
        }
        return seats.toArray(new SeatConfig[0]);
    }

    public Color getAsColor() {
        if (this.value instanceof Number number) {
            return Color.fromDecimal(number.intValue());
        } else {
            return Color.fromStrings(getAsString().split(",", 4));
        }
    }

    public Key getAsIdentifier() {
        String stringFormat = this.value.toString();
        if (Identifier.isValid(stringFormat)) {
            return Key.of(stringFormat);
        } else {
            throw new KnownResourceException(ConfigConstants.PARSE_IDENTIFIER_FAILED, this.path, stringFormat);
        }
    }

    public Key getAsKey() {
        return Key.of(this.value.toString());
    }

    public boolean getAsBoolean() {
        switch (this.value) {
            case Boolean b -> { return b; }
            case Number n -> {
                if (n.byteValue() == 0) return false;
                if (n.byteValue() > 0) return true;
                throw new KnownResourceException(ConfigConstants.PARSE_BOOLEAN_FAILED, this.path, String.valueOf(n));
            }
            case String s -> {
                if (s.equalsIgnoreCase("true") || s.equalsIgnoreCase("yes") || s.equalsIgnoreCase("on")) return true;
                if (s.equalsIgnoreCase("false") || s.equalsIgnoreCase("no") || s.equalsIgnoreCase("off")) return false;
                throw new KnownResourceException(ConfigConstants.PARSE_BOOLEAN_FAILED, this.path, s);
            }
            default -> throw new KnownResourceException(ConfigConstants.PARSE_BOOLEAN_FAILED, this.path, this.value.toString());
        }
    }

    @SuppressWarnings("unchecked")
    public ConfigSection getAsSection() {
        if (this.value instanceof Map<?, ?> map) {
            return ConfigSection.of(this.path, (Map<String, Object>) map);
        }
        throw new KnownResourceException(ConfigConstants.PARSE_SECTION_FAILED, this.path, this.value.getClass().getSimpleName());
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getAsMap() {
        if (this.value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        throw new KnownResourceException(ConfigConstants.PARSE_SECTION_FAILED, this.path, this.value.getClass().getSimpleName());
    }

    @SuppressWarnings("unchecked")
    public List<Object> getAsList() {
        if (this.value instanceof List<?> list) {
            return (List<Object>) list;
        }
        return List.of(this.value);
    }

    public <T> List<T> parseAsList(Function<ConfigValue, T> convertor) {
        if (this.is(List.class)) {
            List<Object> asList = getAsList();
            List<T> converted = new ArrayList<>(asList.size());
            for (int i = 0; i < asList.size(); i++) {
                ConfigValue innerValue = new ConfigValue(assemblePath(i), asList.get(i));
                converted.add(convertor.apply(innerValue));
            }
            return converted;
        } else {
            return List.of(convertor.apply(this));
        }
    }

    public List<ConfigValue> getAsValueList() {
        if (this.is(List.class)) {
            List<Object> asList = getAsList();
            List<ConfigValue> converted = new ArrayList<>(asList.size());
            for (int i = 0; i < asList.size(); i++) {
                ConfigValue innerValue = new ConfigValue(assemblePath(i), asList.get(i));
                converted.add(innerValue);
            }
            return converted;
        } else {
            return List.of(this);
        }
    }

    public void forEach(Consumer<ConfigValue> consumer) {
        if (this.is(List.class)) {
            List<Object> asList = getAsList();
            for (int i = 0; i < asList.size(); i++) {
                ConfigValue innerValue = new ConfigValue(assemblePath(i), asList.get(i));
                consumer.accept(innerValue);
            }
        } else {
            consumer.accept(this);
        }
    }

    @SuppressWarnings("unchecked")
    public List<Object> getAsNonEmptyList() {
        if (this.value instanceof List<?> list) {
            if (list.isEmpty()) {
                throw new KnownResourceException(ConfigConstants.PARSE_NONEMPTY_LIST_FAILED, this.path);
            }
            return (List<Object>) list;
        } else {
            return List.of(this.value);
        }
    }

    public List<String> getAsStringList() {
        if (this.value instanceof List<?> list) {
            List<String> listStr = new ArrayList<>();
            for (Object o : list) {
                listStr.add(o.toString());
            }
            return listStr;
        } else {
            return List.of(this.value.toString());
        }
    }

    public Vector3f getAsVector3f() {
        try {
            switch (this.value) {
                case Number n -> { return new Vector3f(n.floatValue()); }
                case List<?> list -> {
                    if (list.size() == 3) {
                        return new Vector3f(
                                Float.parseFloat(list.get(0).toString()),
                                Float.parseFloat(list.get(1).toString()),
                                Float.parseFloat(list.get(2).toString())
                        );
                    } else if (list.size() == 1) {
                        return new Vector3f(Float.parseFloat(list.getFirst().toString()));
                    }
                }
                case String s -> {
                    String[] split = s.replace("_", "").split(",");
                    if (split.length == 3) {
                        return new Vector3f(Float.parseFloat(split[0]), Float.parseFloat(split[1]), Float.parseFloat(split[2]));
                    } else if (split.length == 1) {
                        return new Vector3f(Float.parseFloat(split[0]));
                    }
                }
                default -> {}
            }
        } catch (Exception ignored) {
        }
        throw new KnownResourceException(ConfigConstants.PARSE_VEC3_FAILED, this.path, this.value.toString());
    }

    public Vec3i getAsVector3i() {
        try {
            switch (this.value) {
                case Number n -> { return new Vec3i(n.intValue()); }
                case List<?> list -> {
                    if (list.size() == 3) {
                        return new Vec3i(
                                Integer.parseInt(list.get(0).toString()),
                                Integer.parseInt(list.get(1).toString()),
                                Integer.parseInt(list.get(2).toString())
                        );
                    } else if (list.size() == 1) {
                        return new Vec3i(Integer.parseInt(list.getFirst().toString()));
                    }
                }
                case String s -> {
                    String[] split = s.replace("_", "").split(",");
                    if (split.length == 3) {
                        return new Vec3i(Integer.parseInt(split[0]), Integer.parseInt(split[1]), Integer.parseInt(split[2]));
                    } else if (split.length == 1) {
                        return new Vec3i(Integer.parseInt(split[0]));
                    }
                }
                default -> {}
            }
        } catch (Exception ignored) {
        }
        throw new KnownResourceException(ConfigConstants.PARSE_VEC3_FAILED, this.path, this.value.toString());
    }

    public ItemModel getAsItemModel() {
        if (is(Map.class)) {
            return ItemModels.fromConfig(getAsSection());
        } else {
            return new BaseItemModel(getAsIdentifier(), List.of(), null);
        }
    }

    public DisplayMeta getAsDisplayMeta() {
        ConfigSection section = getAsSection();
        Vector3f rotation = section.getVector3f(null, "rotation");
        Vector3f scale = section.getVector3f(null, "scale");
        Vector3f translation = section.getVector3f(null, "translation");
        return new DisplayMeta(rotation, scale, translation);
    }

    public Either<Integer, List<Float>> getAsTint() {
        if (this.value instanceof Number number) {
            return Either.left(number.intValue());
        } else if (this.value instanceof List<?>) {
            List<String> colorList = getAsStringList();
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
            return Either.left(getAsColor().color());
        }
    }

    public EquipmentData getAsEquipmentData() {
        ConfigSection section = getAsSection();
        EquipmentSlot slot = section.getNonNullEnum(EquipmentSlot.class, "slot");
        Key assetId = section.getIdentifier("asset_id", "asset-id");
        Key cameraOverlay = section.getIdentifier("camera_overlay", "camera-overlay");
        boolean dispensable = section.getBoolean(true, "dispensable");
        boolean swappable = section.getBoolean(true, "swappable");
        boolean equipOnInteract = section.getBoolean("equip_on_interact", "equip-on-interact");
        boolean damageOnHurt = section.getBoolean(true, "damage_on_hurt", "damage-on-hurt");
        boolean canBeSheared = section.getBoolean("can_be_sheared", "can-be-sheared");
        return new EquipmentData(slot, assetId, dispensable, swappable, damageOnHurt, equipOnInteract, canBeSheared, cameraOverlay);
    }

    public UUID getAsUUID() {
        try {
            return UUID.fromString(this.getAsString());
        } catch (IllegalArgumentException e) {
            throw new KnownResourceException(ConfigConstants.PARSE_UUID_FAILED, this.path, this.value.toString());
        }
    }

    public ConfigValue[] getSplitValues(String regex) {
        return getSplitValues(regex, 0);
    }

    public ConfigValue[] getSplitValues(String regex, int limits) {
        String[] splits = this.getAsString().split(regex, limits);
        ConfigValue[] values = new ConfigValue[splits.length];
        for (int i = 0; i < splits.length; i++) {
            ConfigValue configValue = new ConfigValue(this.path, splits[i]);
            values[i] = configValue;
        }
        return values;
    }

    public ConfigValue[] getSplitValuesRestrict(String regex, int limits) {
        String[] splits = this.getAsString().split(regex, limits);
        if (splits.length != limits) {
            throw new KnownResourceException(ConfigConstants.PARSE_SPLIT_FAILED, this.path, String.valueOf(limits), String.valueOf(splits.length));
        }
        ConfigValue[] values = new ConfigValue[splits.length];
        for (int i = 0; i < splits.length; i++) {
            ConfigValue configValue = new ConfigValue(this.path, splits[i]);
            values[i] = configValue;
        }
        return values;
    }

    public CraftRemainder getAsCraftRemainder() {
        if (this.is(Map.class)) {
            return CraftRemainders.fromConfig(getAsSection());
        } else if (this.is(List.class)) {
            List<CraftRemainder> list = parseAsList(ConfigValue::getAsCraftRemainder);
            return new CompositeCraftRemainder(list.toArray(new CraftRemainder[0]));
        } else {
            return new FixedCraftRemainder(getAsIdentifier(), ConfigConstants.CONSTANT_ONE);
        }
    }

    public ConfigValue nonEmptyList() {
        if (this.value instanceof List<?> list) {
            if (list.isEmpty()) {
                throw new KnownResourceException(ConfigConstants.PARSE_NONEMPTY_LIST_FAILED, this.path);
            }
        }
        return this;
    }

    public <T> Ingredient<T> getAsIngredient() {
        int count = 1;
        ConfigValue itemsValue;
        if (this.is(Map.class)) {
            ConfigSection section = getAsSection();
            count = section.getInt(1, "count");
            itemsValue = section.getNonNullValue(ConfigConstants.ARGUMENT_LIST, "items", "item");
        } else {
            itemsValue = this;
        }
        Set<UniqueKey> itemIds = new HashSet<>();
        Set<UniqueKey> minecraftItemIds = new HashSet<>();
        List<IngredientElement> elements = new ArrayList<>();
        ItemManager<T> itemManager = CraftEngine.instance().itemManager();

        itemsValue.nonEmptyList().forEach(v -> {
            String itemOrTag = v.getAsString();
            if (itemOrTag.charAt(0) == '#') {
                Key tag = Key.of(itemOrTag.substring(1));
                IngredientElement.Tag itemTag = IngredientElement.tag(tag);
                elements.add(itemTag);
                List<UniqueKey> items = itemManager.itemIdsByTag(tag);
                if (items.isEmpty()) {
                    throw new KnownResourceException("resource.recipe.ingredient.invalid_tag", v.path, itemOrTag);
                }
                itemIds.addAll(items);
                for (UniqueKey uniqueKey : items) {
                    List<UniqueKey> ingredientSubstitutes = itemManager.getIngredientSubstitutes(uniqueKey.key());
                    if (!ingredientSubstitutes.isEmpty()) {
                        itemIds.addAll(ingredientSubstitutes);
                    }
                }
            } else {
                Key itemId = Key.of(itemOrTag);
                elements.add(new IngredientElement.Item(itemId));
                if (itemManager.getBuildableItem(itemId).isEmpty()) {
                    throw new KnownResourceException("resource.recipe.ingredient.item_not_exist", v.path, itemOrTag);
                }
                itemIds.add(UniqueKey.create(itemId));
                List<UniqueKey> ingredientSubstitutes = itemManager.getIngredientSubstitutes(itemId);
                if (!ingredientSubstitutes.isEmpty()) {
                    itemIds.addAll(ingredientSubstitutes);
                }
            }
        });
        boolean hasCustomItem = false;
        for (UniqueKey holder : itemIds) {
            Optional<CustomItem<T>> optionalCustomItem = itemManager.getCustomItem(holder.key());
            UniqueKey vanillaItem = holder;
            if (optionalCustomItem.isPresent()) {
                CustomItem<T> customItem = optionalCustomItem.get();
                if (!customItem.isVanillaItem()) {
                    vanillaItem = UniqueKey.create(customItem.material());
                    hasCustomItem = true;
                }
            }
            minecraftItemIds.add(vanillaItem);
        }
        return Ingredient.of(elements, itemIds, minecraftItemIds, hasCustomItem, count);
    }

    public <T> CustomRecipeResult<T> getAsCustomRecipeResult() {
        ConfigSection section = getAsSection();
        Key id = section.getNonNullIdentifier("id");
        int count = section.getInt(1, "count");
        ItemManager<T> itemManager = CraftEngine.instance().itemManager();
        Optional<? extends BuildableItem<T>> buildableItem = itemManager.getBuildableItem(id);
        if (buildableItem.isEmpty()) {
            throw new KnownResourceException("resource.recipe.result.item_not_exist", section.assemblePath("id"), id.asString());
        }
        List<PostProcessor> processors = section.parseSectionList(PostProcessors::fromConfig, "post_processors", "post-processors");
        return new CustomRecipeResult<>(
                buildableItem.get(),
                count,
                processors.isEmpty() ? null : processors.toArray(new PostProcessor[0])
        );
    }

    public ProjectileMeta getAsProjectileMeta() {
        ConfigSection section = getAsSection();
        Key customTridentItemId = section.getNonNullIdentifier("item");
        ItemDisplayContext displayType = section.getEnum(ItemDisplayContext.NONE, ItemDisplayContext.class, "display_transform", "display-transform");
        Billboard billboard = section.getEnum(Billboard.FIXED, Billboard.class, "billboard");
        Vector3f translation = section.getVector3f(new Vector3f(0), "translation");
        Vector3f scale = section.getVector3f(new Vector3f(1), "scale");
        Quaternionf rotation = section.getQuaternionf(new Quaternionf(), "rotation");
        double range = section.getDouble(1, "range");
        return new ProjectileMeta(customTridentItemId, displayType, billboard, scale, translation, rotation, range);
    }

    public FoodData getAsFoodData() {
        ConfigSection section = getAsSection();
        return new FoodData(section.getInt("nutrition"), section.getFloat("saturation"));
    }
}