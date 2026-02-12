package net.momirealms.craftengine.core.pack.atlas;

public class TextureStatus {
    private boolean inBlockAtlas;
    private boolean shouldBeInBlockAtlas;
    private boolean inItemAtlas;

    public TextureStatus() {
        this.inBlockAtlas = false;
        this.shouldBeInBlockAtlas = false;
        this.inItemAtlas = false;
    }

    public TextureStatus(boolean inBlockAtlas, boolean shouldBeInBlockAtlas, boolean inItemAtlas) {
        this.inBlockAtlas = inBlockAtlas;
        this.shouldBeInBlockAtlas = shouldBeInBlockAtlas;
        this.inItemAtlas = inItemAtlas;
    }

    public boolean inBlockAtlas() {
        return inBlockAtlas;
    }

    public void setInBlockAtlas(boolean inBlockAtlas) {
        this.inBlockAtlas = inBlockAtlas;
    }

    public boolean shouldBeInBlockAtlas() {
        return shouldBeInBlockAtlas;
    }

    public void setShouldBeInBlockAtlas(boolean shouldBeInBlockAtlas) {
        this.shouldBeInBlockAtlas = shouldBeInBlockAtlas;
    }

    public boolean inItemAtlas() {
        return inItemAtlas;
    }

    public void setInItemAtlas(boolean inItemAtlas) {
        this.inItemAtlas = inItemAtlas;
    }
}
