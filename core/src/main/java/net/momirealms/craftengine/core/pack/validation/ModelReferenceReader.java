package net.momirealms.craftengine.core.pack.validation;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class ModelReferenceReader {

    private ModelReferenceReader() {
    }

    static List<String> readItem(JsonReader reader) throws IOException {
        List<String> references = new ArrayList<>();
        readItemObject(reader, references);
        return references;
    }

    private static void readItemObject(JsonReader reader, List<String> references) throws IOException {
        int start = references.size();
        String model = null;
        String type = null;
        String base = null;
        Set<String> fields = new HashSet<>(4);
        reader.beginObject();
        while (reader.hasNext()) {
            String name = nextUniqueName(reader, fields);
            JsonToken token = reader.peek();
            if (isPrimitive(token)) {
                switch (name) {
                    case "model" -> model = readPrimitive(reader);
                    case "type" -> type = readPrimitive(reader);
                    case "base" -> base = readPrimitive(reader);
                    default -> reader.skipValue();
                }
            } else if (model != null) {
                reader.skipValue();
            } else if (token == JsonToken.BEGIN_OBJECT) {
                readItemObject(reader, references);
            } else if (token == JsonToken.BEGIN_ARRAY) {
                reader.beginArray();
                while (reader.hasNext()) {
                    if (reader.peek() == JsonToken.BEGIN_OBJECT) {
                        readItemObject(reader, references);
                    } else {
                        reader.skipValue();
                    }
                }
                reader.endArray();
            } else {
                reader.skipValue();
            }
        }
        reader.endObject();
        // A direct model suppresses all nested references, regardless of field order.
        if (model != null) {
            references.subList(start, references.size()).clear();
            references.add(model);
        } else if (base != null && ("special".equals(type) || "minecraft:special".equals(type))) {
            references.add(start, base);
        }
    }

    static List<BlockReference> readBlockState(JsonReader reader) throws IOException {
        List<BlockReference> multipart = null;
        List<BlockReference> variants = List.of();
        Set<String> fields = new HashSet<>(4);
        reader.beginObject();
        while (reader.hasNext()) {
            switch (nextUniqueName(reader, fields)) {
                case "multipart" -> multipart = readMultipart(reader);
                case "variants" -> variants = readVariants(reader);
                default -> reader.skipValue();
            }
        }
        reader.endObject();
        return multipart == null ? variants : multipart;
    }

    private static List<BlockReference> readMultipart(JsonReader reader) throws IOException {
        require(reader, JsonToken.BEGIN_ARRAY);
        List<BlockReference> references = new ArrayList<>();
        reader.beginArray();
        while (reader.hasNext()) {
            if (reader.peek() != JsonToken.BEGIN_OBJECT) {
                reader.skipValue();
                continue;
            }
            Set<String> fields = new HashSet<>(4);
            reader.beginObject();
            while (reader.hasNext()) {
                if (nextUniqueName(reader, fields).equals("apply")) {
                    readModels(reader, null, references);
                } else {
                    reader.skipValue();
                }
            }
            reader.endObject();
        }
        reader.endArray();
        return references;
    }

    private static List<BlockReference> readVariants(JsonReader reader) throws IOException {
        require(reader, JsonToken.BEGIN_OBJECT);
        List<BlockReference> references = new ArrayList<>();
        Set<String> fields = new HashSet<>(4);
        reader.beginObject();
        while (reader.hasNext()) {
            String variant = nextUniqueName(reader, fields);
            readModels(reader, variant, references);
        }
        reader.endObject();
        return references;
    }

    private static void readModels(JsonReader reader, String variant, List<BlockReference> references) throws IOException {
        if (reader.peek() == JsonToken.BEGIN_OBJECT) {
            references.add(new BlockReference(readModel(reader), variant));
        } else if (reader.peek() == JsonToken.BEGIN_ARRAY) {
            reader.beginArray();
            while (reader.hasNext()) {
                if (reader.peek() == JsonToken.BEGIN_OBJECT) {
                    references.add(new BlockReference(readModel(reader), variant));
                } else {
                    reader.skipValue();
                }
            }
            reader.endArray();
        } else {
            reader.skipValue();
        }
    }

    private static String readModel(JsonReader reader) throws IOException {
        String model = null;
        Set<String> fields = new HashSet<>(4);
        reader.beginObject();
        while (reader.hasNext()) {
            if (nextUniqueName(reader, fields).equals("model")) {
                if (!isPrimitive(reader.peek())) {
                    throw new TreeReadingRequired();
                }
                model = readPrimitive(reader);
            } else {
                reader.skipValue();
            }
        }
        reader.endObject();
        if (model == null) {
            throw new TreeReadingRequired();
        }
        return model;
    }

    private static String nextUniqueName(JsonReader reader, Set<String> fields) throws IOException {
        String name = reader.nextName();
        if (!fields.add(name)) {
            // Preserve the tree reader's last-value-wins behavior for duplicate fields.
            throw new TreeReadingRequired();
        }
        return name;
    }

    private static boolean isPrimitive(JsonToken token) {
        return token == JsonToken.STRING || token == JsonToken.NUMBER || token == JsonToken.BOOLEAN;
    }

    private static String readPrimitive(JsonReader reader) throws IOException {
        return reader.peek() == JsonToken.BOOLEAN ? Boolean.toString(reader.nextBoolean()) : reader.nextString();
    }

    private static void require(JsonReader reader, JsonToken token) throws IOException {
        if (reader.peek() != token) {
            throw new TreeReadingRequired();
        }
    }

    record BlockReference(String model, String variant) {
    }

    static final class TreeReadingRequired extends RuntimeException {

        private TreeReadingRequired() {
            super(null, null, false, false);
        }
    }
}
