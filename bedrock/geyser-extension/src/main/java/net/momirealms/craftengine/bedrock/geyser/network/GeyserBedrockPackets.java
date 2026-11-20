package net.momirealms.craftengine.bedrock.geyser.network;

import net.momirealms.craftengine.bedrock.geyser.block.GeyserBlockPlacementHandler;
import net.momirealms.craftengine.bedrock.geyser.entity.model.GeyserNativeModels;
import net.momirealms.craftengine.bedrock.geyser.item.*;
import net.momirealms.craftengine.bedrock.geyser.item.component.GeyserItemComponentPatcher;
import net.momirealms.craftengine.bedrock.geyser.player.GeyserPlayer;
import org.cloudburstmc.protocol.bedrock.netty.BedrockPacketWrapper;
import org.cloudburstmc.protocol.bedrock.packet.*;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

@SuppressWarnings("all")
public final class GeyserBedrockPackets {
    private static final Table INCOMING = new Table();
    private static final Table OUTGOING = new Table();

    static {
        INCOMING.on(MobEquipmentPacket.class,
                bind(GeyserPlayer::selectedItems, GeyserSelectedItemHandler::restoreEquipment));
        INCOMING.on(InventoryTransactionPacket.class,
                bind(GeyserPlayer::selectedItems, GeyserSelectedItemHandler::restoreTransaction),
                bind(GeyserPlayer::activeUse, (use, event) -> {
                    if (use.suppress(event.packet())) {
                        event.setCancelled(true);
                    }
                }),
                bind(GeyserPlayer::placement, GeyserBlockPlacementHandler::onTransaction),
                bind(GeyserPlayer::nativeModels, GeyserNativeModels.Connection::onTransaction));
        INCOMING.on(PlayerAuthInputPacket.class,
                bind(GeyserPlayer::selectedItems, GeyserSelectedItemHandler::restorePlayerInput),
                bind(Function.identity(), (player, event) -> player.cameraDirection(event.packet().getCameraOrientation())));

        OUTGOING.on(InventoryContentPacket.class,
                bind(GeyserPlayer::elytra, (elytra, event) -> elytra.onInventoryContent(event.packet())),
                bind(GeyserPlayer::selectedItems, GeyserSelectedItemHandler::onInventoryContent),
                bind(GeyserPlayer::heldDye, (dye, event) -> dye.onInventoryContent(event.packet())));
        OUTGOING.on(InventorySlotPacket.class,
                bind(GeyserPlayer::elytra, (elytra, event) -> elytra.onInventorySlot(event.packet())),
                bind(GeyserPlayer::selectedItems, GeyserSelectedItemHandler::onInventorySlot),
                bind(GeyserPlayer::heldDye, (dye, event) -> dye.onInventorySlot(event.packet())));
        OUTGOING.on(MobArmorEquipmentPacket.class,
                bind(GeyserPlayer::elytra, (elytra, event) -> elytra.queue(event.packet().getRuntimeEntityId(), false)),
                bind(GeyserPlayer::selectedItems, GeyserSelectedItemHandler::onArmorEquipment),
                bind(GeyserPlayer::heldDye, (dye, event) -> dye.queueHead(event.packet().getRuntimeEntityId())));
        OUTGOING.on(MobEquipmentPacket.class,
                bind(GeyserPlayer::selectedItems, GeyserSelectedItemHandler::onEquipment),
                bind(GeyserPlayer::heldDye, GeyserHeldDye::onEquipment));
        OUTGOING.on(AddPlayerPacket.class,
                bind(GeyserPlayer::elytra, (elytra, event) -> elytra.queue(event.packet().getRuntimeEntityId(), true)),
                bind(GeyserPlayer::selectedItems, GeyserSelectedItemHandler::onAddPlayer),
                bind(GeyserPlayer::heldDye, GeyserHeldDye::onAddPlayer));
        OUTGOING.on(ItemStackResponsePacket.class,
                bind(GeyserPlayer::selectedItems, GeyserSelectedItemHandler::onStackResponse),
                bind(GeyserPlayer::heldDye, (dye, event) -> dye.queueHands()));
        OUTGOING.on(PlayerHotbarPacket.class,
                bind(GeyserPlayer::heldDye, (dye, event) -> dye.onHotbar(event.packet())));
        OUTGOING.on(RespawnPacket.class,
                bind(GeyserPlayer::elytra, (elytra, event) -> elytra.onRespawn(event.packet())),
                bind(GeyserPlayer::activeUse, (use, event) -> use.reset()),
                bind(GeyserPlayer::heldDye, (dye, event) -> dye.onRespawn(event.packet())));
        OUTGOING.on(RemoveEntityPacket.class,
                bind(GeyserPlayer::elytra, (elytra, event) -> elytra.remove(event.packet().getUniqueEntityId())));
        OUTGOING.on(ItemComponentPacket.class,
                bind(GeyserPlayer::componentPatcher, GeyserItemComponentPatcher::patch));
        OUTGOING.on(CraftingDataPacket.class,
                bind(GeyserPlayer::componentPatcher, GeyserItemComponentPatcher::patchRecipes));
        OUTGOING.on(AddEntityPacket.class,
                bind(GeyserPlayer::nativeModels, GeyserNativeModels.Connection::onAddEntity));
        OUTGOING.on(ChangeDimensionPacket.class,
                bind(GeyserPlayer::nativeModels, (models, event) -> models.clear()),
                bind(GeyserPlayer::displayOwners, (owners, event) -> owners.clear()),
                bind(GeyserPlayer::activeUse, (use, event) -> use.reset()));
    }

    private GeyserBedrockPackets() {
    }

    public static boolean receive(GeyserPlayer player, BedrockPacketWrapper wrapper) {
        return INCOMING.dispatch(player, wrapper);
    }

    public static boolean send(GeyserPlayer player, BedrockPacketWrapper wrapper) {
        return OUTGOING.dispatch(player, wrapper);
    }

    private static <S, T extends BedrockPacket> Binding<S, T> bind(Function<GeyserPlayer, S> state, BiConsumer<S, GeyserPacketEvent<T>> handler) {
        return new Binding<>(state, handler);
    }

    private record Binding<S, T extends BedrockPacket>(Function<GeyserPlayer, S> state, BiConsumer<S, GeyserPacketEvent<T>> handler) {
        GeyserPacketEvent<T> dispatch(GeyserPlayer player, Class<T> type, BedrockPacketWrapper wrapper, GeyserPacketEvent<T> event) {
            S target = this.state.apply(player);
            if (target != null) {
                if (event == null) {
                    event = new GeyserPacketEvent<>(wrapper, type);
                }
                this.handler.accept(target, event);
            }
            return event;
        }
    }

    private static final class Table {
        private final Map<Class<?>, Handler> handlers = new IdentityHashMap<>();

        @SafeVarargs
        private <T extends BedrockPacket> void on(Class<T> type, Binding<?, T>... bindings) {
            this.handlers.put(type, (player, wrapper) -> {
                // 同一包只创建一个事件，前序替换对后续处理可见，取消后立即停止分发。
                GeyserPacketEvent<T> event = null;
                for (Binding<?, T> binding : bindings) {
                    event = binding.dispatch(player, type, wrapper, event);
                    if (event != null && event.isCancelled()) {
                        return true;
                    }
                }
                return false;
            });
        }

        private boolean dispatch(GeyserPlayer player, BedrockPacketWrapper wrapper) {
            Handler handler = this.handlers.get(wrapper.getPacket().getClass());
            return handler != null && handler.dispatch(player, wrapper);
        }
    }

    @FunctionalInterface
    private interface Handler {
        boolean dispatch(GeyserPlayer player, BedrockPacketWrapper wrapper);
    }
}