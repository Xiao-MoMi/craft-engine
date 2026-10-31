package net.momirealms.craftengine.bedrock.geyser.block;

import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.data.LevelEvent;
import org.cloudburstmc.protocol.bedrock.packet.LevelEventPacket;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.util.BlockUtils;

import java.util.HashMap;
import java.util.Map;

public final class GeyserMiningProgress {
    private final GeyserSession session;
    private final Map<Vector3i, MiningState> states = new HashMap<>();

    public GeyserMiningProgress(GeyserSession session) {
        this.session = session;
    }

    public void update(int breakerId, Vector3i position, float progress, float speed) {
        MiningState state = this.states.get(position);
        if (progress < 0 || progress >= 1) {
            if (state != null && state.breakerId == breakerId) {
                this.states.remove(position);
                BlockUtils.sendBedrockStopBlockBreak(this.session, position.toFloat());
            }
            return;
        }
        // Bedrock expects progress per tick scaled to 65535, not the duration between Java's ten stages.
        int rate = speed > 0 ? Math.max(1, Math.min(65535, Math.round(speed * 65535))) : 0;
        if (state == null || state.breakerId != breakerId || progress < state.progress) {
            if (state != null) {
                BlockUtils.sendBedrockStopBlockBreak(this.session, position.toFloat());
            }
            this.states.put(position, new MiningState(breakerId, progress, rate));
            this.send(position, LevelEvent.BLOCK_START_BREAK, rate);
        } else {
            state.progress = progress;
            if (state.rate != rate) {
                state.rate = rate;
                this.send(position, LevelEvent.BLOCK_UPDATE_BREAK, rate);
            }
        }
    }

    public void forget(int breakerId, Vector3i position) {
        MiningState state = this.states.get(position);
        if (state != null && state.breakerId == breakerId) {
            this.states.remove(position);
        }
    }

    public void reset() {
        for (Vector3i position : this.states.keySet()) {
            BlockUtils.sendBedrockStopBlockBreak(this.session, position.toFloat());
        }
        this.states.clear();
    }

    private void send(Vector3i position, LevelEvent event, int rate) {
        LevelEventPacket packet = new LevelEventPacket();
        packet.setType(event);
        packet.setPosition(position.toFloat());
        packet.setData(rate);
        this.session.sendUpstreamPacket(packet);
    }

    private static final class MiningState {
        private final int breakerId;
        private float progress;
        private int rate;

        private MiningState(int breakerId, float progress, int rate) {
            this.breakerId = breakerId;
            this.progress = progress;
            this.rate = rate;
        }
    }
}
