package net.momirealms.craftengine.bukkit.plugin.network.listener.game;

import net.momirealms.craftengine.bukkit.item.BukkitItemManager;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.PacketUtils;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.plugin.network.NetWorkUser;
import net.momirealms.craftengine.core.plugin.network.event.ByteBufPacketEvent;
import net.momirealms.craftengine.core.plugin.network.listener.ByteBufferPacketListener;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.proxy.minecraft.core.IdMapProxy;
import net.momirealms.craftengine.proxy.minecraft.core.RegistryProxy;
import net.momirealms.craftengine.proxy.minecraft.core.particles.*;
import net.momirealms.craftengine.proxy.minecraft.core.registries.BuiltInRegistriesProxy;
import net.momirealms.craftengine.proxy.minecraft.network.codec.StreamDecoderProxy;
import net.momirealms.craftengine.proxy.minecraft.network.codec.StreamEncoderProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.ItemStackTemplateProxy;

public final class LevelParticleListener implements ByteBufferPacketListener {
    private final int[] blockStateMapper;
    private final int[] modBlockStateMapper;
    private final Internal internal;

    public LevelParticleListener(int[] blockStateMapper, int[] modBlockStateMapper) {
        this.blockStateMapper = blockStateMapper;
        this.modBlockStateMapper = modBlockStateMapper;
        this.internal = VersionHelper.isOrAbove1_21_4 ? new V1_21_4() : VersionHelper.isOrAbove1_20_5 ? new V1_20_5() : new V1_20();
    }

    @Override
    public void onPacketSend(NetWorkUser user, ByteBufPacketEvent event) {
        this.internal.handleSend(user, event);
    }

    private interface Internal {
        void handleSend(NetWorkUser user, ByteBufPacketEvent event);
    }

    private final class V1_20 implements Internal {
        private V1_20() {}

        @Override
        public void handleSend(NetWorkUser user, ByteBufPacketEvent event) {
            FriendlyByteBuf buf = event.getBuffer();
            Object particleType = IdMapProxy.INSTANCE.byId(BuiltInRegistriesProxy.PARTICLE_TYPE, buf.readVarInt());
            boolean overrideLimiter = buf.readBoolean();
            double x = buf.readDouble();
            double y = buf.readDouble();
            double z = buf.readDouble();
            float xDist = buf.readFloat();
            float yDist = buf.readFloat();
            float zDist = buf.readFloat();
            float maxSpeed = buf.readFloat();
            int count = buf.readInt();
            Object deserializer = ParticleTypeProxy.INSTANCE.getDeserializer(particleType);
            Object option = ParticleOptionsProxy.DeserializerProxy.INSTANCE.fromNetwork(deserializer, particleType, PacketUtils.ensureNMSFriendlyByteBuf(buf));
            if (option == null) return;
            Object type = ParticleOptionsProxy.INSTANCE.getType(option);
            Object newOption;
            if (BlockParticleOptionProxy.CLASS.isInstance(option)) {
                Object blockState = BlockParticleOptionProxy.INSTANCE.getState(option);
                int id = BlockStateUtils.blockStateToId(blockState);
                int remapped = user.clientModEnabled() ? modBlockStateMapper[id] : blockStateMapper[id];
                if (remapped == id) return;
                newOption = BlockParticleOptionProxy.INSTANCE.newInstance(type, BlockStateUtils.idToBlockState(remapped));
            } else if (ItemParticleOptionProxy.CLASS.isInstance(option)) {
                BukkitItemManager itemManager = BukkitItemManager.instance();
                Object itemStack = ItemParticleOptionProxy.INSTANCE.getItemStack(option);
                Item item = itemManager.wrap(itemStack);
                item = itemManager.s2c(item, (net.momirealms.craftengine.core.entity.player.Player) user).orElse(null);
                if (item == null) return;
                newOption = ItemParticleOptionProxy.INSTANCE.newInstance$0(type, item.minecraftItem());
            } else return;
            event.setChanged(true);
            buf.clear();
            buf.writeVarInt(event.packetID());
            buf.writeVarInt(RegistryProxy.INSTANCE.getId(BuiltInRegistriesProxy.PARTICLE_TYPE, type));
            buf.writeBoolean(overrideLimiter);
            buf.writeDouble(x);
            buf.writeDouble(y);
            buf.writeDouble(z);
            buf.writeFloat(xDist);
            buf.writeFloat(yDist);
            buf.writeFloat(zDist);
            buf.writeFloat(maxSpeed);
            buf.writeInt(count);
            ParticleOptionsProxy.INSTANCE.writeToNetwork(newOption, PacketUtils.ensureNMSFriendlyByteBuf(buf));
        }
    }

    private final class V1_20_5 implements Internal {
        private V1_20_5() {}

        @Override
        public void handleSend(NetWorkUser user, ByteBufPacketEvent event) {
            FriendlyByteBuf buf = event.getBuffer();
            boolean overrideLimiter = buf.readBoolean();
            double x = buf.readDouble();
            double y = buf.readDouble();
            double z = buf.readDouble();
            float xDist = buf.readFloat();
            float yDist = buf.readFloat();
            float zDist = buf.readFloat();
            float maxSpeed = buf.readFloat();
            int count = buf.readInt();
            Object option = StreamDecoderProxy.INSTANCE.decode(ParticleTypesProxy.STREAM_CODEC, PacketUtils.ensureNMSFriendlyByteBuf(buf.source()));
            if (option == null) return;
            Object newOption;
            if (BlockParticleOptionProxy.CLASS.isInstance(option)) {
                Object blockState = BlockParticleOptionProxy.INSTANCE.getState(option);
                int id = BlockStateUtils.blockStateToId(blockState);
                int remapped = user.clientModEnabled() ? modBlockStateMapper[id] : blockStateMapper[id];
                if (remapped == id) return;
                Object type = BlockParticleOptionProxy.INSTANCE.getType(option);
                newOption = BlockParticleOptionProxy.INSTANCE.newInstance(type, BlockStateUtils.idToBlockState(remapped));
            } else if (ItemParticleOptionProxy.CLASS.isInstance(option)) {
                BukkitItemManager itemManager = BukkitItemManager.instance();
                Object itemStack = ItemParticleOptionProxy.INSTANCE.getItemStack(option);
                Item item = itemManager.wrap(itemStack);
                item = itemManager.s2c(item, (net.momirealms.craftengine.core.entity.player.Player) user).orElse(null);
                if (item == null) return;
                Object type = ItemParticleOptionProxy.INSTANCE.getType(option);
                newOption = ItemParticleOptionProxy.INSTANCE.newInstance$0(type, item.minecraftItem());
            } else return;
            event.setChanged(true);
            buf.clear();
            buf.writeVarInt(event.packetID());
            buf.writeBoolean(overrideLimiter);
            buf.writeDouble(x);
            buf.writeDouble(y);
            buf.writeDouble(z);
            buf.writeFloat(xDist);
            buf.writeFloat(yDist);
            buf.writeFloat(zDist);
            buf.writeFloat(maxSpeed);
            buf.writeInt(count);
            StreamEncoderProxy.INSTANCE.encode(ParticleTypesProxy.STREAM_CODEC, PacketUtils.ensureNMSFriendlyByteBuf(buf.source()), newOption);
        }
    }

    private final class V1_21_4 implements Internal {
        private V1_21_4() {}

        @Override
        public void handleSend(NetWorkUser user, ByteBufPacketEvent event) {
            FriendlyByteBuf buf = event.getBuffer();
            boolean overrideLimiter = buf.readBoolean();
            boolean alwaysShow = buf.readBoolean();
            double x = buf.readDouble();
            double y = buf.readDouble();
            double z = buf.readDouble();
            float xDist = buf.readFloat();
            float yDist = buf.readFloat();
            float zDist = buf.readFloat();
            float maxSpeed = buf.readFloat();
            int count = buf.readInt();
            Object option = StreamDecoderProxy.INSTANCE.decode(ParticleTypesProxy.STREAM_CODEC, PacketUtils.ensureNMSFriendlyByteBuf(buf.source()));
            if (option == null) return;
            Object newOption;
            if (BlockParticleOptionProxy.CLASS.isInstance(option)) {
                Object blockState = BlockParticleOptionProxy.INSTANCE.getState(option);
                int id = BlockStateUtils.blockStateToId(blockState);
                int remapped = user.clientModEnabled() ? modBlockStateMapper[id] : blockStateMapper[id];
                if (remapped == id) return;
                Object type = BlockParticleOptionProxy.INSTANCE.getType(option);
                newOption = BlockParticleOptionProxy.INSTANCE.newInstance(type, BlockStateUtils.idToBlockState(remapped));
            } else if (ItemParticleOptionProxy.CLASS.isInstance(option)) {
                BukkitItemManager itemManager = BukkitItemManager.instance();
                Object itemStack = ItemParticleOptionProxy.INSTANCE.getItemStack(option);
                if (VersionHelper.isOrAbove26_1) {
                    itemStack = ItemStackTemplateProxy.INSTANCE.create(itemStack);
                }
                Item item = itemManager.wrap(itemStack);
                item = itemManager.s2c(item, (net.momirealms.craftengine.core.entity.player.Player) user).orElse(null);
                if (item == null) return;
                Object type = ItemParticleOptionProxy.INSTANCE.getType(option);
                Object stack = item.minecraftItem();
                if (VersionHelper.isOrAbove26_1) {
                    Object template = ItemStackTemplateProxy.INSTANCE.fromNonEmptyStack(stack);
                    newOption = ItemParticleOptionProxy.INSTANCE.newInstance$1(type, template);
                } else {
                    newOption = ItemParticleOptionProxy.INSTANCE.newInstance$0(type, stack);
                }
            } else return;
            event.setChanged(true);
            buf.clear();
            buf.writeVarInt(event.packetID());
            buf.writeBoolean(overrideLimiter);
            buf.writeBoolean(alwaysShow);
            buf.writeDouble(x);
            buf.writeDouble(y);
            buf.writeDouble(z);
            buf.writeFloat(xDist);
            buf.writeFloat(yDist);
            buf.writeFloat(zDist);
            buf.writeFloat(maxSpeed);
            buf.writeInt(count);
            StreamEncoderProxy.INSTANCE.encode(ParticleTypesProxy.STREAM_CODEC, PacketUtils.ensureNMSFriendlyByteBuf(buf.source()), newOption);
        }
    }
}
