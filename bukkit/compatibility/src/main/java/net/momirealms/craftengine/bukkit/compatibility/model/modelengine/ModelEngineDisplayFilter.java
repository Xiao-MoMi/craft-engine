package net.momirealms.craftengine.bukkit.compatibility.model.modelengine;

import com.ticxo.modelengine.api.nms.network.ProtectedPacket;
import com.ticxo.modelengine.api.nms.network.ProtectedPacketUnpacker;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntList;
import net.momirealms.craftengine.bukkit.entity.data.DisplayData;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.BundlePacketProxy;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.game.ClientboundBundlePacketProxy;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacketProxy;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.game.ClientboundSetEntityDataPacketProxy;
import net.momirealms.craftengine.proxy.minecraft.network.syncher.SynchedEntityDataProxy.DataValueProxy;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

final class ModelEngineDisplayFilter extends ChannelOutboundHandlerAdapter {
    private static final String NAME = "craftengine_modelengine_displays";
    private static final DisplayData<?>[] TRANSFORM = {
            DisplayData.TransformationInterpolationDelay,
            VersionHelper.isOrAbove1_20_2 ? DisplayData.TransformationInterpolationDuration : DisplayData.InterpolationDuration,
            DisplayData.Translation, DisplayData.Scale, DisplayData.LeftRotation, DisplayData.RightRotation
    };
    private final Int2ObjectOpenHashMap<Bone> bones = new Int2ObjectOpenHashMap<>();

    static void update(Channel channel, List<Integer> previous, List<Integer> current) {
        if (channel == null || !channel.isOpen() || previous.equals(current)) {
            return;
        }
        if (channel.eventLoop().inEventLoop()) {
            updateOnEventLoop(channel, previous, current);
        } else {
            channel.eventLoop().execute(() -> updateOnEventLoop(channel, previous, current));
        }
    }

    private static void updateOnEventLoop(Channel channel, List<Integer> previous, List<Integer> current) {
        ChannelPipeline pipeline = channel.pipeline();
        ChannelHandlerContext context = pipeline.context(NAME);
        if (context == null) {
            ChannelHandlerContext unpacker = pipeline.context(ProtectedPacketUnpacker.class);
            if (current.isEmpty() || unpacker == null || !channel.isActive()) {
                return;
            }
            pipeline.addAfter(unpacker.name(), NAME, new ModelEngineDisplayFilter());
            context = pipeline.context(NAME);
        }
        ModelEngineDisplayFilter filter = (ModelEngineDisplayFilter) context.handler();
        filter.replace(context, previous, current);
        if (filter.bones.isEmpty()) {
            pipeline.remove(filter);
        }
    }

    private void replace(ChannelHandlerContext context, List<Integer> previous, List<Integer> current) {
        for (int i = 0; i < current.size(); i++) {
            int id = current.get(i);
            Bone bone = this.bones.get(id);
            if (bone == null) {
                this.bones.put(id, new Bone());
            } else {
                bone.owners++;
            }
        }
        boolean replayed = false;
        for (int i = 0; i < previous.size(); i++) {
            int id = previous.get(i);
            Bone bone = this.bones.get(id);
            if (bone != null && --bone.owners == 0) {
                this.bones.remove(id);
                replayed |= bone.replay(context, id);
            }
        }
        if (replayed) {
            context.flush();
        }
    }

    @Override
    public void write(ChannelHandlerContext context, Object message, ChannelPromise promise) {
        if (message instanceof ProtectedPacket protectedPacket) {
            Object packet = protectedPacket.packet();
            Object filtered = this.filter(packet);
            if (filtered == null) {
                promise.trySuccess();
                return;
            }
            if (filtered != packet) {
                message = new ProtectedPacket(filtered);
            }
        }
        context.write(message, promise);
    }

    private Object filter(Object packet) {
        if (ClientboundBundlePacketProxy.CLASS.isInstance(packet)) {
            Iterable<Object> packets = BundlePacketProxy.INSTANCE.getPackets(packet);
            List<Object> kept = null;
            int index = 0;
            for (Object child : packets) {
                Object filtered = this.filter(child);
                if (kept == null && filtered != child) {
                    kept = new ArrayList<>();
                    for (Object previous : packets) {
                        if (kept.size() == index) {
                            break;
                        }
                        kept.add(previous);
                    }
                }
                if (kept != null && filtered != null) {
                    kept.add(filtered);
                }
                index++;
            }
            if (kept != null) {
                return kept.isEmpty() ? null : ClientboundBundlePacketProxy.INSTANCE.newInstance(kept);
            }
        } else if (ClientboundSetEntityDataPacketProxy.CLASS.isInstance(packet)) {
            Bone bone = this.bones.get(ClientboundSetEntityDataPacketProxy.INSTANCE.getId(packet));
            if (bone != null && !bone.removed && bone.capture(ClientboundSetEntityDataPacketProxy.INSTANCE.getPackedItems(packet))) {
                return null;
            }
        } else if (ClientboundRemoveEntitiesPacketProxy.CLASS.isInstance(packet)) {
            IntList ids = ClientboundRemoveEntitiesPacketProxy.INSTANCE.getEntityIds(packet);
            for (int i = 0; i < ids.size(); i++) {
                Bone bone = this.bones.get(ids.getInt(i));
                if (bone != null) {
                    bone.pending = null;
                    bone.removed = true;
                }
            }
        }
        return packet;
    }

    private static final class Bone {
        private int owners = 1;
        private boolean removed;
        private Object[] pending;

        private boolean replay(ChannelHandlerContext context, int id) {
            if (this.pending == null) {
                return false;
            }
            List<Object> metadata = new ArrayList<>(TRANSFORM.length);
            for (int slot = 0; slot < TRANSFORM.length; slot++) {
                if (this.pending[slot] != null) {
                    DisplayData<?> data = TRANSFORM[slot];
                    metadata.add(DataValueProxy.INSTANCE.newInstance(data.id, data.serializer, this.pending[slot]));
                }
            }
            if (metadata.isEmpty()) {
                return false;
            }
            context.write(new ProtectedPacket(ClientboundSetEntityDataPacketProxy.INSTANCE.newInstance(id, metadata)));
            return true;
        }

        private boolean capture(List<Object> metadata) {
            if (metadata.isEmpty()) {
                return false;
            }
            boolean pure = true;
            for (int i = 0; i < metadata.size(); i++) {
                if (slot(metadata.get(i)) < 0) {
                    pure = false;
                    break;
                }
            }
            if (this.pending == null) {
                if (!pure) {
                    return false;
                }
                this.pending = new Object[TRANSFORM.length];
            }
            for (int i = 0; i < metadata.size(); i++) {
                Object entry = metadata.get(i);
                int slot = slot(entry);
                if (slot < 0) {
                    continue;
                }
                if (!pure) {
                    // 混合包照常发送，其中的最新字段不可再用旧缓存覆盖。
                    this.pending[slot] = null;
                    continue;
                }
                Object value = DataValueProxy.INSTANCE.getValue(entry);
                if (value instanceof Vector3f vector) {
                    this.pending[slot] = this.pending[slot] instanceof Vector3f old ? old.set(vector) : new Vector3f(vector);
                } else if (value instanceof Quaternionf rotation) {
                    this.pending[slot] = this.pending[slot] instanceof Quaternionf old ? old.set(rotation) : new Quaternionf(rotation);
                } else {
                    this.pending[slot] = value;
                }
            }
            return pure;
        }

        private static int slot(Object entry) {
            int id = DataValueProxy.INSTANCE.getId(entry);
            int slot = id - DisplayData.TransformationInterpolationDelay.id;
            if (id >= DisplayData.Translation.id) {
                slot = 2 + id - DisplayData.Translation.id;
            }
            if (slot < 0 || slot >= TRANSFORM.length || TRANSFORM[slot].id != id || TRANSFORM[slot].serializer != DataValueProxy.INSTANCE.getSerializer(entry)) {
                return -1;
            }
            return slot;
        }
    }
}
