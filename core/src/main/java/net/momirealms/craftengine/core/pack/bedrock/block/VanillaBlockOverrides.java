package net.momirealms.craftengine.core.pack.bedrock.block;

import com.google.gson.JsonElement;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.momirealms.craftengine.core.block.BlockRegistryMirror;
import net.momirealms.craftengine.core.block.BlockStateWrapper;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourcePackUtils;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

final class VanillaBlockOverrides {
    private VanillaBlockOverrides() {
    }

    static List<WorldBlockPlan.VanillaOverride> plan(CraftEngine plugin,
                                                     JavaBlockModelResolver models,
                                                     List<WorldBlockSource> customBlocks,
                                                     Function<BlockStateWrapper, WorldBlockPlan.VanillaProperties> properties) {
        Int2ObjectOpenHashMap<WorldBlockSource> occupied = new Int2ObjectOpenHashMap<>();
        for (WorldBlockSource custom : customBlocks) {
            if (custom.visual() != null) {
                occupied.putIfAbsent(custom.visual().registryId(), custom);
            }
        }

        int vanillaCount = plugin.blockManager().vanillaBlockStateCount();
        BitSet seenClientStates = new BitSet(vanillaCount);
        Map<Key, Map<String, JsonElement>> generatedOverrides = plugin.blockManager().blockOverrides();
        List<WorldBlockPlan.VanillaOverride> overrides = new ArrayList<>();
        for (int id = 0; id < vanillaCount; id++) {
            BlockStateWrapper state = BlockRegistryMirror.byId(id);
            Key block = state.ownerId();
            if (!block.isMinecraftNamespace()) {
                continue;
            }
            try {
                if (!models.mayHaveVanillaOverride(block)) {
                    continue;
                }
                int clientId = plugin.networkManager().remapBlockState(id, false);
                WorldBlockSource owner = occupied.get(id);
                if (owner == null) {
                    owner = occupied.get(clientId);
                }
                if (owner != null) {
                    String selector = generatedSelector(state, generatedOverrides.get(block));
                    if (models.selectVanillaOverride(state, selector) != null) {
                        plugin.logger().warn("Skipped Bedrock vanilla model for " + state.getAsString() + ": display state "
                                + owner.visual().getAsString() + " is used by " + owner.id());
                    }
                    continue;
                }
                // 多个原版状态可能发给客户端同一个状态，例如不同年龄的甘蔗
                if (seenClientStates.get(clientId)) {
                    continue;
                }
                BlockStateWrapper clientState = BlockRegistryMirror.byId(clientId);
                if (block.equals(clientState.ownerId())) {
                    seenClientStates.set(clientId);
                    JavaBlockModelResolver.VanillaModelSelection model = models.selectVanillaOverride(clientState, null);
                    if (model != null) {
                        overrides.add(new WorldBlockPlan.VanillaOverride(clientState, properties.apply(clientState), model));
                    }
                }
            } catch (IOException | UnsupportedOperationException exception) {
                plugin.logger().warn("Skipped Bedrock vanilla model for " + state.getAsString() + ": " + exception.getLocalizedMessage());
            }
        }
        return overrides;
    }

    private static @Nullable String generatedSelector(BlockStateWrapper state, @Nullable Map<String, JsonElement> overrides) {
        if (overrides == null) {
            return null;
        }
        String selector = ResourcePackUtils.blockStateSelector(state.getAsString());
        return overrides.containsKey(selector) ? selector : null;
    }
}
