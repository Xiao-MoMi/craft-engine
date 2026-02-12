package net.momirealms.craftengine.core.entity.culling;

import net.momirealms.craftengine.core.entity.player.Player;

public final class CullableHolder {
    public Cullable cullable;
    public boolean isShown;

    public CullableHolder(Cullable cullable) {
        this.cullable = cullable;
        this.isShown = false;
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
}
