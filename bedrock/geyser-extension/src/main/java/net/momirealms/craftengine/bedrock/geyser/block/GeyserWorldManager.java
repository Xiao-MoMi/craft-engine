package net.momirealms.craftengine.bedrock.geyser.block;

import org.cloudburstmc.math.vector.Vector3i;
import org.geysermc.erosion.util.BlockPositionIterator;
import org.geysermc.geyser.level.WorldManager;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.GameMode;
import org.geysermc.mcprotocollib.protocol.data.game.setting.Difficulty;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.IntUnaryOperator;

public final class GeyserWorldManager extends WorldManager {
    private final WorldManager delegate;
    private final IntUnaryOperator visualState;

    public GeyserWorldManager(WorldManager delegate, IntUnaryOperator visualState) {
        this.delegate = delegate;
        this.visualState = visualState;
    }

    @Override
    public int getBlockAt(GeyserSession session, int x, int y, int z) {
        return this.visualState.applyAsInt(this.delegate.getBlockAt(session, x, y, z));
    }

    @Override
    public CompletableFuture<Integer> getBlockAtAsync(GeyserSession session, int x, int y, int z) {
        return this.delegate.getBlockAtAsync(session, x, y, z).thenApply(this.visualState::applyAsInt);
    }

    @Override
    public int[] getBlocksAt(GeyserSession session, BlockPositionIterator iterator) {
        int[] states = this.delegate.getBlocksAt(session, iterator);
        for (int i = 0; i < states.length; i++) {
            states[i] = this.visualState.applyAsInt(states[i]);
        }
        return states;
    }

    @Override
    public boolean hasOwnChunkCache() {
        return this.delegate.hasOwnChunkCache();
    }

    @Override
    public GameMode getDefaultGameMode(GeyserSession session) {
        return this.delegate.getDefaultGameMode(session);
    }

    @Override
    public void setDefaultGameMode(GeyserSession session, GameMode mode) {
        this.delegate.setDefaultGameMode(session, mode);
    }

    @Override
    public void setDifficulty(GeyserSession session, Difficulty difficulty) {
        this.delegate.setDifficulty(session, difficulty);
    }

    @Override
    public String[] getBiomeIdentifiers(boolean withTags) {
        return this.delegate.getBiomeIdentifiers(withTags);
    }

    @Override
    public void getDecoratedPotData(GeyserSession session, Vector3i position, Consumer<Map<String, String>> apply) {
        this.delegate.getDecoratedPotData(session, position, apply);
    }
}
