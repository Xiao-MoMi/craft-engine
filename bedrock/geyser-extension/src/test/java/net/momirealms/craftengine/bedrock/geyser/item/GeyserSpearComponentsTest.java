package net.momirealms.craftengine.bedrock.geyser.item;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GeyserSpearComponentsTest {
    private static final NbtMap VANILLA = NbtMap.builder().putCompound("components", NbtMap.builder()
            .putCompound("minecraft:kinetic_weapon", NbtMap.builder().putCompound("minecraft:kinetic_weapon", NbtMap.EMPTY).build())
            .putCompound("minecraft:use_modifiers", NbtMap.builder().putString("start_using", "always")
                    .putFloat("use_duration", 72000).putFloat("movement_modifier", 1).build()).build()).build();

    @Test
    void preservesRegistrationAndRestoresAllThreeDurations() {
        NbtMap components = NbtMap.builder().putList("item_tags", NbtType.STRING, "geyser:is_custom")
                .putCompound("minecraft:icon", NbtMap.builder().putString("texture", "custom").build())
                .putCompound("item_properties", NbtMap.builder().putInt("max_stack_size", 1).build())
                .putCompound("minecraft:use_modifiers", NbtMap.builder().putFloat("movement_modifier", 0.4f).build())
                .putCompound("minecraft:kinetic_weapon", NbtMap.builder().putFloat("damage_multiplier", 1).build()).build();
        NbtMap original = NbtMap.builder().putInt("id", 1234).putCompound("components", components).build();
        JsonObject behavior = JsonParser.parseString("""
                {"use":{"animation":"SPEAR","maximumDuration":{"value":72000}},"kinetic":{"delayTicks":14,"forwardMovement":0.38,
                  "dismount":{"durationTicks":70,"minimumSpeed":13,"minimumRelativeSpeed":0},
                  "knockback":{"durationTicks":170,"minimumSpeed":5.1,"minimumRelativeSpeed":0},
                  "damage":{"durationTicks":275,"minimumSpeed":0,"minimumRelativeSpeed":4.6}}}
                """).getAsJsonObject();
        NbtMap patched = GeyserSpearComponents.patch(original, VANILLA, behavior);
        NbtMap changed = patched.getCompound("components");
        NbtMap kinetic = changed.getCompound("minecraft:kinetic_weapon").getCompound("minecraft:kinetic_weapon");
        assertEquals(1234, patched.getInt("id"));
        assertEquals(components.getCompound("minecraft:icon"), changed.getCompound("minecraft:icon"));
        assertEquals(List.of("geyser:is_custom", "minecraft:is_spear"), changed.getList("item_tags", NbtType.STRING));
        assertEquals((short) 14, kinetic.getShort("delay"));
        assertEquals((short) 70, kinetic.getCompound("dismount_conditions").getShort("max_duration"));
        assertEquals((short) 170, kinetic.getCompound("knockback_conditions").getShort("max_duration"));
        assertEquals((short) 275, kinetic.getCompound("damage_conditions").getShort("max_duration"));
        assertEquals(4.6f, kinetic.getCompound("damage_conditions").getFloat("min_relative_speed"));
        assertEquals(1f, kinetic.getFloat("damage_multiplier"));
        assertEquals("always", changed.getCompound("minecraft:use_modifiers").getString("start_using"));
        assertEquals(3600f, changed.getCompound("minecraft:use_modifiers").getFloat("use_duration"));
        assertEquals(0.4f, changed.getCompound("minecraft:use_modifiers").getFloat("movement_modifier"));
        assertEquals(72000, changed.getCompound("item_properties").getInt("use_duration"));
        assertEquals(1, changed.getCompound("item_properties").getInt("max_stack_size"));
        assertEquals(List.of("geyser:is_custom"), original.getCompound("components").getList("item_tags", NbtType.STRING));
        assertEquals(patched, GeyserSpearComponents.patch(patched, VANILLA, behavior));
    }

    @Test
    void doesNotRestoreRemovedKineticComponent() {
        NbtMap original = NbtMap.builder().putCompound("components", NbtMap.EMPTY).build();
        NbtMap patched = GeyserSpearComponents.patch(original, VANILLA, new JsonObject());
        assertFalse(patched.getCompound("components").containsKey("minecraft:kinetic_weapon"));
        assertFalse(patched.getCompound("components").containsKey("minecraft:use_modifiers"));
    }
}
