package net.momirealms.craftengine.bukkit.plugin.network.listener.login;

import com.google.common.collect.ImmutableMultimap;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import net.momirealms.craftengine.bukkit.util.LegacyAuthLibUtils;
import net.momirealms.craftengine.core.plugin.network.ConnectionState;
import net.momirealms.craftengine.core.plugin.network.NetWorkUser;
import net.momirealms.craftengine.core.plugin.network.event.ByteBufPacketEvent;
import net.momirealms.craftengine.core.plugin.network.listener.ByteBufferPacketListener;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.core.util.VersionHelper;

public final class LoginFinishedListener implements ByteBufferPacketListener {
    public static final ByteBufferPacketListener INSTANCE = new LoginFinishedListener();

    private LoginFinishedListener() {}

    @Override
    public void onPacketSend(NetWorkUser user, ByteBufPacketEvent event) {
        if (!VersionHelper.isOrAbove1_20_2) {
            /*
            发送这个包以后在1.20.1会从login切换到play
            1. send ClientboundGameProfilePacket
            2. placeNewPlayer 在 ServerLoginPacketListenerImpl
            3. new ServerGamePacketListenerImpl 在 PlayerList 的 placeNewPlayer
             */
            user.setConnectionState(ConnectionState.PLAY);
        }
        FriendlyByteBuf buffer = event.getBuffer();
        user.setVerifiedUUID(buffer.readUUID());
        user.setVerifiedName(buffer.readUtf(16));
        int count = buffer.readVarInt();
        PropertyMap propertyMap;
        if (VersionHelper.isOrAbove1_21_9) {
            ImmutableMultimap.Builder<String, Property> builder = ImmutableMultimap.builder();
            for (int i = 0; i < count; ++i) {
                String name = buffer.readUtf(64);
                String value = buffer.readUtf();
                String signature = buffer.readNullable(buf -> buf.readUtf(1024));
                Property property = new Property(name, value, signature);
                builder.put(name, property);
            }
            propertyMap = new PropertyMap(builder.build());
        } else {
            propertyMap = LegacyAuthLibUtils.constructor$PropertyMap();
            for (int i = 0; i < count; ++i) {
                String name = buffer.readUtf(64);
                String value = buffer.readUtf();
                String signature = buffer.readNullable(buf -> buf.readUtf(1024));
                Property property = new Property(name, value, signature);
                propertyMap.put(name, property);
            }
        }
        user.setPropertyMap(propertyMap);
    }
}
