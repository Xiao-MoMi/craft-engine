package net.momirealms.craftengine.core.world.chunk;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// Adapted Moonrise's per-thread palette packing scratch space.
final class PaletteReadoutCache {
    private final int[] indices = new int[4096];
    private final int[] paletteMapping = new int[4096];
    private final Int2IntOpenHashMap largePaletteMapping = new Int2IntOpenHashMap();
    private final Reference2IntOpenHashMap<Object> entryMapping = new Reference2IntOpenHashMap<>();

    PaletteReadoutCache() {
        this.largePaletteMapping.defaultReturnValue(-1);
        this.entryMapping.defaultReturnValue(-1);
    }

    <T> List<T> read(PaletteStorage storage, Palette<T> palette) {
        storage.writePaletteIndices(this.indices);
        boolean smallPalette = storage.getElementBits() <= 12;
        if (smallPalette) {
            Arrays.fill(this.paletteMapping, 0, 1 << storage.getElementBits(), -1);
        } else {
            this.largePaletteMapping.clear();
        }
        List<T> entries = new ArrayList<>(Math.min(palette.getSize(), storage.size()));
        try {
            for (int i = 0; i < storage.size(); i++) {
                int oldId = this.indices[i];
                int newId = smallPalette ? this.paletteMapping[oldId] : this.largePaletteMapping.get(oldId);
                if (newId == -1) {
                    T entry = palette.get(oldId);
                    // remap() can make different palette IDs refer to the same object.
                    newId = this.entryMapping.getInt(entry);
                    if (newId == -1) {
                        newId = entries.size();
                        entries.add(entry);
                        this.entryMapping.put(entry, newId);
                    }
                    if (smallPalette) {
                        this.paletteMapping[oldId] = newId;
                    } else {
                        this.largePaletteMapping.put(oldId, newId);
                    }
                }
                this.indices[i] = newId;
            }
            return entries;
        } finally {
            // Save workers must not retain block states between serializations.
            this.entryMapping.clear();
        }
    }

    long[] repack(int bits, int size) {
        return new PackedIntegerArray(bits, size, this.indices).getData();
    }
}
