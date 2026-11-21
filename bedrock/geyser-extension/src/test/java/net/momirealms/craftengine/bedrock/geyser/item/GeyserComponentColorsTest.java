package net.momirealms.craftengine.bedrock.geyser.item;

import net.momirealms.craftengine.bedrock.BedrockTintSource;
import org.cloudburstmc.protocol.bedrock.data.definitions.SimpleItemDefinition;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.geysermc.geyser.inventory.item.Potion;
import org.geysermc.mcprotocollib.protocol.data.game.entity.Effect;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.*;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class GeyserComponentColorsTest {

    @Test
    void fireworkUsesOnlyPrimaryColors() {
        assertEquals(-1, GeyserComponentColors.firework(null));
        assertEquals(-1, GeyserComponentColors.firework(explosion()));
        assertEquals(0, GeyserComponentColors.firework(explosion(0)));
        assertEquals(0x7f007f, GeyserComponentColors.firework(explosion(0xff0000, 0x0000ff)));
    }

    @Test
    void potionUsesVisibleEffectsAndTheirLevels() {
        MobEffectInstance hidden = effect(Effect.INVISIBILITY, 20, false);
        MobEffectInstance speed = effect(Effect.SPEED, 1, true);
        assertEquals(-1, GeyserComponentColors.potion(null));
        assertEquals(-1, GeyserComponentColors.potion(new PotionContents(-1, -1, List.of(hidden), null)));
        assertEquals(-1, GeyserComponentColors.potion(Potion.WATER.toComponent()));
        assertEquals(0, GeyserComponentColors.potion(new PotionContents(Potion.POISON.ordinal(), 0, List.of(speed), null)));
        assertEquals(0x87a363, GeyserComponentColors.potion(Potion.POISON.toComponent()));
        assertEquals(0x33ebff, GeyserComponentColors.potion(new PotionContents(-1, -1, List.of(speed, hidden), null)));
        // 神龟基础药水的缓慢 IV 和抗性 III，与额外的迅捷 II 一起计算。
        assertEquals(0x7999ec, GeyserComponentColors.potion(new PotionContents(Potion.TURTLE_MASTER.ordinal(), -1, List.of(speed, hidden), null)));
    }

    @Test
    void inventoryAndHandCanUseDifferentSourcesWithIndependentFallbacks() {
        ItemData icon = ItemData.builder().definition(new SimpleItemDefinition("ce:probe", 12, false)).count(1).build();
        GeyserItemTints tints = new GeyserItemTints(Map.of("ce:probe", Map.of(
                "gui", BedrockTintSource.FIREWORK, "firstperson_righthand", BedrockTintSource.POTION)));
        DataComponents components = new DataComponents(new HashMap<>());
        components.put(DataComponentTypes.FIREWORK_EXPLOSION, explosion(0x123456));
        components.put(DataComponentTypes.POTION_CONTENTS, new PotionContents(-1, 0xabcdef, List.of(), null));
        ItemData dyedIcon = tints.render(components, icon);
        assertEquals(0x123456, dyedIcon.getTag().getInt("customColor"));
        assertEquals(0xabcdef, tints.color(components, icon, "firstperson_righthand"));
        components.remove(DataComponentTypes.FIREWORK_EXPLOSION);
        assertFalse(tints.render(components, dyedIcon).getTag().containsKey("customColor"));
    }

    private static Fireworks.FireworkExplosion explosion(int... colors) {
        return new Fireworks.FireworkExplosion(0, colors, new int[]{0x00ff00}, false, false);
    }

    private static MobEffectInstance effect(Effect effect, int amplifier, boolean particles) {
        return new MobEffectInstance(effect, new MobEffectDetails(amplifier, 20, false, particles, true, null));
    }
}
