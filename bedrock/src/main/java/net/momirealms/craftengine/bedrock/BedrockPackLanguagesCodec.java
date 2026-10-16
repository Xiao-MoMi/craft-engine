package net.momirealms.craftengine.bedrock;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Map;
import java.util.TreeMap;

public final class BedrockPackLanguagesCodec {

    private BedrockPackLanguagesCodec() {
    }

    public static BedrockPackLanguages read(Reader input) throws IOException {
        JsonReader reader = new JsonReader(input);
        Map<String, Map<String, String>> locales = new TreeMap<>();
        Map<String, String> names = Map.of();
        int version = -1;
        try {
            reader.beginObject();
            while (reader.hasNext()) {
                switch (reader.nextName()) {
                    case "format_version" -> version = reader.nextInt();
                    case "locales" -> {
                        reader.beginObject();
                        while (reader.hasNext()) {
                            String locale = reader.nextName();
                            if (!BedrockPackLanguages.isLocale(locale)) {
                                throw new IOException("Invalid language locale: " + locale);
                            }
                            locales.put(locale, readStrings(reader));
                        }
                        reader.endObject();
                    }
                    case "item_names" -> names = readStrings(reader);
                    default -> reader.skipValue();
                }
            }
            reader.endObject();
            if (reader.peek() != JsonToken.END_DOCUMENT) {
                throw new IOException("Unexpected data after Bedrock languages");
            }
        } catch (IllegalStateException | NumberFormatException exception) {
            throw new IOException("Invalid Bedrock languages at " + reader.getPath(), exception);
        }
        if (version != 1) {
            throw new IOException("Unsupported Bedrock languages format_version: " + version);
        }
        return new BedrockPackLanguages(locales, names);
    }

    private static Map<String, String> readStrings(JsonReader reader) throws IOException {
        Map<String, String> strings = new TreeMap<>();
        reader.beginObject();
        while (reader.hasNext()) {
            strings.put(reader.nextName(), reader.nextString());
        }
        reader.endObject();
        return strings;
    }

    public static void write(BedrockPackLanguages languages, Writer output) throws IOException {
        JsonWriter writer = new JsonWriter(output);
        writer.beginObject();
        writer.name("format_version").value(1);
        writer.name("locales").beginObject();
        for (Map.Entry<String, Map<String, String>> locale : new TreeMap<>(languages.locales()).entrySet()) {
            writer.name(locale.getKey());
            writeStrings(writer, locale.getValue());
        }
        writer.endObject();
        writer.name("item_names");
        writeStrings(writer, languages.itemNames());
        writer.endObject();
        writer.flush();
    }

    private static void writeStrings(JsonWriter writer, Map<String, String> strings) throws IOException {
        writer.beginObject();
        for (Map.Entry<String, String> entry : new TreeMap<>(strings).entrySet()) {
            writer.name(entry.getKey()).value(entry.getValue());
        }
        writer.endObject();
    }
}
