package net.momirealms.craftengine.core.plugin.storage;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.bson.UuidRepresentation;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.mongodb.client.model.Filters.eq;

public final class MongoStorage implements Storage {
    private final MongoClient client;
    private final MongoDatabase database;
    private final MongoCollection<Document> packPreferences;

    public MongoStorage(StorageConfig.Mongo settings) {
        String url = settings.url();
        String username = settings.username();
        String password = settings.password();
        ConnectionString connection = new ConnectionString(url);
        if (connection.getDatabase() == null)
            throw new IllegalArgumentException("MongoDB URL must include the database name");
        this.client = MongoClients.create(clientSettings(url, username, password));
        this.database = this.client.getDatabase(connection.getDatabase());
        this.packPreferences = this.database.getCollection("ce_pack_preferences");
    }

    static MongoClientSettings clientSettings(String url, String username, String password) {
        ConnectionString connection = new ConnectionString(url);
        MongoClientSettings.Builder settings = MongoClientSettings.builder()
                .applyConnectionString(connection)
                .uuidRepresentation(UuidRepresentation.STANDARD);
        if (connection.getCredential() == null && !username.isBlank()) {
            String authSource = connection.getDatabase() == null ? "admin" : connection.getDatabase();
            String query = URI.create(url).getRawQuery();
            if (query != null) {
                for (String parameter : query.split("&")) {
                    String[] pair = parameter.split("=", 2);
                    if (pair.length == 2 && pair[0].equalsIgnoreCase("authSource")) {
                        authSource = URLDecoder.decode(pair[1], StandardCharsets.UTF_8);
                    }
                }
            }
            settings.credential(MongoCredential.createCredential(username, authSource, password.toCharArray()));
        }
        return settings.build();
    }

    public MongoDatabase database() {
        return this.database;
    }

    @Override
    public Map<String, Boolean> loadPackPreferences(UUID player) {
        Map<String, Boolean> states = new HashMap<>(4);
        Document playerData = this.packPreferences.find(eq("_id", player)).first();
        if (playerData == null) return states;
        Document preferences = playerData.get("preferences", Document.class);
        if (preferences != null) preferences.forEach((pack, enabled) -> states.put(pack, (Boolean) enabled));
        return states;
    }

    @Override
    public void setPackPreferences(UUID player, Map<String, Boolean> updates) {
        if (updates.isEmpty()) return;
        this.packPreferences.updateOne(eq("_id", player), Updates.combine(updates.entrySet().stream().map(entry -> {
            String field = "preferences." + entry.getKey();
            return entry.getValue() == null ? Updates.unset(field) : Updates.set(field, entry.getValue());
        }).toList()), new UpdateOptions().upsert(true));
    }

    @Override
    public void close() {
        this.client.close();
    }
}
