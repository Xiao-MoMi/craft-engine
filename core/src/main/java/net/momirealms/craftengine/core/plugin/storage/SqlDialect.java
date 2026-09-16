package net.momirealms.craftengine.core.plugin.storage;

import java.util.Locale;

public enum SqlDialect {
    SQLITE,
    H2,
    MYSQL,
    MARIADB,
    POSTGRESQL;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String driverClassName() {
        return switch (this) {
            case SQLITE -> "org.sqlite.JDBC";
            case H2 -> "org.h2.Driver";
            case MYSQL -> "com.mysql.cj.jdbc.Driver";
            case MARIADB -> "org.mariadb.jdbc.Driver";
            case POSTGRESQL -> "org.postgresql.Driver";
        };
    }
}
