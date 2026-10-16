package net.momirealms.craftengine.bedrock;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.TreeMap;

public final class BedrockPackMappingsCodec {

    private BedrockPackMappingsCodec() {
    }

    public static BedrockPackMappings read(Reader input) throws IOException {
        JsonReader reader = new JsonReader(input);
        int version = -1;
        String javaVersion = null;
        String sha1 = null;
        List<BedrockPackMappings.Item> items = null;
        try {
            reader.beginObject();
            while (reader.hasNext()) {
                switch (reader.nextName()) {
                    case "format_version" -> version = reader.nextInt();
                    case "java_version" -> javaVersion = reader.nextString();
                    case "source_sha1" -> sha1 = reader.nextString();
                    case "items" -> {
                        items = new ArrayList<>();
                        reader.beginArray();
                        while (reader.hasNext()) {
                            items.add(readItem(reader));
                        }
                        reader.endArray();
                    }
                    default -> reader.skipValue();
                }
            }
            reader.endObject();
            if (reader.peek() != JsonToken.END_DOCUMENT) {
                throw new IOException("Unexpected data after Bedrock mappings");
            }
        } catch (IllegalStateException | NumberFormatException exception) {
            throw new IOException("Invalid Bedrock mappings at " + reader.getPath(), exception);
        }
        if (version < 1 || version > BedrockPackMappings.FORMAT_VERSION) {
            throw new IOException("Unsupported Bedrock mappings format_version: " + version);
        }
        if (javaVersion == null || sha1 == null || items == null) {
            throw new IOException("Missing Bedrock mappings metadata or items");
        }
        return new BedrockPackMappings(version, javaVersion, sha1, items);
    }

    private static BedrockPackMappings.Item readItem(JsonReader reader) throws IOException {
        String id = null;
        String javaItem = null;
        String model = null;
        String bedrockIdentifier = null;
        String icon = "stick";
        boolean displayHandheld = false;
        BedrockPackMappings.DisplayBlock displayBlock = null;
        reader.beginObject();
        while (reader.hasNext()) {
            switch (reader.nextName()) {
                case "id" -> id = reader.nextString();
                case "java_item" -> javaItem = reader.nextString();
                case "java_item_model" -> model = reader.nextString();
                case "bedrock_identifier" -> bedrockIdentifier = reader.nextString();
                case "icon" -> icon = reader.nextString();
                case "display_handheld" -> displayHandheld = reader.nextBoolean();
                case "display_block" -> displayBlock = readDisplayBlock(reader);
                default -> reader.skipValue();
            }
        }
        reader.endObject();
        if (id == null || javaItem == null || model == null || bedrockIdentifier == null
                || id.isEmpty() || javaItem.isEmpty() || model.isEmpty() || bedrockIdentifier.isEmpty()) {
            throw new IOException("Incomplete Bedrock item mapping at " + reader.getPath());
        }
        return new BedrockPackMappings.Item(id, javaItem, model, bedrockIdentifier, displayBlock == null ? icon : null, displayBlock, displayHandheld);
    }

    private static BedrockPackMappings.DisplayBlock readDisplayBlock(JsonReader reader) throws IOException {
        String name = null;
        String geometry = null;
        String texture = null;
        Map<String, String> materials = Map.of();
        reader.beginObject();
        while (reader.hasNext()) {
            switch (reader.nextName()) {
                case "name" -> name = reader.nextString();
                case "geometry" -> geometry = reader.nextString();
                case "texture" -> texture = reader.nextString();
                case "materials" -> {
                    materials = new LinkedHashMap<>();
                    reader.beginObject();
                    while (reader.hasNext()) {
                        materials.put(reader.nextName(), reader.nextString());
                    }
                    reader.endObject();
                }
                default -> reader.skipValue();
            }
        }
        reader.endObject();
        if (name == null || name.isEmpty() || geometry == null || geometry.isEmpty() || texture == null || texture.isEmpty()) {
            throw new IOException("Incomplete display block at " + reader.getPath());
        }
        return new BedrockPackMappings.DisplayBlock(name, geometry, texture, materials);
    }

    public static void write(BedrockPackMappings mappings, Writer output) throws IOException {
        JsonWriter writer = new JsonWriter(output);
        writer.beginObject();
        writer.name("format_version").value(mappings.formatVersion());
        writer.name("java_version").value(mappings.javaVersion());
        writer.name("source_sha1").value(mappings.sourceSha1());
        writer.name("items").beginArray();
        for (BedrockPackMappings.Item item : mappings.items()) {
            writer.beginObject();
            writer.name("id").value(item.id());
            writer.name("java_item").value(item.javaItem());
            writer.name("java_item_model").value(item.javaItemModel());
            writer.name("bedrock_identifier").value(item.bedrockIdentifier());
            if (item.displayHandheld()) {
                writer.name("display_handheld").value(true);
            }
            if (item.displayBlock() == null) {
                writer.name("icon").value(item.icon());
            } else {
                BedrockPackMappings.DisplayBlock block = item.displayBlock();
                writer.name("display_block").beginObject();
                writer.name("name").value(block.name());
                writer.name("geometry").value(block.geometry());
                writer.name("texture").value(block.texture());
                if (!block.materials().isEmpty()) {
                    writer.name("materials").beginObject();
                    for (Map.Entry<String, String> material : new TreeMap<>(block.materials()).entrySet()) {
                        writer.name(material.getKey()).value(material.getValue());
                    }
                    writer.endObject();
                }
                writer.endObject();
            }
            writer.endObject();
        }
        writer.endArray();
        writer.endObject();
        writer.flush();
    }
}
