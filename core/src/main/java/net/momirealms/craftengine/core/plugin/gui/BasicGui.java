package net.momirealms.craftengine.core.plugin.gui;

public interface BasicGui extends Gui {

    static BasicGuiImpl.Builder builder() {
        return new BasicGuiImpl.Builder();
    }
}
