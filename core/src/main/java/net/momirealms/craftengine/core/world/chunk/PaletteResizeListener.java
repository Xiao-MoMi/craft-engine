package net.momirealms.craftengine.core.world.chunk;

public interface PaletteResizeListener<T> {

    int onResize(int newBits, T object);
}
