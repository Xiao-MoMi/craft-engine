package net.momirealms.craftengine.core.plugin.context;

import net.momirealms.craftengine.core.util.Key;
import net.momirealms.sparrow.nbt.CompoundTag;
import net.momirealms.sparrow.nbt.LongTag;
import net.momirealms.sparrow.nbt.NBT;
import net.momirealms.sparrow.nbt.Tag;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class CooldownData {
    public static final Key COOLDOWN_KEY = Key.ce("cooldown");
    private final Map<String, Long> cooldownMap = Collections.synchronizedMap(new HashMap<>());

    public boolean isOnCooldown(String key) {
        if (this.cooldownMap.containsKey(key)) {
            long currentTime = System.currentTimeMillis();
            long expirationTime = this.cooldownMap.get(key);
            return currentTime < expirationTime;
        }
        return false;
    }

    public void setCooldown(String key, long duration) {
        this.cooldownMap.put(key, System.currentTimeMillis() + duration);
    }

    public void addCooldown(String key, long duration) {
        if (this.cooldownMap.containsKey(key)) {
            this.cooldownMap.put(key, this.cooldownMap.get(key) + duration);
        } else {
            setCooldown(key, duration);
        }
    }

    public void removeCooldown(String key) {
        this.cooldownMap.remove(key);
    }

    public void clearCooldowns() {
        this.cooldownMap.clear();
    }

    public Long getExpirationTime(String key) {
        return this.cooldownMap.get(key);
    }

    public static byte[] toBytes(CooldownData data) throws IOException {
        CompoundTag tag = new CompoundTag();
        long currentTime = System.currentTimeMillis();
        for (Map.Entry<String, Long> entry : data.cooldownMap.entrySet()) {
            if (currentTime < entry.getValue()) {
                tag.putLong(entry.getKey(), entry.getValue());
            }
        }
        return NBT.toBytes(tag);
    }

    public static CooldownData fromBytes(byte[] data) throws IOException {
        if (data == null || data.length == 0) return new CooldownData();
        CooldownData cd = new CooldownData();
        long currentTime = System.currentTimeMillis();
        CompoundTag tag = NBT.fromBytes(data);
        if (tag != null) {
            for (Map.Entry<String, Tag> entry : tag.tags.entrySet()) {
                if (entry.getValue() instanceof LongTag longTag) {
                    long expire = longTag.getAsLong();
                    if (currentTime < expire) {
                        cd.cooldownMap.put(entry.getKey(), expire);
                    }
                }
            }
        }
        return cd;
    }

    @Override
    public String toString() {
        return "CooldownData{" +
                "cooldownMap=" + cooldownMap +
                '}';
    }
}