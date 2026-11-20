package net.momirealms.craftengine.bedrock.geyser.network;

import net.momirealms.craftengine.bedrock.geyser.player.GeyserPlayer;
import net.momirealms.craftengine.bedrock.geyser.player.GeyserPlayerManager;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.mcprotocollib.network.packet.Packet;

import java.util.Arrays;

public final class GeyserJavaPackets {
    private static final InterceptingTranslator<?>[] EMPTY_TRANSLATORS = new InterceptingTranslator<?>[0];
    private final GeyserPlayerManager players;
    private InterceptingTranslator<?>[] translators = EMPTY_TRANSLATORS;

    public GeyserJavaPackets(GeyserPlayerManager players) {
        this.players = players;
    }

    @SuppressWarnings("unchecked")
    public <T extends Packet> void intercept(Class<T> type, Interceptor<T> interceptor) {
        PacketTranslator<T> original = (PacketTranslator<T>) Registries.JAVA_PACKET_TRANSLATORS.get(type);
        if (original == null) {
            throw new IllegalStateException("Missing Geyser translator for " + type.getName());
        }
        InterceptingTranslator<T> translator = new InterceptingTranslator<>(type, original, this.players, interceptor);
        InterceptingTranslator<?>[] updated = Arrays.copyOf(this.translators, this.translators.length + 1);
        updated[updated.length - 1] = translator;
        Registries.JAVA_PACKET_TRANSLATORS.register(type, translator);
        this.translators = updated;
    }

    public void uninstall() {
        for (int i = this.translators.length - 1; i >= 0; i--) {
            InterceptingTranslator<?> translator = this.translators[i];
            Registries.JAVA_PACKET_TRANSLATORS.get().replace(translator.type, translator, translator.original);
        }
        this.translators = EMPTY_TRANSLATORS;
    }

    @FunctionalInterface
    public interface Interceptor<T extends Packet> {
        void translate(GeyserPlayer player, T packet, PacketTranslator<T> original);
    }

    private static final class InterceptingTranslator<T extends Packet> extends PacketTranslator<T> {
        private final Class<T> type;
        private final PacketTranslator<T> original;
        private final GeyserPlayerManager players;
        private final Interceptor<T> interceptor;

        private InterceptingTranslator(Class<T> type, PacketTranslator<T> original, GeyserPlayerManager players, Interceptor<T> interceptor) {
            this.type = type;
            this.original = original;
            this.players = players;
            this.interceptor = interceptor;
        }

        @Override
        public void translate(GeyserSession session, T packet) {
            GeyserPlayer player = this.players.get(session);
            if (player == null) {
                this.original.translate(session, packet);
            } else {
                this.interceptor.translate(player, packet, this.original);
            }
        }

        @Override
        public boolean shouldExecuteInEventLoop() {
            return this.original.shouldExecuteInEventLoop();
        }
    }
}
