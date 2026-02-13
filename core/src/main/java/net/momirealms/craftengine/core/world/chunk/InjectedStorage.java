package net.momirealms.craftengine.core.world.chunk;

import net.momirealms.craftengine.core.world.SectionPos;

public interface InjectedStorage {

    boolean isActive();

    void setActive(boolean active);

    CESection section();

    void setSection(CESection section);

    CEChunk chunk();

    void setChunk(CEChunk chunk);

    SectionPos pos();

    void setPos(SectionPos pos);

    interface Section extends InjectedStorage {
    }

    interface Palette extends InjectedStorage {

        Object delegated();
    }
}
