package net.momirealms.craftengine.core.plugin.config;

import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.core.block.AbstractBlockManager;
import net.momirealms.craftengine.core.block.BlockStateWrapper;
import net.momirealms.craftengine.core.loot.Loot;
import net.momirealms.craftengine.core.loot.LootTable;
import net.momirealms.craftengine.core.pack.Identifier;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.context.number.NumberProvider;
import net.momirealms.craftengine.core.plugin.context.number.NumberProviders;
import net.momirealms.craftengine.core.plugin.context.text.TextProvider;
import net.momirealms.craftengine.core.plugin.context.text.TextProviders;
import net.momirealms.craftengine.core.util.*;
import net.momirealms.craftengine.core.world.Vec3i;
import net.momirealms.craftengine.core.world.collision.AABB;
import net.momirealms.sparrow.nbt.Tag;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

public final class ConfigValue {
    private static final Map<Class<?>, Function<ConfigValue, ?>> SERIALIZERS = new HashMap<>();
    private final String path;
    private final Object value;

    static {
        registerSerializer(ConfigSection.class, ConfigValue::getAsSection);
        registerSerializer(Integer.class, ConfigValue::getAsInt);
        registerSerializer(Double.class, ConfigValue::getAsDouble);
        registerSerializer(Float.class, ConfigValue::getAsFloat);
        registerSerializer(Long.class, ConfigValue::getAsLong);
        registerSerializer(Boolean.class, ConfigValue::getAsBoolean);
        registerSerializer(String.class, ConfigValue::getAsString);
        registerSerializer(List.class, ConfigValue::getAsList);
        registerSerializer(Map.class, ConfigValue::getAsMap);
        registerSerializer(UUID.class, ConfigValue::getAsUUID);
        registerSerializer(BlockStateWrapper.class, ConfigValue::getAsBlockState);
        registerSerializer(Key.class, ConfigValue::getAsKey);
        registerSerializer(NumberProvider.class, ConfigValue::getAsNumber);
        registerSerializer(Tag.class, ConfigValue::getAsSNBT);
        registerSerializer(AABB.class, ConfigValue::getAsAABB);
        registerSerializer(Vector3f.class, ConfigValue::getAsVector3f);
        registerSerializer(Vec3i.class, ConfigValue::getAsVector3i);
        registerSerializer(Color.class, ConfigValue::getAsColor);
        registerSerializer(Quaternionf.class, ConfigValue::getAsQuaternion);
        registerSerializer(Object.class, ConfigValue::value);
    }

    public static <T> void registerSerializer(final Class<T> clazz, final Function<ConfigValue, T> serializer) {
        SERIALIZERS.put(clazz, serializer);
    }

    ConfigValue(String path, Object value) {
        this.path = path;
        this.value = value;
    }

    public String path() {
        return path;
    }

    public Object value() {
        return this.value;
    }

    public static ConfigValue of(String path, Object value) {
        return new ConfigValue(path, value);
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

    @SuppressWarnings({"unchecked", "rawtypes"})
    public <T> T getAs(Class<T> type) {
        Function<ConfigValue, ?> function = SERIALIZERS.get(type);
        if (function == null) {
            if (Enum.class.isAssignableFrom(type)) {
                return (T) getAsEnum((Class<Enum>) type);
            }
            throw new IllegalArgumentException("No serializer found for type " + type);
        }
        return (T) function.apply(this);
    }

    public String getAsString() {
        return this.value.toString();
    }

    public String getAsNonEmptyString() {
        String value = this.value.toString();
        if (value.isEmpty()) {
            throw new KnownResourceException(ConfigConstants.PARSE_NONEMPTY_STRING_FAILED, this.path);
        }
        return value;
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

    public int getAsInt(int min) {
        int value = getAsInt();
        if (value < min) {
            throw new KnownResourceException("number.no_less_than", this.path, this.value.toString(), String.valueOf(min));
        }
        return value;
    }

    public int getAsInt(int min, int max) {
        int value = getAsInt();
        if (value < min) {
            throw new KnownResourceException("number.no_less_than", this.path, this.value.toString(), String.valueOf(min));
        } else if (value > max) {
            throw new KnownResourceException("number.no_greater_than", this.path, this.value.toString(), String.valueOf(max));
        }
        return value;
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

    public float getAsFloat(float min) {
        float value = getAsFloat();
        if (value < min) {
            throw new KnownResourceException("number.no_less_than", this.path, this.value.toString(), String.valueOf(min));
        }
        return value;
    }

    public float getAsFloat(float min, float max) {
        float value = getAsFloat();
        if (value < min) {
            throw new KnownResourceException("number.no_less_than", this.path, this.value.toString(), String.valueOf(min));
        } else if (value > max) {
            throw new KnownResourceException("number.no_greater_than", this.path, this.value.toString(), String.valueOf(max));
        }
        return value;
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

    public double getAsDouble(double min) {
        double value = getAsDouble();
        if (value < min) {
            throw new KnownResourceException("number.no_less_than", this.path, this.value.toString(), String.valueOf(min));
        }
        return value;
    }

    public double getAsDouble(double min, double max) {
        double value = getAsDouble();
        if (value < min) {
            throw new KnownResourceException("number.no_less_than", this.path, this.value.toString(), String.valueOf(min));
        } else if (value > max) {
            throw new KnownResourceException("number.no_greater_than", this.path, this.value.toString(), String.valueOf(max));
        }
        return value;
    }

    public long getAsLong() {
        switch (this.value) {
            case Long l -> { return l; }
            case Number n -> { return n.longValue(); }
            case String s -> {
                try {
                    return Long.parseLong(s.replace("_", ""));
                } catch (NumberFormatException e) {
                    throw new KnownResourceException(ConfigConstants.PARSE_LONG_FAILED, this.path, s);
                }
            }
            case Boolean b -> { return b ? 1L : 0L; }
            default -> throw new KnownResourceException(ConfigConstants.PARSE_LONG_FAILED, this.path, this.value.toString());
        }
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

    public <T extends Enum<T>> T getAsEnum(Class<T> clazz) {
        String enumString = value.toString();
        try {
            return Enum.valueOf(clazz, enumString.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new KnownResourceException(ConfigConstants.PARSE_ENUM_FAILED, this.path, enumString, EnumUtils.toString(clazz.getEnumConstants()));
        }
    }

    public <T extends Enum<T>> T getAsEnum(Class<T> clazz, Function<String, T> custom) {
        String enumString = value.toString();
        T enumValue = custom.apply(enumString);
        if (enumValue == null) {
            throw new KnownResourceException(ConfigConstants.PARSE_ENUM_FAILED, this.path, enumString, EnumUtils.toString(clazz.getEnumConstants()));
        }
        return enumValue;
    }

    public Quaternionf getAsQuaternion() {
        try {
            switch (this.value) {
                case Quaternionf q -> { return q; }
                case Number n -> {
                    return QuaternionUtils.toQuaternionf(0, (float) -Math.toRadians(n.floatValue()), 0);
                }
                case List<?> list -> {
                    if (list.size() == 4) {
                        return new Quaternionf(
                                Float.parseFloat(list.get(0).toString()),
                                Float.parseFloat(list.get(1).toString()),
                                Float.parseFloat(list.get(2).toString()),
                                Float.parseFloat(list.get(3).toString())
                        );
                    } else if (list.size() == 1) {
                        float v = Float.parseFloat(list.getFirst().toString());
                        return QuaternionUtils.toQuaternionf(0, (float) -Math.toRadians(v), 0);
                    }
                }
                case String s -> {
                    String[] split = s.replace("_", "").split(",");
                    switch (split.length) {
                        case 4 -> {
                            return new Quaternionf(Float.parseFloat(split[0]), Float.parseFloat(split[1]), Float.parseFloat(split[2]), Float.parseFloat(split[3]));
                        }
                        case 3 -> {
                            return QuaternionUtils.toQuaternionf((float) Math.toRadians(Float.parseFloat(split[2])), (float) Math.toRadians(Float.parseFloat(split[1])), (float) Math.toRadians(Float.parseFloat(split[0])));
                        }
                        case 2 -> {
                            return QuaternionUtils.toQuaternionf((float) Math.toRadians(Float.parseFloat(split[1])), (float) Math.toRadians(Float.parseFloat(split[0])), 0);
                        }
                        case 1 -> {
                            return QuaternionUtils.toQuaternionf(0, (float) -Math.toRadians(Float.parseFloat(split[0])), 0);
                        }
                    }
                }
                default -> {}
            }
        } catch (Exception ignored) {
        }
        throw new KnownResourceException(ConfigConstants.PARSE_QUATERNION_FAILED, this.path, this.value.toString());
    }

    public Color getAsColor() {
        if (this.value instanceof Number number) {
            return Color.fromDecimal(number.intValue());
        } else {
            return Color.fromStrings(getAsString().split(",", 4));
        }
    }

    public Key getAsKey() {
        return Key.of(this.value.toString());
    }

    public Key getAsIdentifier() {
        String stringFormat = this.value.toString().toLowerCase(Locale.ROOT);
        if (Identifier.isValid(stringFormat)) {
            return Key.of(stringFormat);
        } else {
            throw new KnownResourceException(ConfigConstants.PARSE_IDENTIFIER_FAILED, this.path, stringFormat);
        }
    }

    public Key getAsAssetPath() {
        String stringFormat = CharacterUtils.replaceBackslashWithSlash(this.value.toString().toLowerCase(Locale.ROOT));
        if (Identifier.isValid(stringFormat)) {
            return Key.of(stringFormat);
        } else {
            throw new KnownResourceException(ConfigConstants.PARSE_IDENTIFIER_FAILED, this.path, stringFormat);
        }
    }

    @SuppressWarnings("unchecked")
    public ConfigSection getAsSection() {
        if (this.value instanceof Map<?, ?> map) {
            return ConfigSection.of(this.path, (Map<String, Object>) map);
        } else if (this.value instanceof List<?> list) {
            Object first = list.getFirst();
            if (first instanceof Map<?, ?> map) {
                return ConfigSection.of(assemblePath(0), (Map<String, Object>) map);
            }
        }
        throw new KnownResourceException(ConfigConstants.PARSE_SECTION_FAILED, this.path, this.value.getClass().getSimpleName());
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getAsMap() {
        if (this.value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        } else if (this.value instanceof List<?> list) {
            Object first = list.getFirst();
            if (first instanceof Map<?, ?> map) {
                return (Map<String, Object>) map;
            }
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

    public List<ConfigValue> getAsValueList() {
        if (this.value instanceof List<?> list) {
            List<ConfigValue> values = new ArrayList<>();
            for (int i = 0; i < list.size(); i++) {
                values.add(new ConfigValue(assemblePath(i), list.get(i)));
            }
            return values;
        } else {
            return List.of(this);
        }
    }

    public <T> List<T> getAsList(Function<ConfigValue, T> convertor) {
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

    public List<ConfigValue> getAsNonEmptyValueList() {
        if (this.value instanceof List<?> list) {
            if (list.isEmpty()) {
                throw new KnownResourceException(ConfigConstants.PARSE_NONEMPTY_LIST_FAILED, this.path);
            }
            List<ConfigValue> values = new ArrayList<>();
            for (int i = 0; i < list.size(); i++) {
                values.add(new ConfigValue(assemblePath(i), list.get(i)));
            }
            return values;
        } else {
            return List.of(this);
        }
    }

    public <T> List<T> getAsNonEmptyList(Function<ConfigValue, T> convertor) {
        if (this.value instanceof List<?> list) {
            List<Object> asList = getAsList();
            if (list.isEmpty()) {
                throw new KnownResourceException(ConfigConstants.PARSE_NONEMPTY_LIST_FAILED, this.path);
            }
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
    public List<Object> getAsFixedSizeList(int size) {
        if (this.value instanceof List<?> list) {
            if (list.size() != size) {
                throw new KnownResourceException(ConfigConstants.PARSE_FIXED_SIZE_LIST_FAILED, this.path, String.valueOf(size));
            }
            return (List<Object>) list;
        } else if (size == 1) {
            return List.of(this.value);
        } else {
            throw new KnownResourceException(ConfigConstants.PARSE_FIXED_SIZE_LIST_FAILED, this.path, String.valueOf(size));
        }
    }

    public <T> List<T> getAsFixedSizeList(int size, Function<ConfigValue, T> convertor) {
        if (this.value instanceof List<?> list) {
            if (list.size() != size) {
                throw new KnownResourceException(ConfigConstants.PARSE_FIXED_SIZE_LIST_FAILED, this.path, String.valueOf(size));
            }
            List<Object> asList = getAsList();
            List<T> converted = new ArrayList<>(asList.size());
            for (int i = 0; i < asList.size(); i++) {
                ConfigValue innerValue = new ConfigValue(assemblePath(i), asList.get(i));
                converted.add(convertor.apply(innerValue));
            }
            return converted;
        } else if (size == 1) {
            return List.of(convertor.apply(this));
        } else {
            throw new KnownResourceException(ConfigConstants.PARSE_FIXED_SIZE_LIST_FAILED, this.path, String.valueOf(size));
        }
    }

    public List<String> getAsStringList() {
        if (this.value instanceof List<?> list) {
            List<String> listStr = new ArrayList<>(list.size());
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

    public UUID getAsUUID() {
        try {
            return UUID.fromString(this.getAsString());
        } catch (IllegalArgumentException e) {
            throw new KnownResourceException(ConfigConstants.PARSE_UUID_FAILED, this.path, this.value.toString());
        }
    }

    public ConfigValue[] splitValues(String regex) {
        return splitValues(regex, 0);
    }

    public ConfigValue[] splitValues(String regex, int limits) {
        String[] splits = this.getAsString().split(regex, limits);
        ConfigValue[] values = new ConfigValue[splits.length];
        for (int i = 0; i < splits.length; i++) {
            ConfigValue configValue = new ConfigValue(this.path, splits[i]);
            values[i] = configValue;
        }
        return values;
    }

    public ConfigValue[] splitValuesRestrict(String regex, int limits) {
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

    public AABB getAsAABB() {
        try {
            switch (this.value) {
                case AABB aabb -> { return aabb; }
                case Number n -> {
                    double half = n.doubleValue() / 2.0;
                    return new AABB(-half, -half, -half, half, half, half);
                }
                default -> {
                    double[] args;
                    if (this.value instanceof List<?> list) {
                        args = list.stream().mapToDouble(o -> {
                            if (o instanceof Number n) return n.doubleValue();
                            return Double.parseDouble(o.toString().replace("_", ""));
                        }).toArray();
                    } else {
                        String[] split = this.value.toString().replace("_", "").split(",");
                        args = new double[split.length];
                        for (int i = 0; i < split.length; i++) {
                            args[i] = Double.parseDouble(split[i].trim());
                        }
                    }

                    return switch (args.length) {
                        case 1 -> {
                            double h = args[0] / 2.0;
                            yield new AABB(-h, -h, -h, h, h, h);
                        }
                        case 2 -> {
                            double hX = args[0] / 2.0;
                            double hY = args[1] / 2.0;
                            yield new AABB(-hX, -hY, -hX, hX, hY, hX);
                        }
                        case 3 -> {
                            double hX = args[0] / 2.0;
                            double hY = args[1] / 2.0;
                            double hZ = args[2] / 2.0;
                            yield new AABB(-hX, -hY, -hZ, hX, hY, hZ);
                        }
                        case 6 -> new AABB(args[0], args[1], args[2], args[3], args[4], args[5]);
                        default -> throw new IllegalArgumentException();
                    };
                }
            }
        } catch (Exception ignored) {
        }
        throw new KnownResourceException(ConfigConstants.PARSE_AABB_FAILED, this.path, this.value.toString());
    }

    public Tag getAsSNBT() {
        String snbt = getAsString();
        try {
            return TagParser.parseTagFully(snbt);
        } catch (Exception e) {
            throw new KnownResourceException(ConfigConstants.PARSE_SNBT_FAILED, this.path, snbt, e.getMessage());
        }
    }

    // 五种合理情况
    // minecraft:note_block:10
    // note_block:10
    // minecraft:note_block[xxx=xxx]
    // note_block[xxx=xxx]
    // minecraft:barrier
    public BlockStateWrapper getAsBlockState() {
        String stringFormat = getAsString();
        String[] split = stringFormat.split(":");
        if (split.length >= 4) {
            throw new KnownResourceException(ConfigConstants.PARSE_BLOCK_STATE_FAILED, this.path, stringFormat);
        }
        BlockStateWrapper wrapper;
        // 尝试判断最后一位是不是数字
        String stateOrId = split[split.length - 1];
        boolean isId = false;
        int arrangerIndex = 0;
        try {
            arrangerIndex = Integer.parseInt(stateOrId);
            if (arrangerIndex < 0) {
                throw new KnownResourceException(ConfigConstants.PARSE_BLOCK_STATE_FAILED, this.path, stringFormat);
            }
            isId = true;
        } catch (NumberFormatException ignored) {
        }
        // 如果末尾是id，则至少长度为2
        if (isId) {
            if (split.length == 1) {
                throw new KnownResourceException(ConfigConstants.PARSE_BLOCK_STATE_FAILED, this.path, stringFormat);
            }
            // 获取原版方块的id
            Key block = split.length == 2 ? Key.of(split[0]) : Key.of(split[0], split[1]);
            List<BlockStateWrapper> arranger = ((AbstractBlockManager) CraftEngine.instance().blockManager()).blockStateArranger().get(block);
            if (arranger == null) {
                throw new KnownResourceException("resource.block.state.unreleased_state", this.path, stringFormat);
            }
            if (arrangerIndex >= arranger.size()) {
                throw new KnownResourceException("resource.block.state.unreleased_state", this.path, stringFormat);
            }
            wrapper = arranger.get(arrangerIndex);
        } else {
            // 其他情况则是完整的方块
            BlockStateWrapper packedBlockState = CraftEngine.instance().blockManager().createBlockState(stringFormat);
            if (packedBlockState == null) {
                throw new KnownResourceException(ConfigConstants.PARSE_BLOCK_STATE_FAILED, this.path, stringFormat);
            }
            wrapper = packedBlockState;
        }
        return wrapper;
    }

    public NumberProvider getAsNumber() {
        return NumberProviders.fromConfig(this);
    }

    public Loot getAsLoot() {
        if (this.is(Map.class)) {
            return LootTable.fromConfig(this.getAsSection());
        } else {
            return CraftEngine.instance().lootManager().createReference(this.getAsIdentifier());
        }
    }

    public TextProvider getAsText() {
        return TextProviders.fromString(this.getAsString());
    }

    public Component getAsComponent() {
        return AdventureHelper.miniMessage().deserialize(this.getAsString());
    }
}