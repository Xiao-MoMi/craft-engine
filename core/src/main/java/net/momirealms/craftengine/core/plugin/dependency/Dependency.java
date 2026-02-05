package net.momirealms.craftengine.core.plugin.dependency;

import net.momirealms.craftengine.core.plugin.PluginProperties;
import net.momirealms.craftengine.core.plugin.dependency.relocation.Relocation;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Predicate;

public class Dependency {
    private final String id;
    private final String groupId;
    private final String rawArtifactId;
    private final List<Relocation> relocations;
    private final boolean shared;
    private final String jarInJarPath;

    public Dependency(String id, String groupId, String artifactId, List<Relocation> relocations) {
        this(id, groupId, artifactId, relocations, false);
    }

    public Dependency(String id, String groupId, String artifactId, List<Relocation> relocations, boolean shared) {
        this(id, groupId, artifactId, relocations, shared, null);
    }

    public Dependency(String id, String groupId, String artifactId, List<Relocation> relocations, boolean shared, String jarInJarPath) {
        this.id = id;
        this.groupId = groupId;
        this.rawArtifactId = artifactId;
        this.relocations = relocations;
        this.shared = shared;
        this.jarInJarPath = jarInJarPath;
    }

    public boolean shared() {
        return this.shared;
    }

    public String id() {
        return this.id;
    }

    public String groupId() {
        return this.groupId;
    }

    public String rawArtifactId() {
        return this.rawArtifactId;
    }

    public List<Relocation> relocations() {
        return this.relocations;
    }

    public String toLocalPath() {
        return rewriteEscaping(this.groupId).replace(".", "/") + "/" + this.rawArtifactId + "/" + getVersion();
    }

    public boolean hasJarInJarPath() {
        return this.jarInJarPath != null;
    }

    public String jarInJarPath() {
        return jarInJarPath;
    }

    private static final String MAVEN_FORMAT = "%s/%s/%s/%s-%s.jar";

    public String mavenPath() {
        return String.format(MAVEN_FORMAT,
                rewriteEscaping(this.groupId).replace(".", "/"),
                rewriteEscaping(this.rawArtifactId),
                getVersion(),
                rewriteEscaping(this.rawArtifactId),
                getVersion()
        );
    }

    public String fileName(String classifier) {
        String name = this.rawArtifactId.toLowerCase(Locale.ENGLISH).replace('_', '-');
        String extra = classifier == null || classifier.isEmpty()
                ? ""
                : "-" + classifier;
        return name + "-" + this.getVersion() + extra + ".jar";
    }

    public String getVersion() {
        return PluginProperties.getValue(this.id);
    }

    public static String rewriteEscaping(String s) {
        return s.replace("{}", ".");
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Dependency that)) return false;
        return Objects.equals(this.id, that.id);
    }

    @Override
    public int hashCode() {
        return this.id.hashCode();
    }

    @Override
    public String toString() {
        return "Dependency{" +
                "id='" + id + '\'' +
                ", groupId='" + groupId + '\'' +
                '}';
    }
}
