package net.momirealms.craftengine.core.font;

import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.LazyReference;

public final class ReferenceImage implements Image {
    private final LazyReference<Image> ref;
    private final int row;
    private final int col;

    public ReferenceImage(LazyReference<Image> ref, int row, int col) {
        this.ref = ref;
        this.row = row;
        this.col = col;
    }

    public Image image() {
        return this.ref.get();
    }

    public int row() {
        return this.row;
    }

    public int col() {
        return this.col;
    }

    @Override
    public Key id() {
        return this.ref.get().id();
    }

    @Override
    public Component componentAt(int row, int column) {
        return this.ref.get().componentAt(this.row, this.col);
    }

    @Override
    public String miniMessageAt(int row, int col) {
        return this.ref.get().miniMessageAt(this.row, this.col);
    }

    @Override
    public String mineDownAt(int row, int col) {
        return this.ref.get().mineDownAt(this.row, this.col);
    }

    @Override
    public int codepointAt(int row, int column) {
        return this.ref.get().codepointAt(this.row, this.col);
    }
}
