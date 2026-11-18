package net.momirealms.craftengine.core.pack;

import com.google.gson.JsonElement;
import net.momirealms.craftengine.core.util.GsonHelper;
import org.apache.commons.io.output.StringBuilderWriter;

final class PackJsonWriter {
    private PackJsonWriter() {
    }

    static String toJson(JsonElement json) {
        StringBuilderWriter output = new StringBuilderWriter() {
            // Avoid Writer's synchronized char-array fallback for JsonWriter's small writes.
            @Override
            public void write(int value) {
                getBuilder().append((char) value);
            }

            @Override
            public void write(String value, int offset, int length) {
                getBuilder().append(value, offset, offset + length);
            }
        };
        GsonHelper.writeJson(json, output);
        return output.toString();
    }
}
