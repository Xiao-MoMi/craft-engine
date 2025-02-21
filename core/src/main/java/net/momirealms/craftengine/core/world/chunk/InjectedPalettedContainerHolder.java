package net.momirealms.craftengine.core.world.chunk;

import net.momirealms.craftengine.core.world.CEWorld;
import net.momirealms.craftengine.core.world.SectionPos;

public interface InjectedPalettedContainerHolder {

    Object target();

    CESection ceSection();

    CEWorld ceWorld();

    SectionPos cePos();
}
