/*
 * Copyright (C) <2025> <XiaoMoMi>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package net.momirealms.craftengine.core.plugin.dependency;

import net.momirealms.craftengine.core.plugin.PluginProperties;
import net.momirealms.craftengine.core.plugin.dependency.relocation.Relocation;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class Dependency {
    private final String id;
    private final String groupId;
    private final String rawArtifactId;
    private final String customArtifactID;
    private final List<Relocation> relocations;

    public Dependency(String id, String groupId, String rawArtifactId, String customArtifactID, List<Relocation> relocations) {
        this.id = id;
        this.groupId = groupId;
        this.rawArtifactId = rawArtifactId;
        this.customArtifactID = customArtifactID;
        this.relocations = relocations;
    }

    public String id() {
        return id;
    }

    public String groupId() {
        return groupId;
    }

    public String rawArtifactId() {
        return rawArtifactId;
    }

    public String customArtifactID() {
        return customArtifactID;
    }

    public List<Relocation> relocations() {
        return relocations;
    }

    private static final String MAVEN_FORMAT = "%s/%s/%s/%s-%s.jar";

    public String mavenPath() {
        return String.format(MAVEN_FORMAT,
                rewriteEscaping(groupId).replace(".", "/"),
                rewriteEscaping(rawArtifactId),
                getVersion(),
                rewriteEscaping(rawArtifactId),
                getVersion()
        );
    }

    public String fileName(String classifier) {
        String name = customArtifactID.toLowerCase(Locale.ROOT).replace('_', '-');
        String extra = classifier == null || classifier.isEmpty()
                ? ""
                : "-" + classifier;
        return name + "-" + this.getVersion() + extra + ".jar";
    }

    public String getVersion() {
        return PluginProperties.getValue(id);
    }

    private static String rewriteEscaping(String s) {
        return s.replace("{}", ".");
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Dependency that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Dependency{" +
                "id='" + id + '\'' +
                ", groupId='" + groupId + '\'' +
                '}';
    }
}
