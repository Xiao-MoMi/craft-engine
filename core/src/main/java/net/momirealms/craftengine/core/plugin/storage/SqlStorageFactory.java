package net.momirealms.craftengine.core.plugin.storage;

import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.dependency.Dependencies;
import net.momirealms.craftengine.core.plugin.dependency.Dependency;

import java.util.ArrayList;
import java.util.List;

public final class SqlStorageFactory implements StorageFactory<SqlStorage> {
    private final SqlDialect dialect;

    public SqlStorageFactory(SqlDialect dialect) {
        this.dialect = dialect;
    }

    @Override
    public SqlStorage create(ConfigSection section) {
        StorageConfig.Sql settings = StorageConfig.Sql.fromConfig(this.dialect, section);
        CraftEngine.instance().dependencyManager().loadDependencies(dependencies(this.dialect));
        return new SqlStorage(settings);
    }

    static List<Dependency> dependencies(SqlDialect type) {
        List<Dependency> dependencies = new ArrayList<>(List.of(
                Dependencies.JACKSON_ANNOTATIONS,
                Dependencies.JACKSON_CORE,
                Dependencies.JACKSON_DATABIND,
                Dependencies.FLYWAY_CORE,
                Dependencies.HIKARI,
                Dependencies.GEANTY_REF,
                Dependencies.JDBI3_CORE,
                Dependencies.SLF4J
        ));
        dependencies.addAll(switch (type) {
            case H2 -> List.of(Dependencies.H2);
            case MARIADB -> List.of(
                Dependencies.FLYWAY_MYSQL,
                Dependencies.MARIADB_JAVA_CLIENT
            );
            case MYSQL -> List.of(
                Dependencies.PROTOBUF_JAVA,
                Dependencies.MYSQL_CONNECTOR_J,
                Dependencies.FLYWAY_MYSQL
            );
            case POSTGRESQL -> List.of(
                Dependencies.CHECKER_QUAL,
                Dependencies.FLYWAY_DATABASE_POSTGRESQL,
                Dependencies.POSTGRESQL
            );
            case SQLITE -> List.of(Dependencies.SQLITE_JDBC);
        });
        return dependencies;
    }
}
