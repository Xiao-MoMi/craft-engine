package net.momirealms.craftengine.core.pack.revision;

import net.momirealms.craftengine.core.pack.mcmeta.Overlay;
import net.momirealms.craftengine.core.pack.mcmeta.PackVersion;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.util.MinecraftVersion;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public interface Revision extends Comparable<Revision> {

    PackVersion minPackVersion();

    PackVersion maxPackVersion();

    String versionString();

    boolean matches(MinecraftVersion min, MinecraftVersion max);

    MinecraftVersion minVersion();

    MinecraftVersion maxVersion();

    default Overlay createOverlay() {
        return new Overlay(minPackVersion(), maxPackVersion(), Config.createOverlayFolderName(versionString()));
    }

    @Override
    default int compareTo(@NotNull Revision o) {
        int minCompare = this.minVersion().compareTo(o.minVersion());
        if (minCompare != 0) {
            return minCompare;
        }
        return this.maxVersion().compareTo(o.maxVersion());
    }

    static Revision since(MinecraftVersion minecraftVersion) {
        return new Since(minecraftVersion);
    }

    static Revision fromTo(MinecraftVersion from, MinecraftVersion to) {
        return new FromTo(from, to);
    }

    class Since implements Revision {
        private final MinecraftVersion minVersion;
        private String versionString;

        public Since(MinecraftVersion minVersion) {
            this.minVersion = minVersion;
        }

        @Override
        public MinecraftVersion maxVersion() {
            return MinecraftVersion.FUTURE;
        }

        @Override
        public MinecraftVersion minVersion() {
            return this.minVersion;
        }

        @Override
        public String versionString() {
            if (this.versionString == null) {
                this.versionString = this.minVersion.packFormat().asString();
            }
            return this.versionString;
        }

        @Override
        public boolean matches(MinecraftVersion min, MinecraftVersion max) {
            return this.minVersion.isAtOrBelow(max) && min.isBelow(this.minVersion);
        }

        @Override
        public PackVersion maxPackVersion() {
            return MinecraftVersion.FUTURE.packFormat();
        }

        @Override
        public PackVersion minPackVersion() {
            return this.minVersion.packFormat();
        }

        @Override
        public final boolean equals(Object object) {
            if (!(object instanceof Since since)) return false;
            return this.minVersion.equals(since.minVersion);
        }

        @Override
        public int hashCode() {
            return this.minVersion.hashCode();
        }

        @Override
        public String toString() {
            return "Since{" + "minVersion=" + minVersion + '}';
        }
    }

    class FromTo implements Revision {
        private final MinecraftVersion minVersion;
        private final MinecraftVersion maxVersion;
        private String versionString;

        public FromTo(MinecraftVersion minVersion, MinecraftVersion maxVersion) {
            this.minVersion = minVersion;
            this.maxVersion = maxVersion;
        }

        @Override
        public MinecraftVersion maxVersion() {
            return this.maxVersion;
        }

        @Override
        public MinecraftVersion minVersion() {
            return this.minVersion;
        }

        @Override
        public boolean matches(MinecraftVersion min, MinecraftVersion max) {
            return !min.isAbove(this.maxVersion) || !max.isBelow(this.minVersion);
        }

        @Override
        public PackVersion minPackVersion() {
            return this.minVersion.packFormat();
        }

        @Override
        public PackVersion maxPackVersion() {
            return this.maxVersion.packFormat();
        }

        @Override
        public String versionString() {
            if (this.versionString == null) {
                this.versionString = this.minVersion.packFormat().asString() + "-" + this.maxVersion.packFormat().asString();
            }
            return this.versionString;
        }

        @Override
        public final boolean equals(Object object) {
            if (!(object instanceof FromTo fromTo)) return false;
            return Objects.equals(minVersion, fromTo.minVersion) && Objects.equals(maxVersion, fromTo.maxVersion);
        }

        @Override
        public int hashCode() {
            int result = Objects.hashCode(minVersion);
            result = 31 * result + Objects.hashCode(maxVersion);
            return result;
        }

        @Override
        public String toString() {
            return "FromTo{" +
                    "minVersion=" + minVersion +
                    ", maxVersion=" + maxVersion +
                    '}';
        }
    }
}
