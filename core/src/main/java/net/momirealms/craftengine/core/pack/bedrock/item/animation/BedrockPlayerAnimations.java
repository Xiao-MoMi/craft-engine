package net.momirealms.craftengine.core.pack.bedrock.item.animation;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.momirealms.craftengine.core.util.Key;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

public class BedrockPlayerAnimations {
    private final SortedMap<Key, List<String>> spearPoseScriptsByItem = new TreeMap<>(Comparator.comparing(Key::asString));
    private final Set<Key> shieldsUsingJavaPose = new ObjectOpenHashSet<>();
    private final Set<Key> bows = new ObjectOpenHashSet<>();
    private final Set<Key> bowsUsingJavaPose = new ObjectOpenHashSet<>();
    private final Set<Key> itemsUsingJavaSwing = new ObjectOpenHashSet<>();
    private final BedrockCrossbowPlayerAnimations crossbows = new BedrockCrossbowPlayerAnimations();

    static String sortedItemArguments(Set<Key> items) {
        String[] names = new String[items.size()];
        int index = 0;
        for (Key item : items) {
            names[index++] = item.asString();
        }
        Arrays.sort(names);
        StringBuilder arguments = new StringBuilder();
        for (String name : names) {
            arguments.append(", '").append(name).append('\'');
        }
        return arguments.toString();
    }

    public void registerBow(Key id) {
        this.bows.add(id);
    }

    public void registerBowUsingJavaPose(Key id) {
        this.bows.add(id);
        this.bowsUsingJavaPose.add(id);
    }

    public void registerSpearUsingJavaPose(Key id, List<String> poseScripts) {
        this.spearPoseScriptsByItem.put(id, List.copyOf(poseScripts));
    }

    public void registerShieldUsingJavaPose(Key id) {
        this.shieldsUsingJavaPose.add(id);
    }

    public void registerItemUsingJavaSwing(Key id) {
        this.itemsUsingJavaSwing.add(id);
    }

    public void register2DCrossbow(Key id, boolean charged, int chargeTicks) {
        this.crossbows.register2D(id, charged, chargeTicks);
    }

    public void register3DCrossbow(Key id, boolean charged, int chargeTicks) {
        this.crossbows.register3D(id, charged, chargeTicks);
    }

    public void write(Path root) throws IOException {
        BedrockPlayerAnimationController controller = new BedrockPlayerAnimationController(root);
        this.crossbows.write(controller, root);
        BedrockSpearPlayerAnimations.write(this.spearPoseScriptsByItem, controller, root);
        BedrockShieldPlayerAnimations.write(this.shieldsUsingJavaPose, controller, root);
        BedrockBowPlayerAnimations.write(this.bows, this.bowsUsingJavaPose, controller, root);
        BedrockSwingPlayerAnimations.write(this.itemsUsingJavaSwing, controller, root);
        controller.write();
    }
}
