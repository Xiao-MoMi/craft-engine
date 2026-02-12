package net.momirealms.craftengine.core.world.chunk.client;

import net.momirealms.craftengine.core.world.SectionPos;
import net.momirealms.craftengine.core.world.WorldHeight;
import org.jetbrains.annotations.Nullable;

public final class ClientChunk {
    @Nullable
    public final ClientSection[] sections;
    private final WorldHeight worldHeight;

    public ClientChunk(ClientSection[] sections, WorldHeight worldHeight) {
        this.sections = sections;
        this.worldHeight = worldHeight;
    }

    @Nullable
    public ClientSection[] sections() {
        return sections;
    }

    public boolean isOccluding(int x, int y, int z) {
        if (this.sections == null) return false;
        int index = sectionIndex(SectionPos.blockToSectionCoord(y));
        if (index < 0 || index >= this.sections.length) return false;
        ClientSection section = this.sections[index];
        if (section == null) return false;
        return section.isOccluding((y & 15) << 8 | (z & 15) << 4 | x & 15);
    }

    public void setOccluding(int x, int y, int z, boolean occluding) {
        if (this.sections == null) return;
        int index = sectionIndex(SectionPos.blockToSectionCoord(y));
        if (index < 0 || index >= this.sections.length) return;
        ClientSection section = this.sections[index];
        if (section == null) return;
        section.setOccluding((y & 15) << 8 | (z & 15) << 4 | x & 15, occluding);
    }

    public int sectionIndex(int sectionId) {
        return sectionId - this.worldHeight.getMinSection();
    }

    @Nullable
    public ClientSection sectionByIndex(int sectionIndex) {
        if (this.sections == null) return null;
        return this.sections[sectionIndex];
    }

    @Nullable
    public ClientSection sectionById(int sectionId) {
        if (this.sections == null) return null;
        return this.sections[sectionIndex(sectionId)];
    }
}
