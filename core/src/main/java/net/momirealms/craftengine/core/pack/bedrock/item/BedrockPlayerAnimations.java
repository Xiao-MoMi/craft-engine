package net.momirealms.craftengine.core.pack.bedrock.item;

import net.momirealms.craftengine.core.util.Key;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class BedrockPlayerAnimations {
    private final Map<Key, ItemRegistrationPlan.Entry> spearsUsingJavaPose = new LinkedHashMap<>();
    private final Set<Key> shieldsUsingJavaPose = new HashSet<>();
    private final Set<Key> bows = new HashSet<>();
    private final Set<Key> bowsUsingJavaPose = new HashSet<>();
    private final Set<Key> itemsUsingJavaSwing = new HashSet<>();
    private final BedrockCrossbowPlayerAnimations crossbows = new BedrockCrossbowPlayerAnimations();

    void registerBow(Key id) {
        this.bows.add(id);
    }

    void registerBowUsingJavaPose(Key id) {
        this.bows.add(id);
        this.bowsUsingJavaPose.add(id);
    }

    void registerSpearUsingJavaPose(ItemRegistrationPlan.Entry entry) {
        this.spearsUsingJavaPose.put(entry.bedrockId(), entry);
    }

    void registerShieldUsingJavaPose(Key id) {
        this.shieldsUsingJavaPose.add(id);
    }

    void registerItemUsingJavaSwing(Key id) {
        this.itemsUsingJavaSwing.add(id);
    }

    void register2DCrossbow(BedrockCrossbowStates.State state, int chargeTicks) {
        this.crossbows.register2D(state, chargeTicks);
    }

    void register3DCrossbow(BedrockCrossbowStates.State state, int chargeTicks) {
        this.crossbows.register3D(state, chargeTicks);
    }

    void write(Path root) throws IOException {
        BedrockPlayerAnimationController controller = new BedrockPlayerAnimationController(root);
        this.crossbows.write(controller, root);
        BedrockSpearPlayerAnimations.write(this.spearsUsingJavaPose, controller, root);
        BedrockShieldPlayerAnimations.write(this.shieldsUsingJavaPose, controller, root);
        BedrockBowPlayerAnimations.write(this.bows, this.bowsUsingJavaPose, controller, root);
        BedrockSwingPlayerAnimations.write(this.itemsUsingJavaSwing, controller, root);
        controller.write();
    }
}
