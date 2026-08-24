package net.momirealms.craftengine.bukkit.compatibility.papi;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.momirealms.craftengine.bukkit.api.BukkitAdaptor;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.core.attribute.Attribute;
import net.momirealms.craftengine.core.attribute.AttributeManager;
import net.momirealms.craftengine.core.entity.player.InteractionHand;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.context.PlayerOptionalContext;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.StringUtils;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public final class AttributeExpansion extends PlaceholderExpansion {
    private final CraftEngine plugin;

    public AttributeExpansion(CraftEngine plugin) {
        this.plugin = plugin;
    }

    @NotNull
    @Override
    public String getIdentifier() {
        return "ceattr";
    }

    @NotNull
    @Override
    public String getAuthor() {
        return "XiaoMoMi";
    }

    @NotNull
    @Override
    public String getVersion() {
        return "1.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    // %ceattr_<mode>_<属性id>%，mode ∈ base / value
    // value 支持 +weapon 后缀：额外合并主手武器的 weapon 作用域贡献
    // 例：%ceattr_value+weapon_example:crit_chance%
    @Override
    public @Nullable String onPlaceholderRequest(Player bukkitPlayer, @NotNull String params) {
        if (bukkitPlayer == null) return null;
        String[] split = StringUtils.split(params, '_', 2);
        if (split.length != 2) return null;
        String mode = split[0];
        boolean withWeapon = mode.endsWith("+weapon");
        if (withWeapon) {
            mode = mode.substring(0, mode.length() - "+weapon".length());
        }
        Optional<Attribute> optionalAttribute = this.plugin.attributeManager().getAttribute(Key.of(split[1]));
        if (optionalAttribute.isEmpty()) return null;
        Attribute attribute = optionalAttribute.get();
        BukkitServerPlayer player = BukkitAdaptor.adapt(bukkitPlayer);
        AttributeManager manager = this.plugin.attributeManager();
        return switch (mode) {
            case "base" -> withWeapon ? null : String.valueOf(attribute.baseValueSource().resolve(player));
            case "value" -> String.valueOf(effectiveValue(manager, player, attribute, withWeapon));
            default -> null;
        };
    }

    private double effectiveValue(AttributeManager manager, BukkitServerPlayer player, Attribute attribute, boolean withWeapon) {
        // 派生属性：变量经本方法递归取值，武器贡献在叶子属性层合并
        if (attribute.derived() != null) {
            return attribute.derive(a -> effectiveValue(manager, player, a, withWeapon));
        }
        double value = manager.getAttributeValue(player, attribute);
        if (withWeapon) {
            Item mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);
            value += manager.getWeaponAttributeValue(mainHand, attribute, PlayerOptionalContext.of(player));
        }
        return value;
    }
}
