package net.momirealms.craftengine.core.plugin.config;

import net.momirealms.craftengine.core.plugin.context.number.NumberProvider;
import net.momirealms.craftengine.core.plugin.locale.TranslationManager;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.world.collision.AABB;
import net.momirealms.sparrow.nbt.Tag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

@SuppressWarnings("DuplicatedCode")
public final class ConfigSection {
    private final Map<String, Object> value;
    private final String path;

    private ConfigSection(String path, Map<String, Object> value) {
        this.value = value;
        this.path = path;
    }

    public static ConfigSection ofRoot(Map<String, Object> value) {
        return new ConfigSection("", value);
    }

    public static ConfigSection of(String path, Map<String, Object> value) {
        return new ConfigSection(path, value);
    }

    @SuppressWarnings("unchecked")
    public static ConfigSection of(String path, Object value) {
        if (!(value instanceof Map)) {
            throw new KnownResourceException(ConfigConstants.PARSE_SECTION_FAILED, path, value.getClass().getSimpleName());
        }
        return new ConfigSection(path, (Map<String, Object>) value);
    }

    public ConfigSection withSamePath(Map<String, Object> value) {
        return new ConfigSection(this.path, value);
    }

    public ConfigSection copy() {
        return new ConfigSection(this.path, new LinkedHashMap<>(this.value));
    }

    public void put(String key, Object value) {
        this.value.put(key, value);
    }

    public ConfigValue toValue() {
        return ConfigValue.of(this.path, this.value);
    }

    public String path() {
        return path;
    }

    public String assemblePath(String key) {
        if (this.path.isEmpty()) {
            return key;
        }
        return this.path + "." + key;
    }

    public String assembleExistingPath(String first, String... keys) {
        String next = null;
        if (this.value.containsKey(first)) {
            next = first;
        } else {
            for (String key : keys) {
                if (this.value.containsKey(key)) {
                    next = key;
                    break;
                }
            }
        }
        return next;
    }

    public String assemblePath(String key, int index) {
        if (this.path.isEmpty()) {
            return key + "[" + index + "]";
        }
        return this.path + "." + key + "[" + index + "]";
    }

    public boolean containsKey(String key) {
        return this.value.containsKey(key);
    }

    public boolean containsKey(String[] keys) {
        for (String key : keys) {
            if (this.value.containsKey(key)) {
                return true;
            }
        }
        return false;
    }

    public Map<String, Object> values() {
        return this.value;
    }

    public Set<String> keySet() {
        return this.value.keySet();
    }

    public int size() {
        return this.value.size();
    }

    // 获取 config value

    public ConfigValue getValue(String key) {
        Object value = this.value.get(key);
        if (value == null) {
            return null;
        }
        return new ConfigValue(assemblePath(key), value);
    }

    public ConfigValue getValue(String[] keys) {
        for (String key : keys) {
            ConfigValue value = getValue(key);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    // 获取 config value 进行基础转换

    public <T> T getValue(String key, Function<ConfigValue, T> convertor) {
        ConfigValue value = getValue(key);
        if (value == null) {
            return null;
        }
        return convertor.apply(value);
    }

    public <T> T getValue(String[] keys, Function<ConfigValue, T> convertor) {
        ConfigValue value = getValue(keys);
        if (value == null) {
            return null;
        }
        return convertor.apply(value);
    }

    // 获取有默认值的 config value

    public <T> T getValue(String key, Function<ConfigValue, T> convertor, T def) {
        T value = getValue(key, convertor);
        if (value != null) {
            return value;
        }
        return def;
    }

    public <T> T getValue(String[] keys, Function<ConfigValue, T> convertor, T def) {
        T value = getValue(keys, convertor);
        if (value != null) {
            return value;
        }
        return def;
    }

    public <T> T getValue(String key, Function<ConfigValue, T> convertor, Supplier<T> def) {
        T value = getValue(key, convertor);
        if (value != null) {
            return value;
        }
        return def.get();
    }

    public <T> T getValue(String[] keys, Function<ConfigValue, T> convertor, Supplier<T> def) {
        T value = getValue(keys, convertor);
        if (value != null) {
            return value;
        }
        return def.get();
    }


    // 获取不为空的 config value

    @NotNull
    public ConfigValue getNonNullValue(String key, String argType) {
        Object value = this.value.get(key);
        if (value == null) {
            throw new KnownResourceException(ConfigConstants.MISSING_ARGUMENT, assemblePath(key), key, TranslationManager.instance().plainTranslation(argType));
        }
        return new ConfigValue(assemblePath(key), value);
    }

    @NotNull
    public ConfigValue getNonNullValue(String[] keys, String argType) {
        for (String key : keys) {
            Object value = this.value.get(key);
            if (value != null) {
                return new ConfigValue(assemblePath(key), value);
            }
        }
        throw new KnownResourceException(ConfigConstants.MISSING_ARGUMENT, assemblePath(keys[0]), keys[0], TranslationManager.instance().plainTranslation(argType));
    }

    // 获取非空 config value 进行基础转换

    public <T> T getNonNullValue(String key, String argType, Function<ConfigValue, T> convertor) {
        return convertor.apply(getNonNullValue(key, argType));
    }

    public <T> T getNonNullValue(String[] keys, String argType, Function<ConfigValue, T> convertor) {
        return convertor.apply(getNonNullValue(keys, argType));
    }

    // 基础

    @Nullable
    public Object get(String key) {
        return this.value.get(key);
    }

    @Nullable
    public Object get(String[] keys) {
        for (String key : keys) {
            Object value = this.value.get(key);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    public Object getOrDefault(String key, Object defaultValue) {
        Object value = this.value.get(key);
        if (value == null) {
            return defaultValue;
        }
        return value;
    }

    public Object getOrDefault(String[] keys, Object defaultValue) {
        for (String key : keys) {
            Object value = this.value.get(key);
            if (value != null) {
                return value;
            }
        }
        return defaultValue;
    }

    // 字符串

    @NotNull
    public String getNonEmptyString(String key) {
        return getNonNullValue(key, ConfigConstants.ARGUMENT_STRING, ConfigValue::getAsNonEmptyString);
    }

    @NotNull
    public String getNonEmptyString(String[] keys) {
        return getNonNullValue(keys, ConfigConstants.ARGUMENT_STRING, ConfigValue::getAsNonEmptyString);
    }

    @NotNull
    public String getNonNullString(String key) {
        return getNonNullValue(key, ConfigConstants.ARGUMENT_STRING, ConfigValue::getAsString);
    }

    @NotNull
    public String getNonNullString(String[] keys) {
        return getNonNullValue(keys, ConfigConstants.ARGUMENT_STRING, ConfigValue::getAsString);
    }

    @Nullable
    public String getString(String key) {
        return getValue(key, ConfigValue::getAsString);
    }

    @Nullable
    public String getString(String[] keys) {
        return getValue(keys, ConfigValue::getAsString);
    }

    public String getString(String key, String def) {
        return getValue(key, ConfigValue::getAsString, def);
    }

    public String getString(String[] keys, String def) {
        return getValue(keys, ConfigValue::getAsString, def);
    }

    public String getString(String key, Supplier<String> def) {
        return getValue(key, (Function<ConfigValue, String>) ConfigValue::getAsString, def);
    }

    public String getString(String[] keys, Supplier<String> def) {
        return getValue(keys, (Function<ConfigValue, String>) ConfigValue::getAsString, def);
    }

    // 标识符

    @NotNull
    public Key getNonNullIdentifier(String key) {
        return getNonNullValue(key, ConfigConstants.ARGUMENT_IDENTIFIER, ConfigValue::getAsIdentifier);
    }

    @NotNull
    public Key getNonNullIdentifier(String[] keys) {
        return getNonNullValue(keys, ConfigConstants.ARGUMENT_IDENTIFIER, ConfigValue::getAsIdentifier);
    }

    @Nullable
    public Key getIdentifier(String key) {
        return getValue(key, ConfigValue::getAsIdentifier);
    }

    @Nullable
    public Key getIdentifier(String[] keys) {
        return getValue(keys, ConfigValue::getAsIdentifier);
    }

    public Key getIdentifier(String key, Key def) {
        return getValue(key, ConfigValue::getAsIdentifier, def);
    }

    public Key getIdentifier(String[] keys, Key def) {
        return getValue(keys, ConfigValue::getAsIdentifier, def);
    }

    public Key getIdentifier(String key, Supplier<Key> def) {
        return getValue(key, (Function<ConfigValue, Key>) ConfigValue::getAsIdentifier, def);
    }

    // 键

    @NotNull
    public Key getNonNullKey(String key) {
        return getNonNullValue(key, ConfigConstants.ARGUMENT_IDENTIFIER, ConfigValue::getAsKey);
    }

    @NotNull
    public Key getNonNullKey(String[] keys) {
        return getNonNullValue(keys, ConfigConstants.ARGUMENT_IDENTIFIER, ConfigValue::getAsKey);
    }

    @Nullable
    public Key getKey(String key) {
        return getValue(key, ConfigValue::getAsKey);
    }

    @Nullable
    public Key getKey(String[] keys) {
        return getValue(keys, ConfigValue::getAsKey);
    }

    public Key getKey(String key, Key def) {
        return getValue(key, ConfigValue::getAsKey, def);
    }

    public Key getKey(String[] keys, Key def) {
        return getValue(keys, ConfigValue::getAsKey, def);
    }

    public Key getKey(String key, Supplier<Key> def) {
        return getValue(key, (Function<ConfigValue, Key>) ConfigValue::getAsKey, def);
    }

    public Key getKey(String[] keys, Supplier<Key> def) {
        return getValue(keys, (Function<ConfigValue, Key>) ConfigValue::getAsKey, def);
    }

    // 资产标识符

    @NotNull
    public Key getNonNullAssetPath(String key) {
        return getNonNullValue(key, ConfigConstants.ARGUMENT_IDENTIFIER, ConfigValue::getAsAssetPath);
    }

    @NotNull
    public Key getNonNullAssetPath(String[] keys) {
        return getNonNullValue(keys, ConfigConstants.ARGUMENT_IDENTIFIER, ConfigValue::getAsAssetPath);
    }

    @Nullable
    public Key getAssetPath(String key) {
        return getValue(key, ConfigValue::getAsAssetPath);
    }

    @Nullable
    public Key getAssetPath(String[] keys) {
        return getValue(keys, ConfigValue::getAsAssetPath);
    }

    public Key getAssetPath(String key, Key def) {
        return getValue(key, ConfigValue::getAsAssetPath, def);
    }

    public Key getAssetPath(String[] keys, Key def) {
        return getValue(keys, ConfigValue::getAsAssetPath, def);
    }

    public Key getAssetPath(String key, Supplier<Key> def) {
        return getValue(key, (Function<ConfigValue, Key>) ConfigValue::getAsAssetPath, def);
    }

    public Key getAssetPath(String[] keys, Supplier<Key> def) {
        return getValue(keys, (Function<ConfigValue, Key>) ConfigValue::getAsAssetPath, def);
    }

    // 数值提供器

    @NotNull
    public NumberProvider getNonNullNumber(String key) {
        return getNonNullValue(key, ConfigConstants.ARGUMENT_NUMBER, ConfigValue::getAsNumber);
    }

    @NotNull
    public NumberProvider getNonNullNumber(String[] keys) {
        return getNonNullValue(keys, ConfigConstants.ARGUMENT_NUMBER, ConfigValue::getAsNumber);
    }

    @Nullable
    public NumberProvider getNumber(String key) {
        return getValue(key, ConfigValue::getAsNumber);
    }

    @Nullable
    public NumberProvider getNumber(String[] keys) {
        return getValue(keys, ConfigValue::getAsNumber);
    }

    public NumberProvider getNumber(String key, NumberProvider def) {
        return getValue(key, ConfigValue::getAsNumber, def);
    }

    public NumberProvider getNumber(String[] keys, NumberProvider def) {
        return getValue(keys, ConfigValue::getAsNumber, def);
    }

    public NumberProvider getNumber(String key, Supplier<NumberProvider> def) {
        return getValue(key, (Function<ConfigValue, NumberProvider>) ConfigValue::getAsNumber, def);
    }

    public NumberProvider getNumber(String[] keys, Supplier<NumberProvider> def) {
        return getValue(keys, (Function<ConfigValue, NumberProvider>) ConfigValue::getAsNumber, def);
    }

    // 枚举

    public <T extends Enum<T>> T getEnum(String key, Class<T> enumClass) {
        return getValue(key, v -> v.getAsEnum(enumClass));
    }

    public <T extends Enum<T>> T getEnum(String[] keys, Class<T> enumClass) {
        return getValue(keys, v -> v.getAsEnum(enumClass));
    }

    @NotNull
    public <T extends Enum<T>> T getNonNullEnum(String key, Class<T> enumClass) {
        return getNonNullValue(key, ConfigConstants.ARGUMENT_ENUM, v -> v.getAsEnum(enumClass));
    }

    @NotNull
    public <T extends Enum<T>> T getNonNullEnum(String[] keys, Class<T> enumClass) {
        return getNonNullValue(keys, ConfigConstants.ARGUMENT_ENUM, v -> v.getAsEnum(enumClass));
    }

    public <T extends Enum<T>> T getEnum(String key, Class<T> enumClass, T def) {
        return getValue(key, v -> v.getAsEnum(enumClass), def);
    }

    public <T extends Enum<T>> T getEnum(String[] keys, Class<T> enumClass, T def) {
        return getValue(keys, v -> v.getAsEnum(enumClass), def);
    }

    public <T extends Enum<T>> T getEnum(String key, Class<T> enumClass, Supplier<T> def) {
        return getValue(key, (Function<ConfigValue, T>) v -> v.getAsEnum(enumClass), def);
    }

    public <T extends Enum<T>> T getEnum(String[] keys, Class<T> enumClass, Supplier<T> def) {
        return getValue(keys, (Function<ConfigValue, T>) v -> v.getAsEnum(enumClass), def);
    }

    // 布尔值

    public boolean getBoolean(String key) {
        return getValue(key, ConfigValue::getAsBoolean, false);
    }

    public boolean getBoolean(String[] keys) {
        return getValue(keys, ConfigValue::getAsBoolean, false);
    }

    public boolean getNonNullBoolean(String key) {
        return getNonNullValue(key, ConfigConstants.ARGUMENT_BOOLEAN, ConfigValue::getAsBoolean);
    }

    public boolean getNonNullBoolean(String[] keys) {
        return getNonNullValue(keys, ConfigConstants.ARGUMENT_BOOLEAN, ConfigValue::getAsBoolean);
    }

    public boolean getBoolean(String key, boolean def) {
        return getValue(key, ConfigValue::getAsBoolean, def);
    }

    public boolean getBoolean(String[] keys, boolean def) {
        return getValue(keys, ConfigValue::getAsBoolean, def);
    }

    public boolean getBoolean(String key, Supplier<Boolean> def) {
        return getValue(key, (Function<ConfigValue, Boolean>) ConfigValue::getAsBoolean, def);
    }

    public boolean getBoolean(String[] keys, Supplier<Boolean> def) {
        return getValue(keys, (Function<ConfigValue, Boolean>) ConfigValue::getAsBoolean, def);
    }
    
    // 整数

    public int getInt(String key) {
        return getValue(key, ConfigValue::getAsInt, 0);
    }

    public int getInt(String[] keys) {
        return getValue(keys, ConfigValue::getAsInt, 0);
    }

    public int getNonNullInt(String key) {
        return getNonNullValue(key, ConfigConstants.ARGUMENT_INT, ConfigValue::getAsInt);
    }

    public int getNonNullInt(String[] keys) {
        return getNonNullValue(keys, ConfigConstants.ARGUMENT_INT, ConfigValue::getAsInt);
    }

    public int getInt(String key, int def) {
        return getValue(key, ConfigValue::getAsInt, def);
    }

    public int getInt(String[] keys, int def) {
        return getValue(keys, ConfigValue::getAsInt, def);
    }

    public int getInt(String key, Supplier<Integer> def) {
        return getValue(key, (Function<ConfigValue, Integer>) ConfigValue::getAsInt, def);
    }

    public int getInt(String[] keys, Supplier<Integer> def) {
        return getValue(keys, (Function<ConfigValue, Integer>) ConfigValue::getAsInt, def);
    }

    // 长整型

    public long getLong(String key) {
        return getValue(key, ConfigValue::getAsLong, 0L);
    }

    public long getLong(String[] keys) {
        return getValue(keys, ConfigValue::getAsLong, 0L);
    }

    public long getNonNullLong(String key) {
        return getNonNullValue(key, ConfigConstants.ARGUMENT_LONG, ConfigValue::getAsLong);
    }

    public long getNonNullLong(String[] keys) {
        return getNonNullValue(keys, ConfigConstants.ARGUMENT_LONG, ConfigValue::getAsLong);
    }

    public long getLong(String key, long def) {
        return getValue(key, ConfigValue::getAsLong, def);
    }

    public long getLong(String[] keys, long def) {
        return getValue(keys, ConfigValue::getAsLong, def);
    }

    public long getLong(String key, Supplier<Long> def) {
        return getValue(key, (Function<ConfigValue, Long>) ConfigValue::getAsLong, def);
    }

    public long getLong(String[] keys, Supplier<Long> def) {
        return getValue(keys, (Function<ConfigValue, Long>) ConfigValue::getAsLong, def);
    }

    // 浮点数

    public float getFloat(String key) {
        return getValue(key, ConfigValue::getAsFloat, 0f);
    }

    public float getFloat(String[] keys) {
        return getValue(keys, ConfigValue::getAsFloat, 0f);
    }

    public float getNonNullFloat(String key) {
        return getNonNullValue(key, ConfigConstants.ARGUMENT_FLOAT, ConfigValue::getAsFloat);
    }

    public float getNonNullFloat(String[] keys) {
        return getNonNullValue(keys, ConfigConstants.ARGUMENT_FLOAT, ConfigValue::getAsFloat);
    }

    public float getFloat(String key, float def) {
        return getValue(key, ConfigValue::getAsFloat, def);
    }

    public float getFloat(String[] keys, float def) {
        return getValue(keys, ConfigValue::getAsFloat, def);
    }

    public float getFloat(String key, Supplier<Float> def) {
        return getValue(key, (Function<ConfigValue, Float>) ConfigValue::getAsFloat, def);
    }

    public float getFloat(String[] keys, Supplier<Float> def) {
        return getValue(keys, (Function<ConfigValue, Float>) ConfigValue::getAsFloat, def);
    }

    // 双精度浮点数

    public double getDouble(String key) {
        return getValue(key, ConfigValue::getAsDouble, 0.0);
    }

    public double getDouble(String[] keys) {
        return getValue(keys, ConfigValue::getAsDouble, 0.0);
    }

    public double getNonNullDouble(String key) {
        return getNonNullValue(key, ConfigConstants.ARGUMENT_DOUBLE, ConfigValue::getAsDouble);
    }

    public double getNonNullDouble(String[] keys) {
        return getNonNullValue(keys, ConfigConstants.ARGUMENT_DOUBLE, ConfigValue::getAsDouble);
    }

    public double getDouble(String key, double def) {
        return getValue(key, ConfigValue::getAsDouble, def);
    }

    public double getDouble(String[] keys, double def) {
        return getValue(keys, ConfigValue::getAsDouble, def);
    }

    public double getDouble(String key, Supplier<Double> def) {
        return getValue(key, (Function<ConfigValue, Double>) ConfigValue::getAsDouble, def);
    }

    public double getDouble(String[] keys, Supplier<Double> def) {
        return getValue(keys, (Function<ConfigValue, Double>) ConfigValue::getAsDouble, def);
    }

    // 配置节点

    public ConfigSection getSection(String key) {
        return getValue(key, ConfigValue::getAsSection);
    }

    public ConfigSection getSection(String[] keys) {
        return getValue(keys, ConfigValue::getAsSection);
    }

    @NotNull
    public ConfigSection getNonNullSection(String key) {
        return getNonNullValue(key, ConfigConstants.ARGUMENT_SECTION, ConfigValue::getAsSection);
    }

    @NotNull
    public ConfigSection getNonNullSection(String[] keys) {
        return getNonNullValue(keys, ConfigConstants.ARGUMENT_SECTION, ConfigValue::getAsSection);
    }

    // 三维向量 (Float)

    @Nullable
    public Vector3f getVector3f(String key) {
        return getValue(key, ConfigValue::getAsVector3f);
    }

    @Nullable
    public Vector3f getVector3f(String[] keys) {
        return getValue(keys, ConfigValue::getAsVector3f);
    }

    @NotNull
    public Vector3f getNonNullVector3f(String key) {
        return getNonNullValue(key, ConfigConstants.ARGUMENT_VEC3, ConfigValue::getAsVector3f);
    }

    @NotNull
    public Vector3f getNonNullVector3f(String[] keys) {
        return getNonNullValue(keys, ConfigConstants.ARGUMENT_VEC3, ConfigValue::getAsVector3f);
    }

    public Vector3f getVector3f(String key, Vector3f def) {
        return getValue(key, ConfigValue::getAsVector3f, def);
    }

    public Vector3f getVector3f(String[] keys, Vector3f def) {
        return getValue(keys, ConfigValue::getAsVector3f, def);
    }

    public Vector3f getVector3f(String key, Supplier<Vector3f> def) {
        return getValue(key, (Function<ConfigValue, Vector3f>) ConfigValue::getAsVector3f, def);
    }

    public Vector3f getVector3f(String[] keys, Supplier<Vector3f> def) {
        return getValue(keys, (Function<ConfigValue, Vector3f>) ConfigValue::getAsVector3f, def);
    }

    // 四元数

    public Quaternionf getQuaternion(String key) {
        return getValue(key, ConfigValue::getAsQuaternion, ConfigConstants.ZERO_QUATERNION);
    }

    public Quaternionf getQuaternion(String[] keys) {
        return getValue(keys, ConfigValue::getAsQuaternion, ConfigConstants.ZERO_QUATERNION);
    }

    @NotNull
    public Quaternionf getNonNullQuaternion(String key) {
        return getNonNullValue(key, ConfigConstants.ARGUMENT_QUATERNION, ConfigValue::getAsQuaternion);
    }

    @NotNull
    public Quaternionf getNonNullQuaternion(String[] keys) {
        return getNonNullValue(keys, ConfigConstants.ARGUMENT_QUATERNION, ConfigValue::getAsQuaternion);
    }

    public Quaternionf getQuaternion(String key, Quaternionf def) {
        return getValue(key, ConfigValue::getAsQuaternion, def);
    }

    public Quaternionf getQuaternion(String[] keys, Quaternionf def) {
        return getValue(keys, ConfigValue::getAsQuaternion, def);
    }

    public Quaternionf getQuaternion(String key, Supplier<Quaternionf> def) {
        return getValue(key, (Function<ConfigValue, Quaternionf>) ConfigValue::getAsQuaternion, def);
    }

    public Quaternionf getQuaternion(String[] keys, Supplier<Quaternionf> def) {
        return getValue(keys, (Function<ConfigValue, Quaternionf>) ConfigValue::getAsQuaternion, def);
    }

    // 轴对齐包围盒 (AABB)

    public AABB getAABB(String key) {
        return getValue(key, ConfigValue::getAsAABB);
    }

    public AABB getAABB(String[] keys) {
        return getValue(keys, ConfigValue::getAsAABB);
    }

    @NotNull
    public AABB getNonNullAABB(String key) {
        return getNonNullValue(key, ConfigConstants.ARGUMENT_AABB, ConfigValue::getAsAABB);
    }

    @NotNull
    public AABB getNonNullAABB(String[] keys) {
        return getNonNullValue(keys, ConfigConstants.ARGUMENT_AABB, ConfigValue::getAsAABB);
    }

    public AABB getAABB(String key, AABB def) {
        return getValue(key, ConfigValue::getAsAABB, def);
    }

    public AABB getAABB(String[] keys, AABB def) {
        return getValue(keys, ConfigValue::getAsAABB, def);
    }

    public AABB getAABB(String key, Supplier<AABB> def) {
        return getValue(key, (Function<ConfigValue, AABB>) ConfigValue::getAsAABB, def);
    }

    public AABB getAABB(String[] keys, Supplier<AABB> def) {
        return getValue(keys, (Function<ConfigValue, AABB>) ConfigValue::getAsAABB, def);
    }

    // SNBT

    public Tag getSNBT(String key) {
        return getValue(key, ConfigValue::getAsSNBT);
    }

    public Tag getSNBT(String[] keys) {
        return getValue(keys, ConfigValue::getAsSNBT);
    }

    @NotNull
    public Tag getNonNullSNBT(String key) {
        return getNonNullValue(key, ConfigConstants.ARGUMENT_SNBT, ConfigValue::getAsSNBT);
    }

    @NotNull
    public Tag getNonNullSNBT(String[] keys) {
        return getNonNullValue(keys, ConfigConstants.ARGUMENT_SNBT, ConfigValue::getAsSNBT);
    }

    public Tag getSNBT(String key, Tag def) {
        return getValue(key, ConfigValue::getAsSNBT, def);
    }

    public Tag getSNBT(String[] keys, Tag def) {
        return getValue(keys, ConfigValue::getAsSNBT, def);
    }

    public Tag getSNBT(String key, Supplier<Tag> def) {
        return getValue(key, (Function<ConfigValue, Tag>) ConfigValue::getAsSNBT, def);
    }

    public Tag getSNBT(String[] keys, Supplier<Tag> def) {
        return getValue(keys, (Function<ConfigValue, Tag>) ConfigValue::getAsSNBT, def);
    }

    // 列表

    public List<Object> getNonEmptyList(String key) {
        return getNonNullValue(key, ConfigConstants.ARGUMENT_LIST, ConfigValue::getAsNonEmptyList);
    }

    public List<Object> getNonEmptyList(String[] keys) {
        return getNonNullValue(keys, ConfigConstants.ARGUMENT_LIST, ConfigValue::getAsNonEmptyList);
    }

    public List<Object> getNonNullList(String key) {
        return getNonNullValue(key, ConfigConstants.ARGUMENT_LIST, ConfigValue::getAsList);
    }

    public List<Object> getNonNullList(String[] keys) {
        return getNonNullValue(keys, ConfigConstants.ARGUMENT_LIST, ConfigValue::getAsList);
    }

    public List<Object> getList(String key) {
        return getValue(key, ConfigValue::getAsList, List.of());
    }

    public List<Object> getList(String[] keys) {
        return getValue(keys, ConfigValue::getAsList, List.of());
    }

    public List<Object> getList(String key, List<Object> def) {
        return getValue(key, ConfigValue::getAsList, def);
    }

    public List<Object> getList(String[] keys, List<Object> def) {
        return getValue(keys, ConfigValue::getAsList, def);
    }

    public List<Object> getList(String key, Supplier<List<Object>> def) {
        return getValue(key, (Function<ConfigValue, List<Object>>) ConfigValue::getAsList, def);
    }

    public List<Object> getList(String[] keys, Supplier<List<Object>> def) {
        return getValue(keys, (Function<ConfigValue, List<Object>>) ConfigValue::getAsList, def);
    }

    public List<String> getStringList(String key) {
        return getValue(key, ConfigValue::getAsStringList, List.of());
    }

    public List<String> getStringList(String[] keys) {
        return getValue(keys, ConfigValue::getAsStringList, List.of());
    }

    public List<String> getStringList(String key, List<String> def) {
        return getValue(key, ConfigValue::getAsStringList, def);
    }

    public List<String> getStringList(String[] keys, List<String> def) {
        return getValue(keys, ConfigValue::getAsStringList, def);
    }

    public List<String> getNonNullStringList(String key) {
        return getNonNullValue(key, ConfigConstants.ARGUMENT_LIST, ConfigValue::getAsStringList);
    }

    public List<String> getNonNullStringList(String[] keys) {
        return getNonNullValue(keys, ConfigConstants.ARGUMENT_LIST, ConfigValue::getAsStringList);
    }

    // 杂项

    public <T> List<T> getNonEmptyList(String key, Function<ConfigValue, T> parser) {
        ConfigValue value = getNonNullValue(key, ConfigConstants.ARGUMENT_LIST);
        List<ConfigValue> list = value.getAsNonEmptyValueList();
        List<T> result = new ArrayList<>(list.size());
        for (ConfigValue configValue : list) {
            result.add(parser.apply(configValue));
        }
        return result;
    }

    public <T> List<T> getNonEmptyList(String[] keys, Function<ConfigValue, T> parser) {
        ConfigValue value = getNonNullValue(keys, ConfigConstants.ARGUMENT_LIST);
        List<ConfigValue> list = value.getAsNonEmptyValueList();
        List<T> result = new ArrayList<>(list.size());
        for (ConfigValue configValue : list) {
            result.add(parser.apply(configValue));
        }
        return result;
    }

    public <T> List<T> getList(String key, Function<ConfigValue, T> parser) {
        ConfigValue value = getValue(key);
        if (value == null) {
            return List.of();
        }
        List<ConfigValue> list = value.getAsValueList();
        List<T> result = new ArrayList<>(list.size());
        for (ConfigValue configValue : list) {
            result.add(parser.apply(configValue));
        }
        return result;
    }

    public <T> List<T> getList(String[] keys, Function<ConfigValue, T> parser) {
        ConfigValue value = getValue(keys);
        if (value == null) {
            return List.of();
        }
        List<ConfigValue> list = value.getAsValueList();
        List<T> result = new ArrayList<>(list.size());
        for (ConfigValue configValue : list) {
            result.add(parser.apply(configValue));
        }
        return result;
    }

    public <T> List<T> getSectionList(String key, Function<ConfigSection, T> parser) {
        ConfigValue value = getValue(key);
        if (value == null) {
            return List.of();
        }
        List<ConfigValue> list = value.getAsValueList();
        List<T> result = new ArrayList<>(list.size());
        for (ConfigValue configValue : list) {
            result.add(parser.apply(configValue.getAsSection()));
        }
        return result;
    }

    public <T> List<T> getSectionList(String[] keys, Function<ConfigSection, T> parser) {
        ConfigValue value = getValue(keys);
        if (value == null) {
            return List.of();
        }
        List<ConfigValue> list = value.getAsValueList();
        List<T> result = new ArrayList<>(list.size());
        for (ConfigValue configValue : list) {
            result.add(parser.apply(configValue.getAsSection()));
        }
        return result;
    }
}