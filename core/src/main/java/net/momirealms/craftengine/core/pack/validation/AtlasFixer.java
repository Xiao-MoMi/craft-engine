package net.momirealms.craftengine.core.pack.validation;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.pack.mcmeta.PackVersion;

import java.util.ArrayList;
import java.util.List;

final class AtlasFixer {
    private final List<Entry> entries;

    public AtlasFixer() {
        this.entries = new ArrayList<>();
    }

    // 理论是从小到大加的
    public void addEntry(PackVersion min, PackVersion max, JsonObject atlas) {
        if (!atlas.has("sources")) {
            atlas.add("sources", new JsonArray());
        }
        if (this.entries.isEmpty()) {
            this.entries.add(new Entry(min, max, atlas));
        } else {
            Entry last = this.entries.getLast();
            // 相同则扩大区间
            if (last.atlas.equals(atlas)) {
                last.setMax(max);
            }
            // 不同则另加元素
            else {
                this.entries.add(new Entry(min, max, atlas));
            }
        }
    }

    public List<Entry> entries() {
        return this.entries;
    }

    protected static class Entry {
        private final JsonObject atlas;
        private PackVersion min, max;

        public Entry(PackVersion min, PackVersion max, JsonObject atlas) {
            this.min = min;
            this.max = max;
            this.atlas = atlas;
        }

        public PackVersion min() {
            return this.min;
        }

        public PackVersion max() {
            return this.max;
        }

        public JsonObject atlas() {
            return this.atlas;
        }

        public void setMin(PackVersion min) {
            this.min = min;
        }

        public void setMax(PackVersion max) {
            this.max = max;
        }
    }
}
