package net.momirealms.craftengine.core.entity.culling;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.config.Config;

public final class CullableHolder {
    public volatile Cullable cullable;
    public volatile boolean isShown;
    public volatile boolean forceVisible;
    // 剔除后仍有实体保留在客户端（例如视距被设为 0 的展示实体），恢复时可直接复用。
    private volatile boolean retained;

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
            if (this.retained) {
                this.cullable.restore(player);
                this.retained = false;
            } else {
                this.cullable.show(player);
            }
        } else if (Config.entityCullingUpdateDisplayViewRange()) {
            this.retained = this.cullable.cull(player);
        } else {
            this.cullable.hide(player);
        }
    }

    public void remove(Player player) {
        if (this.isShown || this.retained) {
            this.cullable.hide(player);
        }
        this.retained = false;
    }

    /**
     * 替换追踪的渲染对象，保留当前的显示/剔除状态。
     * 已显示时，新旧元素的更新或替换由调用方处理；这里负责清理剔除期间保留的旧实体。
     */
    public void replace(Player player, Cullable cullable) {
        if (this.retained) {
            // 旧实体只是不可见，并未从客户端移除。新对象的实体 ID、数量或配置可能不同，
            // 不能直接接管这些旧实体；必须在丢弃旧对象引用前，用它发送真正的移除包。
            this.cullable.hide(player);
            // 新对象尚未生成。下次变为可见时应走 show 完整生成，而不是 restore 仅恢复视距。
            this.retained = false;
        }
        // 此处不主动显示新对象：若当前仍被剔除，继续等待下一次可见性更新。
        this.cullable = cullable;
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
