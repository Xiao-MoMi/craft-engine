package net.momirealms.craftengine.bedrock.geyser.item;

import org.geysermc.geyser.api.entity.property.BatchPropertyUpdater;
import org.geysermc.geyser.api.entity.property.type.GeyserIntEntityProperty;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineEntityPropertiesEvent;
import org.geysermc.geyser.api.util.Identifier;
import org.geysermc.geyser.inventory.GeyserItemStack;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentTypes;

public record GeyserDyeProperties(GeyserIntEntityProperty red, GeyserIntEntityProperty greenBlue) {

    public static GeyserDyeProperties register(GeyserDefineEntityPropertiesEvent event, Identifier entity, String name) {
        // 将 RGB 拆成 8 位与 16 位，两项都处于基岩实体整型属性的精确范围内。红色通道为 -1 表示未染色。
        return new GeyserDyeProperties(
                event.registerIntegerProperty(entity, Identifier.of(name + "_r"), -1, 255, -1),
                event.registerIntegerProperty(entity, Identifier.of(name + "_gb"), 0, 65535, 0)
        );
    }

    public static int color(GeyserItemStack item) {
        Integer color = item == null || item.isEmpty() ? null : item.getComponent(DataComponentTypes.DYED_COLOR);
        return color == null ? -1 : color & 0xffffff;
    }

    public void write(BatchPropertyUpdater batch, int rgb) {
        batch.update(this.red, rgb < 0 ? -1 : rgb >>> 16);
        batch.update(this.greenBlue, rgb < 0 ? 0 : rgb & 65535);
    }
}
