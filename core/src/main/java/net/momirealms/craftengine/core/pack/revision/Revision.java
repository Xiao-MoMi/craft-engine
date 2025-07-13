package net.momirealms.craftengine.core.pack.revision;

import net.momirealms.craftengine.core.util.MinecraftVersion;
import net.momirealms.craftengine.core.util.MinecraftVersions;

import java.util.Objects;

public interface Revision {

    int minPackVersion();

    int maxPackVersion();

    String versionString();

    boolean matches(MinecraftVersion min, MinecraftVersion max);

    MinecraftVersion minVersion();

    MinecraftVersion maxVersion();

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
            return MinecraftVersions.FUTURE;
        }

        @Override
        public MinecraftVersion minVersion() {
            return this.minVersion;
        }

        @Override
        public String versionString() {
            if (this.versionString == null) {
                this.versionString = this.minVersion.version().replace(".", "_");
            }
            return this.versionString;
        }

        @Override
        public boolean matches(MinecraftVersion min, MinecraftVersion max) {
            return this.minVersion.isAtOrBelow(max);
        }

        @Override
        public int maxPackVersion() {
            return MinecraftVersions.FUTURE.packFormat();
        }

        @Override
        public int minPackVersion() {
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
        public int minPackVersion() {
            return this.minVersion.packFormat();
        }

        @Override
        public int maxPackVersion() {
            return this.maxVersion.packFormat();
        }

        @Override
        public String versionString() {
            if (this.versionString == null) {
                this.versionString = this.minVersion.version().replace(".", "_") + "-" + this.maxVersion.version().replace(".", "_");
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
    }
}
