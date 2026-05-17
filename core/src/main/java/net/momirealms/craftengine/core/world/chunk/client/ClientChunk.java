package net.momirealms.craftengine.core.world.chunk.client;

import net.momirealms.craftengine.core.world.SectionPos;
import net.momirealms.craftengine.core.world.WorldHeight;
import net.momirealms.craftengine.core.world.chunk.client.light.LightSection;
import net.momirealms.craftengine.core.world.chunk.client.occlusion.OccludingSection;
import org.jetbrains.annotations.Nullable;

public final class ClientChunk {
    @Nullable
    public final OccludingSection[] occludingSections;
    @Nullable
    public final LightSection[] lightSections;
    private final WorldHeight worldHeight;

    public ClientChunk(OccludingSection[] occludingSections, @Nullable LightSection[] lightSections, WorldHeight worldHeight) {
        this.occludingSections = occludingSections;
        this.lightSections = lightSections;
        this.worldHeight = worldHeight;
    }

    @Nullable
    public OccludingSection[] occludingSections() {
        return occludingSections;
    }

    @Nullable
    public LightSection[] lightSections() {
        return lightSections;
    }

    public boolean isOccluding(int x, int y, int z) {
        if (this.occludingSections == null) return false;
        int index = sectionIndex(SectionPos.blockToSectionCoord(y));
        if (index < 0 || index >= this.occludingSections.length) return false;
        OccludingSection section = this.occludingSections[index];
        if (section == null) return false;
        return section.isOccluding((y & 15) << 8 | (z & 15) << 4 | x & 15);
    }

    public void setOccluding(int x, int y, int z, boolean occluding) {
        if (this.occludingSections == null) return;
        int index = sectionIndex(SectionPos.blockToSectionCoord(y));
        if (index < 0 || index >= this.occludingSections.length) return;
        OccludingSection section = this.occludingSections[index];
        if (section == null) return;
        section.setOccluding((y & 15) << 8 | (z & 15) << 4 | x & 15, occluding);
    }

    /**
     * return 0 -> Solid;
     * return 1 -> Air;
     * return 2 -> Water;
     */
    public int lightBlockType(int x, int y, int z) {
        if (this.lightSections == null) return 0;
        int index = sectionIndex(SectionPos.blockToSectionCoord(y));
        if (index < 0 || index >= this.lightSections.length) return 0;
        LightSection section = this.lightSections[index];
        if (section == null) return 0;
        return section.blockType((y & 15) << 8 | (z & 15) << 4 | x & 15);
    }

    public void setLightBlockType(int x, int y, int z, int type) {
        if (this.lightSections == null || type < 0 || type > 2) return;
        int index = sectionIndex(SectionPos.blockToSectionCoord(y));
        if (index < 0 || index >= this.lightSections.length) return;
        LightSection section = this.lightSections[index];
        if (section == null) return;
        section.setBlockType((y & 15) << 8 | (z & 15) << 4 | x & 15, type);
    }

    public int sectionIndex(int sectionId) {
        return sectionId - this.worldHeight.getMinSection();
    }

    @Nullable
    public OccludingSection occludingSectionByIndex(int sectionIndex) {
        if (this.occludingSections == null) return null;
        return this.occludingSections[sectionIndex];
    }

    @Nullable
    public OccludingSection occludingSectionById(int sectionId) {
        if (this.occludingSections == null) return null;
        return this.occludingSections[sectionIndex(sectionId)];
    }

    @Nullable
    public LightSection lightSectionByIndex(int sectionIndex) {
        if (this.lightSections == null) return null;
        return this.lightSections[sectionIndex];
    }

    @Nullable
    public LightSection lightSectionById(int sectionId) {
        if (this.lightSections == null) return null;
        return this.lightSections[sectionIndex(sectionId)];
    }
}
