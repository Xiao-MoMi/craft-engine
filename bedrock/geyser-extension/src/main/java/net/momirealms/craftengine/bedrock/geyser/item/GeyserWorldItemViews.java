package net.momirealms.craftengine.bedrock.geyser.item;

import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.level.block.type.BlockState;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.level.block.entity.BlockEntityTranslator;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.mcprotocollib.protocol.data.game.entity.type.EntityType;
import org.geysermc.mcprotocollib.protocol.data.game.level.block.BlockEntityType;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundSetEntityDataPacket;

public final class GeyserWorldItemViews extends PacketTranslator<ClientboundSetEntityDataPacket> {
    private static final ThreadLocal<String> VIEW = new ThreadLocal<>();
    private PacketTranslator<ClientboundSetEntityDataPacket> metadata;
    private BlockEntityTranslator shelf;
    private BlockEntityTranslator shelfWrapper;

    static boolean matches(String view) {
        return view.equals(VIEW.get());
    }

    // Scope the visual to Geyser's normal Java stack translation. No inventory or transaction packet gets a visual-only ID.
    private static void inView(String view, Runnable translate) {
        String previous = VIEW.get();
        VIEW.set(view);
        try {
            translate.run();
        } finally {
            if (previous == null) {
                VIEW.remove();
            } else {
                VIEW.set(previous);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public void install() {
        if (this.metadata != null) {
            return;
        }
        this.metadata = (PacketTranslator<ClientboundSetEntityDataPacket>) Registries.JAVA_PACKET_TRANSLATORS.get(ClientboundSetEntityDataPacket.class);
        Registries.JAVA_PACKET_TRANSLATORS.register(ClientboundSetEntityDataPacket.class, this);
        this.shelf = Registries.BLOCK_ENTITIES.get(BlockEntityType.SHELF);
        this.shelfWrapper = new BlockEntityTranslator() {
            @Override
            public void translateTag(GeyserSession session, NbtMapBuilder target, NbtMap source, BlockState state) {
                inView("ON_SHELF", () -> GeyserWorldItemViews.this.shelf.translateTag(session, target, source, state));
            }
        };
        Registries.BLOCK_ENTITIES.register(BlockEntityType.SHELF, this.shelfWrapper);
    }

    @Override
    public void translate(GeyserSession session, ClientboundSetEntityDataPacket packet) {
        Entity entity = session.getEntityCache().getEntityByJavaId(packet.getEntityId());
        EntityType type = entity == null ? null : entity.getEntityType();
        if (type == EntityType.ITEM) {
            inView("GROUND", () -> this.metadata.translate(session, packet));
        } else if (type == EntityType.ITEM_FRAME || type == EntityType.GLOW_ITEM_FRAME) {
            inView("FIXED", () -> this.metadata.translate(session, packet));
        } else {
            this.metadata.translate(session, packet);
        }
    }

    public void uninstall() {
        if (this.metadata != null) {
            Registries.JAVA_PACKET_TRANSLATORS.get().replace(ClientboundSetEntityDataPacket.class, this, this.metadata);
            Registries.BLOCK_ENTITIES.get().replace(BlockEntityType.SHELF, this.shelfWrapper, this.shelf);
            this.metadata = null;
        }
    }
}
