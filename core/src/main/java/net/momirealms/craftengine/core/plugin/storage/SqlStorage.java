package net.momirealms.craftengine.core.plugin.storage;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.jdbi.v3.core.Jdbi;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class SqlStorage implements Storage {
    private final SqlDialect type;
    private final HikariDataSource pool;
    private final Jdbi jdbi;
    private final String insertPackPreferences;
    private final String selectPackPreferencesForUpdate;

    public SqlStorage(StorageConfig.Sql settings) {
        this.type = settings.type();
        String insert = "INSERT INTO ce_pack_preferences (player_id, preferences) VALUES (:player, '{}')";
        this.insertPackPreferences = switch (this.type) {
            case MYSQL, MARIADB -> insert + " ON DUPLICATE KEY UPDATE player_id = :player";
            case H2 -> "MERGE INTO ce_pack_preferences (player_id) KEY (player_id) VALUES (:player)";
            default -> insert + " ON CONFLICT (player_id) DO NOTHING";
        };
        // SQLite's initial INSERT acquires the write lock; other SQL backends lock the player's row.
        this.selectPackPreferencesForUpdate = "SELECT preferences FROM ce_pack_preferences WHERE player_id = :player"
                + (this.type == SqlDialect.SQLITE ? "" : " FOR UPDATE");
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(settings.url());
        config.setDriverClassName(this.type.driverClassName());
        config.setUsername(settings.username());
        config.setPassword(settings.password());
        config.setMaximumPoolSize(settings.poolSize());
        config.setMinimumIdle(1);
        config.setPoolName("craftengine-storage");
        this.pool = new HikariDataSource(config);
        try {
            Flyway.configure(getClass().getClassLoader()).dataSource(this.pool)
                    .baselineOnMigrate(true).baselineVersion("0")
                    .table("ce_schema_history").locations("classpath:db/migration/sql").load().migrate();
            this.jdbi = Jdbi.create(this.pool);
        } catch (Throwable e) {
            this.pool.close();
            throw e;
        }
    }

    public SqlDialect type() {
        return this.type;
    }

    public Jdbi jdbi() {
        return this.jdbi;
    }

    @Override
    public Map<String, Boolean> loadPackPreferences(UUID player) {
        String preferences = this.jdbi.withHandle(handle -> handle.createQuery("SELECT preferences FROM ce_pack_preferences WHERE player_id = :player")
                .bind("player", player.toString()).mapTo(String.class).findOne().orElse("{}"));
        Map<String, Boolean> states = new HashMap<>();
        JsonParser.parseString(preferences).getAsJsonObject().entrySet()
                .forEach(entry -> states.put(entry.getKey(), entry.getValue().getAsBoolean()));
        return states;
    }

    @Override
    public void setPackPreferences(UUID player, Map<String, Boolean> updates) {
        this.jdbi.useTransaction(handle -> {
            handle.createUpdate(this.insertPackPreferences).bind("player", player.toString()).execute();
            String stored = handle.createQuery(this.selectPackPreferencesForUpdate).bind("player", player.toString()).mapTo(String.class).one();
            JsonObject preferences = JsonParser.parseString(stored).getAsJsonObject();
            updates.forEach((pack, enabled) -> {
                if (enabled == null) preferences.remove(pack);
                else preferences.addProperty(pack, enabled);
            });
            handle.createUpdate("UPDATE ce_pack_preferences SET preferences = :preferences WHERE player_id = :player")
                    .bind("player", player.toString()).bind("preferences", preferences.toString()).execute();
        });
    }

    @Override
    public void close() {
        this.pool.close();
    }
}
