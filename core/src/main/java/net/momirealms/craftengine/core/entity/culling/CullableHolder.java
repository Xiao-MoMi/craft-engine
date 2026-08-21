package net.momirealms.craftengine.core.entity.culling;

import net.momirealms.craftengine.core.entity.player.Player;

public final class CullableHolder {
    public volatile Cullable cullable;
    public volatile boolean isShown;
    public volatile boolean forceVisible;

    public CullableHolder(Cullable cullable) {
        this(cullable, false);
    }

    public CullableHolder(Cullable cullable, boolean forceVisible) {
        this.cullable = cullable;
        this.isShown = false;
        this.forceVisible = forceVisible;
    }

    public void setShown(Player player, boolean shown) {
        if (this.isShown == shown) return;
        this.isShown = shown;
        if (shown) {
            this.cullable.show(player);
        } else {
            this.cullable.hide(player);
        }
    }

    public boolean forceVisible() {
        return this.forceVisible;
    }

    public void setForceVisible(Player player, boolean forceVisible) {
        this.forceVisible = forceVisible;
        if (forceVisible) {
            this.setShown(player, true);
        }
    }
}
