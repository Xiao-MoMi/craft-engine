package net.momirealms.craftengine.bedrock.geyser.entity.display;

import net.momirealms.craftengine.bedrock.geyser.player.GeyserPlayer;
import org.geysermc.geyser.api.entity.type.GeyserEntity;
import org.geysermc.geyser.entity.VanillaEntityType;
import org.geysermc.geyser.entity.spawn.EntitySpawnContext;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.mcprotocollib.protocol.data.game.entity.type.EntityType;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundAddEntityPacket;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Consumer;

public final class GeyserDisplayEntityTranslator extends PacketTranslator<ClientboundAddEntityPacket> {
    private static final int ENTITY_TYPE_COUNT = EntityType.values().length;

    private final PacketTranslator<ClientboundAddEntityPacket> delegate;
    private final SpawnHandler[] handlers = new SpawnHandler[ENTITY_TYPE_COUNT];

    private GeyserDisplayEntityTranslator(PacketTranslator<ClientboundAddEntityPacket> delegate,
                                          @Nullable VanillaEntityType<?> itemDefinition,
                                          @Nullable VanillaEntityType<?> blockDefinition,
                                          GeyserTextDisplayManager textDisplays) {
        this.delegate = delegate;
        this.registerModel(EntityType.ITEM_DISPLAY, itemDefinition);
        this.registerModel(EntityType.BLOCK_DISPLAY, blockDefinition);
        this.handlers[EntityType.TEXT_DISPLAY.ordinal()] = (session, packet) -> spawnText(session, packet, textDisplays);
    }

    @SuppressWarnings("unchecked")
    public static GeyserDisplayEntityTranslator install(GeyserModelDisplayManager itemDisplays, GeyserTextDisplayManager textDisplays) {
        VanillaEntityType<?> itemDefinition = itemDisplays.itemDefinition();
        VanillaEntityType<?> blockDefinition = itemDisplays.blockDefinition();
        PacketTranslator<ClientboundAddEntityPacket> delegate = (PacketTranslator<ClientboundAddEntityPacket>) Registries.JAVA_PACKET_TRANSLATORS.get(ClientboundAddEntityPacket.class);
        if (delegate instanceof GeyserDisplayEntityTranslator translator) {
            delegate = translator.delegate;
        }
        GeyserDisplayEntityTranslator translator = new GeyserDisplayEntityTranslator(delegate, itemDefinition, blockDefinition, textDisplays);
        Registries.JAVA_PACKET_TRANSLATORS.register(ClientboundAddEntityPacket.class, translator);
        return translator;
    }

    public void uninstall() {
        Registries.JAVA_PACKET_TRANSLATORS.get().replace(ClientboundAddEntityPacket.class, this, this.delegate);
        Arrays.fill(this.handlers, null);
    }

    @Override
    public void translate(GeyserSession session, ClientboundAddEntityPacket packet) {
        SpawnHandler handler = this.handlers[packet.getType().ordinal()];
        if (handler == null || !handler.spawn(session, packet)) {
            this.delegate.translate(session, packet);
        }
    }

    private void registerModel(EntityType type, @Nullable VanillaEntityType<?> definition) {
        if (definition != null) {
            this.handlers[type.ordinal()] = (session, packet) -> spawnModel(session, packet, definition);
        }
    }

    private static boolean spawnModel(GeyserSession session, ClientboundAddEntityPacket packet, VanillaEntityType<?> definition) {
        EntitySpawnContext context = spawnContext(session, packet, definition);
        if (context != null) {
            spawn(context, definition.factory().create(context));
        }
        return true;
    }

    private static boolean spawnText(GeyserSession session, ClientboundAddEntityPacket packet, GeyserTextDisplayManager textDisplays) {
        GeyserPlayer player = textDisplays.spawnPlayer(session);
        if (player == null) {
            return false;
        }
        EntitySpawnContext context = spawnContext(session, packet, textDisplays.definition());
        if (context != null) {
            spawn(context, textDisplays.create(context, player));
        }
        return true;
    }

    @Nullable
    private static EntitySpawnContext spawnContext(GeyserSession session, ClientboundAddEntityPacket packet, VanillaEntityType<?> definition) {
        EntitySpawnContext context = EntitySpawnContext.fromPacket(session, definition, packet);
        // 已由 CraftEngine 接管，事件取消时直接结束，避免回退后再次触发生成事件
        return context.callServerSpawnEvent() ? context : null;
    }

    private static void spawn(EntitySpawnContext context, Entity entity) {
        Collection<Consumer<GeyserEntity>> consumers = context.consumers();
        if (consumers != null) {
            for (Consumer<GeyserEntity> consumer : consumers) {
                consumer.accept(entity);
            }
        }
        context.session().getEntityCache().spawnEntity(entity);
    }

    @FunctionalInterface
    private interface SpawnHandler {

        boolean spawn(GeyserSession session, ClientboundAddEntityPacket packet);
    }
}
