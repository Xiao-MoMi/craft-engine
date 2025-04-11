package net.momirealms.craftengine.bukkit.plugin.network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.IntList;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslationArgument;
import net.momirealms.craftengine.bukkit.api.CraftEngineFurniture;
import net.momirealms.craftengine.bukkit.api.event.FurnitureBreakEvent;
import net.momirealms.craftengine.bukkit.api.event.FurnitureInteractEvent;
import net.momirealms.craftengine.bukkit.block.BukkitBlockManager;
import net.momirealms.craftengine.bukkit.compatibility.modelengine.ModelEngineUtils;
import net.momirealms.craftengine.bukkit.entity.furniture.BukkitFurnitureManager;
import net.momirealms.craftengine.bukkit.entity.furniture.LoadedFurniture;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.plugin.injector.BukkitInjector;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.bukkit.util.*;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.entity.player.InteractionHand;
import net.momirealms.craftengine.core.font.FontManager;
import net.momirealms.craftengine.core.font.IllegalCharacterProcessResult;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.network.ConnectionState;
import net.momirealms.craftengine.core.plugin.network.NetWorkUser;
import net.momirealms.craftengine.core.plugin.network.NetworkManager;
import net.momirealms.craftengine.core.util.*;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.chunk.Palette;
import net.momirealms.craftengine.core.world.chunk.PalettedContainer;
import net.momirealms.craftengine.core.world.chunk.packet.MCSection;
import net.momirealms.sparrow.nbt.Tag;
import org.bukkit.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.util.RayTraceResult;

import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.BiConsumer;

public class PacketConsumers {
    private static int[] mappings;
    private static int[] mappingsMOD;
    private static IntIdentityList BLOCK_LIST;
    private static IntIdentityList BIOME_LIST;

    public static void init(Map<Integer, Integer> map, int registrySize) {
        mappings = new int[registrySize];
        for (int i = 0; i < registrySize; i++) {
            mappings[i] = i;
        }
        mappingsMOD = Arrays.copyOf(mappings, registrySize);
        for (Map.Entry<Integer, Integer> entry : map.entrySet()) {
            mappings[entry.getKey()] = entry.getValue();
            if (BlockStateUtils.isVanillaBlock(entry.getKey())) {
                mappingsMOD[entry.getKey()] = entry.getValue();
            }
        }
        for (int i = 0; i < mappingsMOD.length; i++) {
            if (BlockStateUtils.isVanillaBlock(i)) {
                mappingsMOD[i] = remap(i);
            }
        }
        BLOCK_LIST = new IntIdentityList(registrySize);
        BIOME_LIST = new IntIdentityList(RegistryUtils.currentBiomeRegistrySize());
    }

    public static int remap(int stateId) {
        return mappings[stateId];
    }

    public static int remapMOD(int stateId) {
        return mappingsMOD[stateId];
    }

    // TODO Use bytebuffer?
    public static final TriConsumer<NetWorkUser, NMSPacketEvent, Object> LEVEL_CHUNK_WITH_LIGHT = (user, event, packet) -> {
        try {
            if (user.clientModEnabled()) {
                BukkitServerPlayer player = (BukkitServerPlayer) user;
                Object chunkData = FastNMS.INSTANCE.field$ClientboundLevelChunkWithLightPacket$chunkData(packet);
                byte[] buffer = (byte[]) Reflections.field$ClientboundLevelChunkPacketData$buffer.get(chunkData);
                ByteBuf buf = Unpooled.copiedBuffer(buffer);
                FriendlyByteBuf friendlyByteBuf = new FriendlyByteBuf(buf);
                FriendlyByteBuf newBuf = new FriendlyByteBuf(Unpooled.buffer());
                for (int i = 0, count = player.clientSideSectionCount(); i < count; i++) {
                    try {
                        MCSection mcSection = new MCSection(BLOCK_LIST, BIOME_LIST);
                        mcSection.readPacket(friendlyByteBuf);
                        PalettedContainer<Integer> container = mcSection.blockStateContainer();
                        Palette<Integer> palette = container.data().palette();
                        if (palette.canRemap()) {
                            palette.remap(PacketConsumers::remapMOD);
                        } else {
                            for (int j = 0; j < 4096; j++) {
                                int state = container.get(j);
                                int newState = remapMOD(state);
                                if (newState != state) {
                                    container.set(j, newState);
                                }
                            }
                        }
                        mcSection.writePacket(newBuf);
                    } catch (Exception e) {
                        break;
                    }
                }
                Reflections.field$ClientboundLevelChunkPacketData$buffer.set(chunkData, newBuf.array());
            } else {
                BukkitServerPlayer player = (BukkitServerPlayer) user;
                Object chunkData = FastNMS.INSTANCE.field$ClientboundLevelChunkWithLightPacket$chunkData(packet);
                byte[] buffer = (byte[]) Reflections.field$ClientboundLevelChunkPacketData$buffer.get(chunkData);
                ByteBuf buf = Unpooled.copiedBuffer(buffer);
                FriendlyByteBuf friendlyByteBuf = new FriendlyByteBuf(buf);
                FriendlyByteBuf newBuf = new FriendlyByteBuf(Unpooled.buffer());
                for (int i = 0, count = player.clientSideSectionCount(); i < count; i++) {
                    try {
                        MCSection mcSection = new MCSection(BLOCK_LIST, BIOME_LIST);
                        mcSection.readPacket(friendlyByteBuf);
                        PalettedContainer<Integer> container = mcSection.blockStateContainer();
                        Palette<Integer> palette = container.data().palette();
                        if (palette.canRemap()) {
                            palette.remap(PacketConsumers::remap);
                        } else {
                            for (int j = 0; j < 4096; j++) {
                                int state = container.get(j);
                                int newState = remap(state);
                                if (newState != state) {
                                    container.set(j, newState);
                                }
                            }
                        }
                        mcSection.writePacket(newBuf);
                    } catch (Exception e) {
                        break;
                    }
                }
                Reflections.field$ClientboundLevelChunkPacketData$buffer.set(chunkData, newBuf.array());
            }
        } catch (Exception e) {
            CraftEngine.instance().logger().warn("Failed to handle ClientboundLevelChunkWithLightPacket", e);
        }
    };

    public static final BiConsumer<NetWorkUser, ByteBufPacketEvent> SECTION_BLOCK_UPDATE = (user, event) -> {
        try {
            if (user.clientModEnabled()) {
                FriendlyByteBuf buf = event.getBuffer();
                long pos = buf.readLong();
                int blocks = buf.readVarInt();
                short[] positions = new short[blocks];
                int[] states = new int[blocks];
                for (int i = 0; i < blocks; i++) {
                    long k = buf.readVarLong();
                    positions[i] = (short) ((int) (k & 4095L));
                    states[i] = remapMOD((int) (k >>> 12));
                }
                buf.clear();
                buf.writeVarInt(event.packetID());
                buf.writeLong(pos);
                buf.writeVarInt(blocks);
                for (int i = 0; i < blocks; i++) {
                    buf.writeVarLong((long) states[i] << 12 | positions[i]);
                }
                event.setChanged(true);
            } else {
                FriendlyByteBuf buf = event.getBuffer();
                long pos = buf.readLong();
                int blocks = buf.readVarInt();
                short[] positions = new short[blocks];
                int[] states = new int[blocks];
                for (int i = 0; i < blocks; i++) {
                    long k = buf.readVarLong();
                    positions[i] = (short) ((int) (k & 4095L));
                    states[i] = remap((int) (k >>> 12));
                }
                buf.clear();
                buf.writeVarInt(event.packetID());
                buf.writeLong(pos);
                buf.writeVarInt(blocks);
                for (int i = 0; i < blocks; i++) {
                    buf.writeVarLong((long) states[i] << 12 | positions[i]);
                }
                event.setChanged(true);
            }
        } catch (Exception e) {
            CraftEngine.instance().logger().warn("Failed to handle ClientboundSectionBlocksUpdatePacket", e);
        }
    };

    public static final BiConsumer<NetWorkUser, ByteBufPacketEvent> BLOCK_UPDATE = (user, event) -> {
        try {
            FriendlyByteBuf buf = event.getBuffer();
            BlockPos pos = buf.readBlockPos(buf);
            int before = buf.readVarInt();
            if (user.clientModEnabled() && !BlockStateUtils.isVanillaBlock(before)) {
                return;
            }
            int state = remap(before);
            if (state == before) {
                return;
            }
            event.setChanged(true);
            buf.clear();
            buf.writeVarInt(event.packetID());
            buf.writeBlockPos(pos);
            buf.writeVarInt(state);
        } catch (Exception e) {
            CraftEngine.instance().logger().warn("Failed to handle ClientboundBlockUpdatePacket", e);
        }
    };

    public static final BiConsumer<NetWorkUser, ByteBufPacketEvent> LEVEL_EVENT = (user, event) -> {
        try {
            FriendlyByteBuf buf = event.getBuffer();
            int eventId = buf.readInt();
            if (eventId != 2001) return;
            BlockPos blockPos = buf.readBlockPos(buf);
            int state = buf.readInt();
            boolean global = buf.readBoolean();
            int newState = remap(state);
            if (newState == state) {
                return;
            }
            event.setChanged(true);
            buf.clear();
            buf.writeVarInt(event.packetID());
            buf.writeInt(eventId);
            buf.writeBlockPos(blockPos);
            buf.writeInt(newState);
            buf.writeBoolean(global);
        } catch (Exception e) {
            CraftEngine.instance().logger().warn("Failed to handle ClientboundLevelEventPacket", e);
        }
    };

    public static final BiConsumer<NetWorkUser, ByteBufPacketEvent> TEAM_1_20_3 = (user, event) -> {
        if (!Config.interceptTeam()) return;
        try {
            FriendlyByteBuf buf = event.getBuffer();
            String name = buf.readUtf();
            byte method = buf.readByte();
            if (method != 2 && method != 0) {
                return;
            }
            Tag displayName = buf.readNbt(false);
            if (displayName == null) return;
            byte friendlyFlags = buf.readByte();
            String nameTagVisibility = buf.readUtf(40);
            String collisionRule = buf.readUtf(40);
            int color = buf.readVarInt();
            Tag prefix = buf.readNbt(false);
            if (prefix == null) return;
            Tag suffix = buf.readNbt(false);
            if (suffix == null) return;

            Map<String, Component> tokens1 = CraftEngine.instance().imageManager().matchTags(displayName.getAsString());
            Map<String, Component> tokens2 = CraftEngine.instance().imageManager().matchTags(prefix.getAsString());
            Map<String, Component> tokens3 = CraftEngine.instance().imageManager().matchTags(suffix.getAsString());
            if (tokens1.isEmpty() && tokens2.isEmpty() && tokens3.isEmpty()) return;
            event.setChanged(true);

            List<String> entities;
            if (method == 0) {
                entities = buf.readStringList();
            } else {
                entities = null;
            }

            buf.clear();
            buf.writeVarInt(event.packetID());
            buf.writeUtf(name);
            buf.writeByte(method);

            if (!tokens1.isEmpty()) {
                Component component = AdventureHelper.tagToComponent(displayName);
                for (Map.Entry<String, Component> token : tokens1.entrySet()) {
                    component = component.replaceText(b -> b.matchLiteral(token.getKey()).replacement(token.getValue()));
                }
                buf.writeNbt(AdventureHelper.componentToTag(component), false);
            } else {
                buf.writeNbt(displayName, false);
            }

            buf.writeByte(friendlyFlags);
            buf.writeUtf(nameTagVisibility);
            buf.writeUtf(collisionRule);
            buf.writeVarInt(color);

            if (!tokens2.isEmpty()) {
                Component component = AdventureHelper.tagToComponent(prefix);
                for (Map.Entry<String, Component> token : tokens2.entrySet()) {
                    component = component.replaceText(b -> b.matchLiteral(token.getKey()).replacement(token.getValue()));
                }
                buf.writeNbt(AdventureHelper.componentToTag(component), false);
            } else {
                buf.writeNbt(prefix, false);
            }

            if (!tokens3.isEmpty()) {
                Component component = AdventureHelper.tagToComponent(suffix);
                for (Map.Entry<String, Component> token : tokens3.entrySet()) {
                    component = component.replaceText(b -> b.matchLiteral(token.getKey()).replacement(token.getValue()));
                }
                buf.writeNbt(AdventureHelper.componentToTag(component), false);
            } else {
                buf.writeNbt(suffix, false);
            }

            if (entities != null) {
                buf.writeStringList(entities);
            }
        } catch (Exception e) {
            CraftEngine.instance().logger().warn("Failed to handle ClientboundSetPlayerTeamPacket", e);
        }
    };

    public static final TriConsumer<NetWorkUser, NMSPacketEvent, Object> PLAYER_INFO_UPDATE = (user, event, packet) -> {
        try {
            if (!user.isOnline()) return;
            if (!Config.interceptPlayerInfo()) return;
            List<Object> entries = FastNMS.INSTANCE.field$ClientboundPlayerInfoUpdatePacket$entries(packet);
            if (entries instanceof MarkedArrayList) {
                return;
            }
            EnumSet<? extends Enum<?>> enums = FastNMS.INSTANCE.field$ClientboundPlayerInfoUpdatePacket$actions(packet);
            outer: {
                for (Object entry : enums) {
                    if (entry == Reflections.instance$ClientboundPlayerInfoUpdatePacket$Action$UPDATE_DISPLAY_NAME) {
                        break outer;
                    }
                }
                return;
            }

            boolean isChanged = false;
            List<Object> newEntries = new MarkedArrayList<>();
            for (Object entry : entries) {
                Object mcComponent = FastNMS.INSTANCE.field$ClientboundPlayerInfoUpdatePacket$Entry$displayName(entry);
                if (mcComponent == null) {
                    newEntries.add(entry);
                    continue;
                }
                String json = ComponentUtils.minecraftToJson(mcComponent);
                Map<String, Component> tokens = CraftEngine.instance().imageManager().matchTags(json);
                if (tokens.isEmpty()) {
                    newEntries.add(entry);
                    continue;
                }
                Component component = AdventureHelper.jsonToComponent(json);
                for (Map.Entry<String, Component> token : tokens.entrySet()) {
                    component = component.replaceText(b -> b.matchLiteral(token.getKey()).replacement(token.getValue()));
                }
                Object newEntry = FastNMS.INSTANCE.constructor$ClientboundPlayerInfoUpdatePacket$Entry(entry, ComponentUtils.adventureToMinecraft(component));
                newEntries.add(newEntry);
                isChanged = true;
            }
            if (isChanged) {
                event.setNewPacket(FastNMS.INSTANCE.constructor$ClientboundPlayerInfoUpdatePacket(enums, newEntries));
            }
        } catch (Exception e) {
            CraftEngine.instance().logger().warn("Failed to handle ClientboundPlayerInfoUpdatePacket", e);
        }
    };

    public static final BiConsumer<NetWorkUser, ByteBufPacketEvent> TEAM_1_20 = (user, event) -> {
        if (!Config.interceptTeam()) return;
        try {
            FriendlyByteBuf buf = event.getBuffer();
            String name = buf.readUtf();
            byte method = buf.readByte();
            if (method != 2 && method != 0) {
                return;
            }
            String displayName = buf.readUtf();
            byte friendlyFlags = buf.readByte();
            String nameTagVisibility = buf.readUtf(40);
            String collisionRule = buf.readUtf(40);
            int color = buf.readVarInt();
            String prefix = buf.readUtf();
            String suffix = buf.readUtf();

            Map<String, Component> tokens1 = CraftEngine.instance().imageManager().matchTags(displayName);
            Map<String, Component> tokens2 = CraftEngine.instance().imageManager().matchTags(prefix);
            Map<String, Component> tokens3 = CraftEngine.instance().imageManager().matchTags(suffix);
            if (tokens1.isEmpty() && tokens2.isEmpty() && tokens3.isEmpty()) return;
            event.setChanged(true);

            List<String> entities;
            if (method == 0) {
                entities = buf.readStringList();
            } else {
                entities = null;
            }

            buf.clear();
            buf.writeVarInt(event.packetID());
            buf.writeUtf(name);
            buf.writeByte(method);

            if (!tokens1.isEmpty()) {
                Component component = AdventureHelper.jsonToComponent(displayName);
                for (Map.Entry<String, Component> token : tokens1.entrySet()) {
                    component = component.replaceText(b -> b.matchLiteral(token.getKey()).replacement(token.getValue()));
                }
                buf.writeUtf(AdventureHelper.componentToJson(component));
            } else {
                buf.writeUtf(displayName);
            }

            buf.writeByte(friendlyFlags);
            buf.writeUtf(nameTagVisibility);
            buf.writeUtf(collisionRule);
            buf.writeVarInt(color);

            if (!tokens2.isEmpty()) {
                Component component = AdventureHelper.jsonToComponent(prefix);
                for (Map.Entry<String, Component> token : tokens2.entrySet()) {
                    component = component.replaceText(b -> b.matchLiteral(token.getKey()).replacement(token.getValue()));
                }
                buf.writeUtf(AdventureHelper.componentToJson(component));
            } else {
                buf.writeUtf(prefix);
            }

            if (!tokens3.isEmpty()) {
                Component component = AdventureHelper.jsonToComponent(suffix);
                for (Map.Entry<String, Component> token : tokens3.entrySet()) {
                    component = component.replaceText(b -> b.matchLiteral(token.getKey()).replacement(token.getValue()));
                }
                buf.writeUtf(AdventureHelper.componentToJson(component));
            } else {
                buf.writeUtf(suffix);
            }

            if (entities != null) {
                buf.writeStringList(entities);
            }
        } catch (Exception e) {
            CraftEngine.instance().logger().warn("Failed to handle ClientboundSetPlayerTeamPacket", e);
        }
    };

    public static final BiConsumer<NetWorkUser, ByteBufPacketEvent> BOSS_EVENT_1_20 = (user, event) -> {
        if (!Config.interceptBossBar()) return;
        try {
            FriendlyByteBuf buf = event.getBuffer();
            UUID uuid = buf.readUUID();
            int actionType = buf.readVarInt();
            if (actionType == 0) {
                String json = buf.readUtf();
                Map<String, Component> tokens = CraftEngine.instance().imageManager().matchTags(json);
                if (tokens.isEmpty()) return;
                Component component = AdventureHelper.jsonToComponent(json);
                for (Map.Entry<String, Component> token : tokens.entrySet()) {
                    component = component.replaceText(b -> b.matchLiteral(token.getKey()).replacement(token.getValue()));
                }
                float health = buf.readFloat();
                int color = buf.readVarInt();
                int division = buf.readVarInt();
                byte flag = buf.readByte();
                event.setChanged(true);
                buf.clear();
                buf.writeVarInt(event.packetID());
                buf.writeUUID(uuid);
                buf.writeVarInt(actionType);
                buf.writeUtf(AdventureHelper.componentToJson(component));
                buf.writeFloat(health);
                buf.writeVarInt(color);
                buf.writeVarInt(division);
                buf.writeByte(flag);
            } else if (actionType == 3) {
                String json = buf.readUtf();
                Map<String, Component> tokens = CraftEngine.instance().imageManager().matchTags(json);
                if (tokens.isEmpty()) return;
                event.setChanged(true);
                Component component = AdventureHelper.jsonToComponent(json);
                for (Map.Entry<String, Component> token : tokens.entrySet()) {
                    component = component.replaceText(b -> b.matchLiteral(token.getKey()).replacement(token.getValue()));
                }
                buf.clear();
                buf.writeVarInt(event.packetID());
                buf.writeUUID(uuid);
                buf.writeVarInt(actionType);
                buf.writeUtf(AdventureHelper.componentToJson(component));
            }
        } catch (Exception e) {
            CraftEngine.instance().logger().warn("Failed to handle ClientboundBossEventPacket", e);
        }
    };

    public static final BiConsumer<NetWorkUser, ByteBufPacketEvent> BOSS_EVENT_1_20_3 = (user, event) -> {
        if (!Config.interceptBossBar()) return;
        try {
            FriendlyByteBuf buf = event.getBuffer();
            UUID uuid = buf.readUUID();
            int actionType = buf.readVarInt();
            if (actionType == 0) {
                Tag nbt = buf.readNbt(false);
                if (nbt == null) return;
                Map<String, Component> tokens = CraftEngine.instance().imageManager().matchTags(nbt.getAsString());
                if (tokens.isEmpty()) return;
                Component component = AdventureHelper.tagToComponent(nbt);
                for (Map.Entry<String, Component> token : tokens.entrySet()) {
                    component = component.replaceText(b -> b.matchLiteral(token.getKey()).replacement(token.getValue()));
                }
                float health = buf.readFloat();
                int color = buf.readVarInt();
                int division = buf.readVarInt();
                byte flag = buf.readByte();
                event.setChanged(true);
                buf.clear();
                buf.writeVarInt(event.packetID());
                buf.writeUUID(uuid);
                buf.writeVarInt(actionType);
                buf.writeNbt(AdventureHelper.componentToTag(component), false);
                buf.writeFloat(health);
                buf.writeVarInt(color);
                buf.writeVarInt(division);
                buf.writeByte(flag);
            } else if (actionType == 3) {
                Tag nbt = buf.readNbt(false);
                if (nbt == null) return;
                Map<String, Component> tokens = CraftEngine.instance().imageManager().matchTags(nbt.getAsString());
                if (tokens.isEmpty()) return;
                event.setChanged(true);
                Component component = AdventureHelper.tagToComponent(nbt);
                for (Map.Entry<String, Component> token : tokens.entrySet()) {
                    component = component.replaceText(b -> b.matchLiteral(token.getKey()).replacement(token.getValue()));
                }
                buf.clear();
                buf.writeVarInt(event.packetID());
                buf.writeUUID(uuid);
                buf.writeVarInt(actionType);
                buf.writeNbt(AdventureHelper.componentToTag(component), false);
            }
        } catch (Exception e) {
            CraftEngine.instance().logger().warn("Failed to handle ClientboundBossEventPacket", e);
        }
    };

    public static final BiConsumer<NetWorkUser, ByteBufPacketEvent> SET_OBJECTIVE_1_20 = (user, event) -> {
        if (!Config.interceptScoreboard()) return;
        try {
            FriendlyByteBuf buf = event.getBuffer();
            String objective = buf.readUtf();
            byte mode = buf.readByte();
            if (mode != 0 && mode != 2) return;
            String displayName = buf.readUtf();
            int renderType = buf.readVarInt();
            Map<String, Component> tokens = CraftEngine.instance().imageManager().matchTags(displayName);
            if (tokens.isEmpty()) return;
            event.setChanged(true);
            Component component = AdventureHelper.jsonToComponent(displayName);
            for (Map.Entry<String, Component> token : tokens.entrySet()) {
                component = component.replaceText(b -> b.matchLiteral(token.getKey()).replacement(token.getValue()));
            }
            buf.clear();
            buf.writeVarInt(event.packetID());
            buf.writeUtf(objective);
            buf.writeByte(mode);
            buf.writeUtf(AdventureHelper.componentToJson(component));
            buf.writeVarInt(renderType);
        } catch (Exception e) {
            CraftEngine.instance().logger().warn("Failed to handle ClientboundSetObjectivePacket", e);
        }
    };

    public static final BiConsumer<NetWorkUser, ByteBufPacketEvent> SET_OBJECTIVE_1_20_3 = (user, event) -> {
        if (!Config.interceptScoreboard()) return;
        try {
            FriendlyByteBuf buf = event.getBuffer();
            String objective = buf.readUtf();
            byte mode = buf.readByte();
            if (mode != 0 && mode != 2) return;
            Tag displayName = buf.readNbt(false);
            if (displayName == null) return;
            int renderType = buf.readVarInt();
            boolean optionalNumberFormat = buf.readBoolean();
            if (optionalNumberFormat) {
                int format = buf.readVarInt();
                if (format == 0) {
                    Map<String, Component> tokens = CraftEngine.instance().imageManager().matchTags(displayName.getAsString());
                    if (tokens.isEmpty()) return;
                    event.setChanged(true);
                    Component component = AdventureHelper.tagToComponent(displayName);
                    for (Map.Entry<String, Component> token : tokens.entrySet()) {
                        component = component.replaceText(b -> b.matchLiteral(token.getKey()).replacement(token.getValue()));
                    }
                    buf.clear();
                    buf.writeVarInt(event.packetID());
                    buf.writeUtf(objective);
                    buf.writeByte(mode);
                    buf.writeNbt(AdventureHelper.componentToTag(component), false);
                    buf.writeVarInt(renderType);
                    buf.writeBoolean(true);
                    buf.writeVarInt(0);
                } else if (format == 1) {
                    Map<String, Component> tokens = CraftEngine.instance().imageManager().matchTags(displayName.getAsString());
                    if (tokens.isEmpty()) return;
                    Tag style = buf.readNbt(false);
                    event.setChanged(true);
                    Component component = AdventureHelper.tagToComponent(displayName);
                    for (Map.Entry<String, Component> token : tokens.entrySet()) {
                        component = component.replaceText(b -> b.matchLiteral(token.getKey()).replacement(token.getValue()));
                    }
                    buf.clear();
                    buf.writeVarInt(event.packetID());
                    buf.writeUtf(objective);
                    buf.writeByte(mode);
                    buf.writeNbt(AdventureHelper.componentToTag(component), false);
                    buf.writeVarInt(renderType);
                    buf.writeBoolean(true);
                    buf.writeVarInt(1);
                    buf.writeNbt(style, false);
                } else if (format == 2) {
                    Tag fixed = buf.readNbt(false);
                    if (fixed == null) return;
                    Map<String, Component> tokens1 = CraftEngine.instance().imageManager().matchTags(displayName.getAsString());
                    Map<String, Component> tokens2 = CraftEngine.instance().imageManager().matchTags(fixed.getAsString());
                    if (tokens1.isEmpty() && tokens2.isEmpty()) return;
                    event.setChanged(true);
                    buf.clear();
                    buf.writeVarInt(event.packetID());
                    buf.writeUtf(objective);
                    buf.writeByte(mode);
                    if (!tokens1.isEmpty()) {
                        Component component = AdventureHelper.tagToComponent(displayName);
                        for (Map.Entry<String, Component> token : tokens1.entrySet()) {
                            component = component.replaceText(b -> b.matchLiteral(token.getKey()).replacement(token.getValue()));
                        }
                        buf.writeNbt(AdventureHelper.componentToTag(component), false);
                    } else {
                        buf.writeNbt(displayName, false);
                    }
                    buf.writeVarInt(renderType);
                    buf.writeBoolean(true);
                    buf.writeVarInt(2);
                    if (!tokens2.isEmpty()) {
                        Component component = AdventureHelper.tagToComponent(fixed);
                        for (Map.Entry<String, Component> token : tokens2.entrySet()) {
                            component = component.replaceText(b -> b.matchLiteral(token.getKey()).replacement(token.getValue()));
                        }
                        buf.writeNbt(AdventureHelper.componentToTag(component), false);
                    } else {
                        buf.writeNbt(fixed, false);
                    }
                }
            } else {
                Map<String, Component> tokens = CraftEngine.instance().imageManager().matchTags(displayName.getAsString());
                if (tokens.isEmpty()) return;
                event.setChanged(true);
                Component component = AdventureHelper.tagToComponent(displayName);
                for (Map.Entry<String, Component> token : tokens.entrySet()) {
                    component = component.replaceText(b -> b.matchLiteral(token.getKey()).replacement(token.getValue()));
                }
                buf.clear();
                buf.writeVarInt(event.packetID());
                buf.writeUtf(objective);
                buf.writeByte(mode);
                buf.writeNbt(AdventureHelper.componentToTag(component), false);
                buf.writeVarInt(renderType);
                buf.writeBoolean(false);
            }
        } catch (Exception e) {
            CraftEngine.instance().logger().warn("Failed to handle ClientboundSetObjectivePacket", e);
        }
    };

    public static final BiConsumer<NetWorkUser, ByteBufPacketEvent> SYSTEM_CHAT_1_20 = (user, event) -> {
        if (!Config.interceptSystemChat()) return;
        try {
            FriendlyByteBuf buf = event.getBuffer();
            String jsonOrPlainString = buf.readUtf();
            Map<String, Component> tokens = CraftEngine.instance().imageManager().matchTags(jsonOrPlainString);
            if (tokens.isEmpty()) return;
            boolean overlay = buf.readBoolean();
            event.setChanged(true);
            Component component = AdventureHelper.jsonToComponent(jsonOrPlainString);
            for (Map.Entry<String, Component> token : tokens.entrySet()) {
                component = component.replaceText(b -> b.matchLiteral(token.getKey()).replacement(token.getValue()));
            }
            buf.clear();
            buf.writeVarInt(event.packetID());
            buf.writeUtf(AdventureHelper.componentToJson(component));
            buf.writeBoolean(overlay);
        } catch (Exception e) {
            CraftEngine.instance().logger().warn("Failed to handle ClientboundSystemChatPacket", e);
        }
    };

    public static final BiConsumer<NetWorkUser, ByteBufPacketEvent> SYSTEM_CHAT_1_20_3 = (user, event) -> {
        if (!Config.interceptSystemChat()) return;
        try {
            FriendlyByteBuf buf = event.getBuffer();
            Tag nbt = buf.readNbt(false);
            if (nbt == null) return;
            Map<String, Component> tokens = CraftEngine.instance().imageManager().matchTags(nbt.getAsString());
            if (tokens.isEmpty()) return;
            boolean overlay = buf.readBoolean();
            event.setChanged(true);
            Component component = AdventureHelper.tagToComponent(nbt);
            for (Map.Entry<String, Component> token : tokens.entrySet()) {
                component = component.replaceText(b -> b.matchLiteral(token.getKey()).replacement(token.getValue()));
            }
            buf.clear();
            buf.writeVarInt(event.packetID());
            buf.writeNbt(AdventureHelper.componentToTag(component), false);
            buf.writeBoolean(overlay);
        } catch (Exception e) {
            CraftEngine.instance().logger().warn("Failed to handle ClientboundSystemChatPacket", e);
        }
    };

    public static final BiConsumer<NetWorkUser, ByteBufPacketEvent> SET_SUBTITLE_TEXT_1_20 = (user, event) -> {
        if (!Config.interceptTitle()) return;
        try {
            FriendlyByteBuf buf = event.getBuffer();
            String json = buf.readUtf();
            Map<String, Component> tokens = CraftEngine.instance().imageManager().matchTags(json);
            if (tokens.isEmpty()) return;
            event.setChanged(true);
            Component component = AdventureHelper.jsonToComponent(json);
            for (Map.Entry<String, Component> token : tokens.entrySet()) {
                component = component.replaceText(b -> b.matchLiteral(token.getKey()).replacement(token.getValue()));
            }
            buf.clear();
            buf.writeVarInt(event.packetID());
            buf.writeUtf(AdventureHelper.componentToJson(component));
        } catch (Exception e) {
            CraftEngine.instance().logger().warn("Failed to handle ClientboundSetSubtitleTextPacket", e);
        }
    };

    public static final BiConsumer<NetWorkUser, ByteBufPacketEvent> SET_SUBTITLE_TEXT_1_20_3 = (user, event) -> {
        if (!Config.interceptTitle()) return;
        try {
            FriendlyByteBuf buf = event.getBuffer();
            Tag nbt = buf.readNbt(false);
            if (nbt == null) return;
            Map<String, Component> tokens = CraftEngine.instance().imageManager().matchTags(nbt.getAsString());
            if (tokens.isEmpty()) return;
            event.setChanged(true);
            Component component = AdventureHelper.tagToComponent(nbt);
            for (Map.Entry<String, Component> token : tokens.entrySet()) {
                component = component.replaceText(b -> b.matchLiteral(token.getKey()).replacement(token.getValue()));
            }
            buf.clear();
            buf.writeVarInt(event.packetID());
            buf.writeNbt(AdventureHelper.componentToTag(component), false);
        } catch (Exception e) {
            CraftEngine.instance().logger().warn("Failed to handle ClientboundSetSubtitleTextPacket", e);
        }
    };

    public static final BiConsumer<NetWorkUser, ByteBufPacketEvent> SET_TITLE_TEXT_1_20 = (user, event) -> {
        if (!Config.interceptTitle()) return;
        try {
            FriendlyByteBuf buf = event.getBuffer();
            String json = buf.readUtf();
            Map<String, Component> tokens = CraftEngine.instance().imageManager().matchTags(json);
            if (tokens.isEmpty()) return;
            event.setChanged(true);
            Component component = AdventureHelper.jsonToComponent(json);
            for (Map.Entry<String, Component> token : tokens.entrySet()) {
                component = component.replaceText(b -> b.matchLiteral(token.getKey()).replacement(token.getValue()));
            }
            buf.clear();
            buf.writeVarInt(event.packetID());
            buf.writeUtf(AdventureHelper.componentToJson(component));
        } catch (Exception e) {
            CraftEngine.instance().logger().warn("Failed to handle ClientboundSetTitleTextPacket", e);
        }
    };

    public static final BiConsumer<NetWorkUser, ByteBufPacketEvent> SET_TITLE_TEXT_1_20_3 = (user, event) -> {
        if (!Config.interceptTitle()) return;
        try {
            FriendlyByteBuf buf = event.getBuffer();
            Tag nbt = buf.readNbt(false);
            if (nbt == null) return;
            Map<String, Component> tokens = CraftEngine.instance().imageManager().matchTags(nbt.getAsString());
            if (tokens.isEmpty()) return;
            event.setChanged(true);
            Component component = AdventureHelper.tagToComponent(nbt);
            for (Map.Entry<String, Component> token : tokens.entrySet()) {
                component = component.replaceText(b -> b.matchLiteral(token.getKey()).replacement(token.getValue()));
            }
            buf.clear();
            buf.writeVarInt(event.packetID());
            buf.writeNbt(AdventureHelper.componentToTag(component), false);
        } catch (Exception e) {
            CraftEngine.instance().logger().warn("Failed to handle ClientboundSetTitleTextPacket", e);
        }
    };

    public static final BiConsumer<NetWorkUser, ByteBufPacketEvent> SET_ACTIONBAR_TEXT_1_20 = (user, event) -> {
        if (!Config.interceptActionBar()) return;
        try {
            FriendlyByteBuf buf = event.getBuffer();
            String json = buf.readUtf();
            Map<String, Component> tokens = CraftEngine.instance().imageManager().matchTags(json);
            if (tokens.isEmpty()) return;
            event.setChanged(true);
            Component component = AdventureHelper.jsonToComponent(json);
            for (Map.Entry<String, Component> token : tokens.entrySet()) {
                component = component.replaceText(b -> b.matchLiteral(token.getKey()).replacement(token.getValue()));
            }
            buf.clear();
            buf.writeVarInt(event.packetID());
            buf.writeUtf(AdventureHelper.componentToJson(component));
        } catch (Exception e) {
            CraftEngine.instance().logger().warn("Failed to handle ClientboundSetActionBarTextPacket", e);
        }
    };

    public static final BiConsumer<NetWorkUser, ByteBufPacketEvent> SET_ACTIONBAR_TEXT_1_20_3 = (user, event) -> {
        if (!Config.interceptActionBar()) return;
        try {
            FriendlyByteBuf buf = event.getBuffer();
            Tag nbt = buf.readNbt(false);
            if (nbt == null) return;
            Map<String, Component> tokens = CraftEngine.instance().imageManager().matchTags(nbt.getAsString());
            if (tokens.isEmpty()) return;
            event.setChanged(true);
            Component component = AdventureHelper.tagToComponent(nbt);
            for (Map.Entry<String, Component> token : tokens.entrySet()) {
                component = component.replaceText(b -> b.matchLiteral(token.getKey()).replacement(token.getValue()));
            }
            buf.clear();
            buf.writeVarInt(event.packetID());
            buf.writeNbt(AdventureHelper.componentToTag(component), false);
        } catch (Exception e) {
            CraftEngine.instance().logger().warn("Failed to handle ClientboundSetActionBarTextPacket", e);
        }
    };

    public static final BiConsumer<NetWorkUser, ByteBufPacketEvent> TAB_LIST_1_20 = (user, event) -> {
        if (!Config.interceptTabList()) return;
        try {
            FriendlyByteBuf buf = event.getBuffer();
            String json1 = buf.readUtf();
            String json2 = buf.readUtf();
            Map<String, Component> tokens1 = CraftEngine.instance().imageManager().matchTags(json1);
            Map<String, Component> tokens2 = CraftEngine.instance().imageManager().matchTags(json2);
            if (tokens1.isEmpty() && tokens2.isEmpty()) return;
            event.setChanged(true);
            buf.clear();
            buf.writeVarInt(event.packetID());
            if (!tokens1.isEmpty()) {
                Component component = AdventureHelper.jsonToComponent(json1);
                for (Map.Entry<String, Component> token : tokens1.entrySet()) {
                    component = component.replaceText(b -> b.matchLiteral(token.getKey()).replacement(token.getValue()));
                }
                buf.writeUtf(AdventureHelper.componentToJson(component));
            } else {
                buf.writeUtf(json1);
            }
            if (!tokens2.isEmpty()) {
                Component component = AdventureHelper.jsonToComponent(json2);
                for (Map.Entry<String, Component> token : tokens2.entrySet()) {
                    component = component.replaceText(b -> b.matchLiteral(token.getKey()).replacement(token.getValue()));
                }
                buf.writeUtf(AdventureHelper.componentToJson(component));
            } else {
                buf.writeUtf(json2);
            }
        } catch (Exception e) {
            CraftEngine.instance().logger().warn("Failed to handle ClientboundSet[(Sub)Title/ActionBar]TextPacket", e);
        }
    };

    public static final BiConsumer<NetWorkUser, ByteBufPacketEvent> TAB_LIST_1_20_3 = (user, event) -> {
        if (!Config.interceptTabList()) return;
        try {
            FriendlyByteBuf buf = event.getBuffer();
            Tag nbt1 = buf.readNbt(false);
            if (nbt1 == null) return;
            Tag nbt2 = buf.readNbt(false);
            if (nbt2 == null) return;
            Map<String, Component> tokens1 = CraftEngine.instance().imageManager().matchTags(nbt1.getAsString());
            Map<String, Component> tokens2 = CraftEngine.instance().imageManager().matchTags(nbt2.getAsString());
            if (tokens1.isEmpty() && tokens2.isEmpty()) return;
            event.setChanged(true);
            buf.clear();
            buf.writeVarInt(event.packetID());
            if (!tokens1.isEmpty()) {
                Component component = AdventureHelper.tagToComponent(nbt1);
                for (Map.Entry<String, Component> token : tokens1.entrySet()) {
                    component = component.replaceText(b -> b.matchLiteral(token.getKey()).replacement(token.getValue()));
                }
                buf.writeNbt(AdventureHelper.componentToTag(component), false);
            } else {
                buf.writeNbt(nbt1, false);
            }
            if (!tokens2.isEmpty()) {
                Component component = AdventureHelper.tagToComponent(nbt2);
                for (Map.Entry<String, Component> token : tokens2.entrySet()) {
                    component = component.replaceText(b -> b.matchLiteral(token.getKey()).replacement(token.getValue()));
                }
                buf.writeNbt(AdventureHelper.componentToTag(component), false);
            } else {
                buf.writeNbt(nbt2, false);
            }
        } catch (Exception e) {
            CraftEngine.instance().logger().warn("Failed to handle ClientboundSet[(Sub)Title/ActionBar]TextPacket", e);
        }
    };

    public static final BiConsumer<NetWorkUser, ByteBufPacketEvent> OPEN_SCREEN_1_20 = (user, event) -> {
        if (!Config.interceptContainer()) return;
        try {
            FriendlyByteBuf buf = event.getBuffer();
            int containerId = buf.readVarInt();
            int type = buf.readVarInt();
            String json = buf.readUtf();
            Map<String, Component> tokens = CraftEngine.instance().imageManager().matchTags(json);
            if (tokens.isEmpty()) return;
            event.setChanged(true);
            Component component = AdventureHelper.jsonToComponent(json);
            for (Map.Entry<String, Component> token : tokens.entrySet()) {
                component = component.replaceText(b -> b.matchLiteral(token.getKey()).replacement(token.getValue()));
            }
            buf.clear();
            buf.writeVarInt(event.packetID());
            buf.writeVarInt(containerId);
            buf.writeVarInt(type);
            buf.writeUtf(AdventureHelper.componentToJson(component));
        } catch (Exception e) {
            CraftEngine.instance().logger().warn("Failed to handle ClientboundOpenScreenPacket", e);
        }
    };

    public static final BiConsumer<NetWorkUser, ByteBufPacketEvent> OPEN_SCREEN_1_20_3 = (user, event) -> {
        if (!Config.interceptContainer()) return;
        try {
            FriendlyByteBuf buf = event.getBuffer();
            int containerId = buf.readVarInt();
            int type = buf.readVarInt();
            Tag nbt = buf.readNbt(false);
            if (nbt == null) return;
            Map<String, Component> tokens = CraftEngine.instance().imageManager().matchTags(nbt.getAsString());
            if (tokens.isEmpty()) return;
            Component component = AdventureHelper.tagToComponent(nbt);
            for (Map.Entry<String, Component> token : tokens.entrySet()) {
                component = component.replaceText(b -> b.matchLiteral(token.getKey()).replacement(token.getValue()));
            }
            buf.clear();
            buf.writeVarInt(event.packetID());
            buf.writeVarInt(containerId);
            buf.writeVarInt(type);
            buf.writeNbt(AdventureHelper.componentToTag(component), false);
        } catch (Exception e) {
            CraftEngine.instance().logger().warn("Failed to handle ClientboundOpenScreenPacket", e);
        }
    };

    public static final BiConsumer<NetWorkUser, ByteBufPacketEvent> LEVEL_PARTICLE_1_21_3 = (user, event) -> {
        try {
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
            Object option = FastNMS.INSTANCE.method$ParticleTypes$STREAM_CODEC$decode(buf);
            if (option == null) return;
            if (!Reflections.clazz$BlockParticleOption.isInstance(option)) return;
            Object blockState = FastNMS.INSTANCE.field$BlockParticleOption$blockState(option);
            int id = BlockStateUtils.blockStateToId(blockState);
            int remapped = remap(id);
            if (remapped == id) return;
            Object type = FastNMS.INSTANCE.method$BlockParticleOption$getType(option);
            Object remappedOption = FastNMS.INSTANCE.constructor$BlockParticleOption(type, BlockStateUtils.idToBlockState(remapped));
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
            FastNMS.INSTANCE.method$ParticleTypes$STREAM_CODEC$encode(buf, remappedOption);
        } catch (Exception e) {
            CraftEngine.instance().logger().warn("Failed to handle ClientboundLevelParticlesPacket", e);
        }
    };

    public static final BiConsumer<NetWorkUser, ByteBufPacketEvent> LEVEL_PARTICLE_1_20_5 = (user, event) -> {
        try {
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
            Object option = FastNMS.INSTANCE.method$ParticleTypes$STREAM_CODEC$decode(buf);
            if (option == null) return;
            if (!Reflections.clazz$BlockParticleOption.isInstance(option)) return;
            Object blockState = FastNMS.INSTANCE.field$BlockParticleOption$blockState(option);
            int id = BlockStateUtils.blockStateToId(blockState);
            int remapped = remap(id);
            if (remapped == id) return;
            Object type = FastNMS.INSTANCE.method$BlockParticleOption$getType(option);
            Object remappedOption = FastNMS.INSTANCE.constructor$BlockParticleOption(type, BlockStateUtils.idToBlockState(remapped));
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
            FastNMS.INSTANCE.method$ParticleTypes$STREAM_CODEC$encode(buf, remappedOption);
        } catch (Exception e) {
            CraftEngine.instance().logger().warn("Failed to handle ClientboundLevelParticlesPacket", e);
        }
    };

    public static final BiConsumer<NetWorkUser, ByteBufPacketEvent> LEVEL_PARTICLE_1_20 = (user, event) -> {
        try {
            FriendlyByteBuf buf = event.getBuffer();
            Object particleType = FastNMS.INSTANCE.method$FriendlyByteBuf$readById(buf, Reflections.instance$BuiltInRegistries$PARTICLE_TYPE);
            boolean overrideLimiter = buf.readBoolean();
            double x = buf.readDouble();
            double y = buf.readDouble();
            double z = buf.readDouble();
            float xDist = buf.readFloat();
            float yDist = buf.readFloat();
            float zDist = buf.readFloat();
            float maxSpeed = buf.readFloat();
            int count = buf.readInt();
            Object option = FastNMS.INSTANCE.method$ClientboundLevelParticlesPacket$readParticle(buf, particleType);
            if (option == null) return;
            if (!Reflections.clazz$BlockParticleOption.isInstance(option)) return;
            Object blockState = FastNMS.INSTANCE.field$BlockParticleOption$blockState(option);
            int id = BlockStateUtils.blockStateToId(blockState);
            int remapped = remap(id);
            if (remapped == id) return;
            Object type = FastNMS.INSTANCE.method$BlockParticleOption$getType(option);
            Object remappedOption = FastNMS.INSTANCE.constructor$BlockParticleOption(type, BlockStateUtils.idToBlockState(remapped));
            event.setChanged(true);
            buf.clear();
            buf.writeVarInt(event.packetID());
            FastNMS.INSTANCE.method$FriendlyByteBuf$writeId(buf, remappedOption, Reflections.instance$BuiltInRegistries$PARTICLE_TYPE);
            buf.writeBoolean(overrideLimiter);
            buf.writeDouble(x);
            buf.writeDouble(y);
            buf.writeDouble(z);
            buf.writeFloat(xDist);
            buf.writeFloat(yDist);
            buf.writeFloat(zDist);
            buf.writeFloat(maxSpeed);
            buf.writeInt(count);
            FastNMS.INSTANCE.method$ParticleOptions$writeToNetwork(remappedOption, buf);
        } catch (Exception e) {
            CraftEngine.instance().logger().warn("Failed to handle ClientboundLevelParticlesPacket", e);
        }
    };

    public static final TriConsumer<NetWorkUser, NMSPacketEvent, Object> PLAYER_ACTION = (user, event, packet) -> {
        try {
            if (!user.isOnline()) return;
            BukkitServerPlayer player = (BukkitServerPlayer) user;
            Player platformPlayer = player.platformPlayer();
            World world = platformPlayer.getWorld();
            Object blockPos = FastNMS.INSTANCE.field$ServerboundPlayerActionPacket$pos(packet);
            BlockPos pos = LocationUtils.fromBlockPos(blockPos);
            if (VersionHelper.isFolia()) {
                BukkitCraftEngine.instance().scheduler().sync().run(() -> {
                    try {
                        handlePlayerActionPacketOnMainThread(player, world, pos, packet);
                    } catch (Exception e) {
                        CraftEngine.instance().logger().warn("Failed to handle ServerboundPlayerActionPacket", e);
                    }
                }, world, pos.x() >> 4, pos.z() >> 4);
            } else {
                handlePlayerActionPacketOnMainThread(player, world, pos, packet);
            }
        } catch (Exception e) {
            CraftEngine.instance().logger().warn("Failed to handle ServerboundPlayerActionPacket", e);
        }
    };

    private static void handlePlayerActionPacketOnMainThread(BukkitServerPlayer player, World world, BlockPos pos, Object packet) throws Exception {
        Object action = FastNMS.INSTANCE.field$ServerboundPlayerActionPacket$action(packet);
        if (action == Reflections.instance$ServerboundPlayerActionPacket$Action$START_DESTROY_BLOCK) {
            Object serverLevel = FastNMS.INSTANCE.field$CraftWorld$ServerLevel(world);
            Object blockState = FastNMS.INSTANCE.method$BlockGetter$getBlockState(serverLevel, LocationUtils.toBlockPos(pos));
            int stateId = BlockStateUtils.blockStateToId(blockState);
            // not a custom block
            if (BlockStateUtils.isVanillaBlock(stateId)) {
                if (Config.enableSoundSystem()) {
                    Object blockOwner = Reflections.field$StateHolder$owner.get(blockState);
                    if (BukkitBlockManager.instance().isBlockSoundRemoved(blockOwner)) {
                        player.startMiningBlock(world, pos, blockState, false, null);
                        return;
                    }
                }
                if (player.isMiningBlock() || player.shouldSyncAttribute()) {
                    player.stopMiningBlock();
                }
                return;
            }
            if (player.isAdventureMode()) {
                Object itemStack = FastNMS.INSTANCE.method$CraftItemStack$asNMSCopy(player.platformPlayer().getInventory().getItemInMainHand());
                Object blockPos = LocationUtils.toBlockPos(pos);
                Object blockInWorld = Reflections.constructor$BlockInWorld.newInstance(serverLevel, blockPos, false);
                if (VersionHelper.isVersionNewerThan1_20_5()) {
                    if (Reflections.method$ItemStack$canBreakBlockInAdventureMode != null
                            && !(boolean) Reflections.method$ItemStack$canBreakBlockInAdventureMode.invoke(
                            itemStack, blockInWorld
                    )) {
                        player.preventMiningBlock();
                        return;
                    }
                } else {
                    if (Reflections.method$ItemStack$canDestroy != null
                            && !(boolean) Reflections.method$ItemStack$canDestroy.invoke(
                            itemStack, Reflections.instance$BuiltInRegistries$BLOCK, blockInWorld
                    )) {
                        player.preventMiningBlock();
                        return;
                    }
                }
            }
            player.startMiningBlock(world, pos, blockState, true, BukkitBlockManager.instance().getImmutableBlockStateUnsafe(stateId));
        } else if (action == Reflections.instance$ServerboundPlayerActionPacket$Action$ABORT_DESTROY_BLOCK) {
            if (player.isMiningBlock()) {
                player.abortMiningBlock();
            }
        } else if (action == Reflections.instance$ServerboundPlayerActionPacket$Action$STOP_DESTROY_BLOCK) {
            if (player.isMiningBlock()) {
                player.stopMiningBlock();
            }
        }
    }

    public static final TriConsumer<NetWorkUser, NMSPacketEvent, Object> SWING_HAND = (user, event, packet) -> {
        try {
            if (!user.isOnline()) return;
            BukkitServerPlayer player = (BukkitServerPlayer) user;
            if (!player.isMiningBlock()) return;
            Object hand = FastNMS.INSTANCE.field$ServerboundSwingPacket$hand(packet);
            if (hand == Reflections.instance$InteractionHand$MAIN_HAND) {
                player.onSwingHand();
            }
        } catch (Exception e) {
            CraftEngine.instance().logger().warn("Failed to handle ServerboundSwingPacket", e);
        }
    };

    public static final TriConsumer<NetWorkUser, NMSPacketEvent, Object> USE_ITEM_ON = (user, event, packet) -> {
        try {
            if (!user.isOnline()) return;
            BukkitServerPlayer player = (BukkitServerPlayer) user;
            if (player.isMiningBlock()) {
                player.stopMiningBlock();
            }
        } catch (Exception e) {
            CraftEngine.instance().logger().warn("Failed to handle ServerboundUseItemOnPacket", e);
        }
    };

    public static final TriConsumer<NetWorkUser, NMSPacketEvent, Object> RESPAWN = (user, event, packet) -> {
        try {
            BukkitServerPlayer player = (BukkitServerPlayer) user;
            player.clearView();
            Object dimensionKey;
            if (!VersionHelper.isVersionNewerThan1_20_2()) {
                dimensionKey = Reflections.field$ClientboundRespawnPacket$dimension.get(packet);
            } else {
                Object commonInfo = Reflections.field$ClientboundRespawnPacket$commonPlayerSpawnInfo.get(packet);
                dimensionKey = Reflections.field$CommonPlayerSpawnInfo$dimension.get(commonInfo);
            }
            Object location = FastNMS.INSTANCE.field$ResourceKey$location(dimensionKey);
            World world = Bukkit.getWorld(Objects.requireNonNull(NamespacedKey.fromString(location.toString())));
            if (world != null) {
                int sectionCount = (world.getMaxHeight() - world.getMinHeight()) / 16;
                player.setClientSideSectionCount(sectionCount);
                player.setClientSideDimension(Key.of(location.toString()));
            } else {
                CraftEngine.instance().logger().warn("Failed to handle ClientboundRespawnPacket: World " + location + " does not exist");
            }
        } catch (Exception e) {
            CraftEngine.instance().logger().warn("Failed to handle ClientboundRespawnPacket", e);
        }
    };

    public static final TriConsumer<NetWorkUser, NMSPacketEvent, Object> LOGIN = (user, event, packet) -> {
        try {
            BukkitServerPlayer player = (BukkitServerPlayer) user;
            player.setConnectionState(ConnectionState.PLAY);
            Object dimensionKey;
            if (!VersionHelper.isVersionNewerThan1_20_2()) {
                dimensionKey = Reflections.field$ClientboundLoginPacket$dimension.get(packet);
            } else {
                Object commonInfo = Reflections.field$ClientboundLoginPacket$commonPlayerSpawnInfo.get(packet);
                dimensionKey = Reflections.field$CommonPlayerSpawnInfo$dimension.get(commonInfo);
            }
            Object location = FastNMS.INSTANCE.field$ResourceKey$location(dimensionKey);
            World world = Bukkit.getWorld(Objects.requireNonNull(NamespacedKey.fromString(location.toString())));
            if (world != null) {
                int sectionCount = (world.getMaxHeight() - world.getMinHeight()) / 16;
                player.setClientSideSectionCount(sectionCount);
                player.setClientSideDimension(Key.of(location.toString()));
            } else {
                CraftEngine.instance().logger().warn("Failed to handle ClientboundLoginPacket: World " + location + " does not exist");
            }
        } catch (Exception e) {
            CraftEngine.instance().logger().warn("Failed to handle ClientboundLoginPacket", e);
        }
    };

    // 1.21.4-
    // We can't find the best solution, we can only keep the feel as good as possible
    // When the hotbar is full, the latest creative mode inventory can only be accessed when the player opens the inventory screen. Currently, it is not worth further handling this issue.
    public static final TriConsumer<NetWorkUser, NMSPacketEvent, Object> SET_CREATIVE_SLOT = (user, event, packet) -> {
        try {
            if (VersionHelper.isVersionNewerThan1_21_4()) return;
            if (!user.isOnline()) return;
            BukkitServerPlayer player = (BukkitServerPlayer) user;
            if (VersionHelper.isFolia()) {
                BukkitCraftEngine.instance().scheduler().sync().run(() -> {
                    try {
                        handleSetCreativeSlotPacketOnMainThread(player, packet);
                    } catch (Exception e) {
                        CraftEngine.instance().logger().warn("Failed to handle ServerboundSetCreativeModeSlotPacket", e);
                    }
                }, (World) player.level().platformWorld(), (MCUtils.fastFloor(player.x())) >> 4, (MCUtils.fastFloor(player.z())) >> 4);
            } else {
                handleSetCreativeSlotPacketOnMainThread(player, packet);
            }
        } catch (Exception e) {
            CraftEngine.instance().logger().warn("Failed to handle ServerboundSetCreativeModeSlotPacket", e);
        }
    };

    private static void handleSetCreativeSlotPacketOnMainThread(BukkitServerPlayer player, Object packet) throws Exception {
        Player bukkitPlayer = player.platformPlayer();
        if (bukkitPlayer == null) return;
        if (bukkitPlayer.getGameMode() != GameMode.CREATIVE) return;
        int slot = VersionHelper.isVersionNewerThan1_20_5() ? Reflections.field$ServerboundSetCreativeModeSlotPacket$slotNum.getShort(packet) : Reflections.field$ServerboundSetCreativeModeSlotPacket$slotNum.getInt(packet);
        if (slot < 36 || slot > 44) return;
        ItemStack item = FastNMS.INSTANCE.method$CraftItemStack$asCraftMirror(Reflections.field$ServerboundSetCreativeModeSlotPacket$itemStack.get(packet));
        if (ItemUtils.isEmpty(item)) return;
        if (slot - 36 != bukkitPlayer.getInventory().getHeldItemSlot()) {
            return;
        }
        double interactionRange = player.getInteractionRange();
        // do ray trace to get current block
        RayTraceResult result = bukkitPlayer.rayTraceBlocks(interactionRange, FluidCollisionMode.NEVER);
        if (result == null) return;
        org.bukkit.block.Block hitBlock = result.getHitBlock();
        if (hitBlock == null) return;
        ImmutableBlockState state = BukkitBlockManager.instance().getImmutableBlockState(BlockStateUtils.blockDataToId(hitBlock.getBlockData()));
        // not a custom block
        if (state == null || state.isEmpty()) return;
        Key itemId = state.settings().itemId();
        // no item available
        if (itemId == null) return;
        BlockData data = BlockStateUtils.fromBlockData(state.vanillaBlockState().handle());
        // compare item
        if (data == null || !data.getMaterial().equals(item.getType())) return;
        ItemStack itemStack = BukkitCraftEngine.instance().itemManager().buildCustomItemStack(itemId, player);
        if (ItemUtils.isEmpty(itemStack)) {
            CraftEngine.instance().logger().warn("Item: " + itemId + " is not a valid item");
            return;
        }
        PlayerInventory inventory = bukkitPlayer.getInventory();
        int sameItemSlot = -1;
        int emptySlot = -1;
        for (int i = 0; i < 9 + 27; i++) {
            ItemStack invItem = inventory.getItem(i);
            if (ItemUtils.isEmpty(invItem)) {
                if (emptySlot == -1 && i < 9) emptySlot = i;
                continue;
            }
            if (invItem.getType().equals(itemStack.getType()) && invItem.getItemMeta().equals(itemStack.getItemMeta())) {
                if (sameItemSlot == -1) sameItemSlot = i;
            }
        }
        if (sameItemSlot != -1) {
            if (sameItemSlot < 9) {
                inventory.setHeldItemSlot(sameItemSlot);
                ItemStack previousItem = inventory.getItem(slot - 36);
                BukkitCraftEngine.instance().scheduler().sync().runDelayed(() -> inventory.setItem(slot - 36, previousItem));
            } else {
                ItemStack sameItem = inventory.getItem(sameItemSlot);
                int finalSameItemSlot = sameItemSlot;
                BukkitCraftEngine.instance().scheduler().sync().runDelayed(() -> {
                    inventory.setItem(finalSameItemSlot, new ItemStack(Material.AIR));
                    inventory.setItem(slot - 36, sameItem);
                });
            }
        } else {
            if (item.getAmount() == 1) {
                if (ItemUtils.isEmpty(inventory.getItem(slot - 36))) {
                    BukkitCraftEngine.instance().scheduler().sync().runDelayed(() -> inventory.setItem(slot - 36, itemStack));
                    return;
                }
                if (emptySlot != -1) {
                    inventory.setHeldItemSlot(emptySlot);
                    inventory.setItem(emptySlot, itemStack);
                } else {
                    BukkitCraftEngine.instance().scheduler().sync().runDelayed(() -> inventory.setItem(slot - 36, itemStack));
                }
            }
        }
    }

    // 1.21.4+
    public static final TriConsumer<NetWorkUser, NMSPacketEvent, Object> PICK_ITEM_FROM_BLOCK = (user, event, packet) -> {
        try {
            if (!user.isOnline()) return;
            Player player = (Player) user.platformPlayer();
            if (player == null) return;
            Object pos = Reflections.field$ServerboundPickItemFromBlockPacket$pos.get(packet);
            if (VersionHelper.isFolia()) {
                int x = FastNMS.INSTANCE.field$Vec3i$x(pos);
                int z = FastNMS.INSTANCE.field$Vec3i$z(pos);
                BukkitCraftEngine.instance().scheduler().sync().run(() -> {
                    try {
                        handlePickItemFromBlockPacketOnMainThread(player, pos);
                    } catch (Exception e) {
                        CraftEngine.instance().logger().warn("Failed to handle ServerboundPickItemFromBlockPacket", e);
                    }
                }, player.getWorld(), x >> 4, z >> 4);
            } else {
                BukkitCraftEngine.instance().scheduler().sync().run(() -> {
                    try {
                        handlePickItemFromBlockPacketOnMainThread(player, pos);
                    } catch (Exception e) {
                        CraftEngine.instance().logger().warn("Failed to handle ServerboundPickItemFromBlockPacket", e);
                    }
                });
            }
        } catch (Exception e) {
            CraftEngine.instance().logger().warn("Failed to handle ServerboundPickItemFromBlockPacket", e);
        }
    };

    private static void handlePickItemFromBlockPacketOnMainThread(Player player, Object pos) throws Exception {
        Object serverLevel = FastNMS.INSTANCE.field$CraftWorld$ServerLevel(player.getWorld());
        Object blockState = FastNMS.INSTANCE.method$BlockGetter$getBlockState(serverLevel, pos);
        ImmutableBlockState state = BukkitBlockManager.instance().getImmutableBlockState(BlockStateUtils.blockStateToId(blockState));
        if (state == null) return;
        Key itemId = state.settings().itemId();
        if (itemId == null) return;
        pickItem(player, itemId);
    }

    // 1.21.4+
    public static final TriConsumer<NetWorkUser, NMSPacketEvent, Object> PICK_ITEM_FROM_ENTITY = (user, event, packet) -> {
        try {
            int entityId = (int) Reflections.field$ServerboundPickItemFromEntityPacket$id.get(packet);
            LoadedFurniture furniture = BukkitFurnitureManager.instance().loadedFurnitureByEntityId(entityId);
            if (furniture == null) return;
            Player player = (Player) user.platformPlayer();
            if (player == null) return;
            if (VersionHelper.isFolia()) {
                Location location = player.getLocation();
                int x = location.getBlockX();
                int z = location.getBlockZ();
                BukkitCraftEngine.instance().scheduler().sync().run(() -> {
                    try {
                        handlePickItemFromEntityOnMainThread(player, furniture);
                    } catch (Exception e) {
                        CraftEngine.instance().logger().warn("Failed to handle ServerboundPickItemFromEntityPacket", e);
                    }
                }, player.getWorld(), x >> 4, z >> 4);
            } else {
                BukkitCraftEngine.instance().scheduler().sync().run(() -> {
                    try {
                        handlePickItemFromEntityOnMainThread(player, furniture);
                    } catch (Exception e) {
                        CraftEngine.instance().logger().warn("Failed to handle ServerboundPickItemFromEntityPacket", e);
                    }
                });
            }
        } catch (Exception e) {
            CraftEngine.instance().logger().warn("Failed to handle ServerboundPickItemFromEntityPacket", e);
        }
    };

    private static void handlePickItemFromEntityOnMainThread(Player player, LoadedFurniture furniture) throws Exception {
        Key itemId = furniture.config().settings().itemId();
        if (itemId == null) return;
        pickItem(player, itemId);
    }

    private static void pickItem(Player player, Key itemId) throws IllegalAccessException, InvocationTargetException {
        ItemStack itemStack = BukkitCraftEngine.instance().itemManager().buildCustomItemStack(itemId, BukkitCraftEngine.instance().adapt(player));
        if (itemStack == null) {
            CraftEngine.instance().logger().warn("Item: " + itemId + " is not a valid item");
            return;
        }
        assert Reflections.method$ServerGamePacketListenerImpl$tryPickItem != null;
        Reflections.method$ServerGamePacketListenerImpl$tryPickItem.invoke(
                Reflections.field$ServerPlayer$connection.get(FastNMS.INSTANCE.method$CraftPlayer$getHandle(player)), FastNMS.INSTANCE.method$CraftItemStack$asNMSCopy(itemStack));
    }

    public static final BiConsumer<NetWorkUser, ByteBufPacketEvent> ADD_ENTITY_BYTEBUFFER = (user, event) -> {
        try {
            FriendlyByteBuf buf = event.getBuffer();
            int id = buf.readVarInt();
            UUID uuid = buf.readUUID();
            int type = buf.readVarInt();
            double x = buf.readDouble();
            double y = buf.readDouble();
            double z = buf.readDouble();
            byte xRot = buf.readByte();
            byte yRot = buf.readByte();
            byte yHeadRot = buf.readByte();
            int data = buf.readVarInt();
            int xa = buf.readShort();
            int ya = buf.readShort();
            int za = buf.readShort();
            // Falling blocks
            if (type == Reflections.instance$EntityType$FALLING_BLOCK$registryId) {
                int remapped = remap(data);
                if (remapped != data) {
                    event.setChanged(true);
                    buf.clear();
                    buf.writeVarInt(event.packetID());
                    buf.writeVarInt(id);
                    buf.writeUUID(uuid);
                    buf.writeVarInt(type);
                    buf.writeDouble(x);
                    buf.writeDouble(y);
                    buf.writeDouble(z);
                    buf.writeByte(xRot);
                    buf.writeByte(yRot);
                    buf.writeByte(yHeadRot);
                    buf.writeVarInt(remapped);
                    buf.writeShort(xa);
                    buf.writeShort(ya);
                    buf.writeShort(za);
                }
            } else if (type == Reflections.instance$EntityType$BLOCK_DISPLAY$registryId) {
                user.entityView().put(id, Reflections.instance$EntityType$BLOCK_DISPLAY);
            } else if (type == Reflections.instance$EntityType$TEXT_DISPLAY$registryId) {
                user.entityView().put(id, Reflections.instance$EntityType$TEXT_DISPLAY);
            }
        } catch (Exception e) {
            CraftEngine.instance().logger().warn("Failed to handle ClientboundAddEntityPacket", e);
        }
    };

    public static final TriConsumer<NetWorkUser, NMSPacketEvent, Object> ADD_ENTITY = (user, event, packet) -> {
        try {
            Object entityType = FastNMS.INSTANCE.field$ClientboundAddEntityPacket$type(packet);
            if (entityType == Reflections.instance$EntityType$ITEM_DISPLAY) {
                // Furniture
                int entityId = FastNMS.INSTANCE.field$ClientboundAddEntityPacket$entityId(packet);
                LoadedFurniture furniture = BukkitFurnitureManager.instance().loadedFurnitureByRealEntityId(entityId);
                if (furniture != null) {
                    user.furnitureView().computeIfAbsent(furniture.baseEntityId(), k -> new ArrayList<>()).addAll(furniture.fakeEntityIds());
                    user.sendPacket(furniture.spawnPacket((Player) user.platformPlayer()), false);
                    if (Config.hideBaseEntity() && !furniture.hasExternalModel()) {
                        event.setCancelled(true);
                    }
                }
            } else if (entityType == Reflections.instance$EntityType$SHULKER) {
                // Cancel collider entity packet
                int entityId = FastNMS.INSTANCE.field$ClientboundAddEntityPacket$entityId(packet);
                LoadedFurniture furniture = BukkitFurnitureManager.instance().loadedFurnitureByRealEntityId(entityId);
                if (furniture != null) {
                    event.setCancelled(true);
                }
            }
        } catch (Exception e) {
            CraftEngine.instance().logger().warn("Failed to handle ClientboundAddEntityPacket", e);
        }
    };

    // 1.21.3+
    public static final TriConsumer<NetWorkUser, NMSPacketEvent, Object> SYNC_ENTITY_POSITION = (user, event, packet) -> {
        try {
            int entityId = FastNMS.INSTANCE.method$ClientboundEntityPositionSyncPacket$id(packet);
            if (BukkitFurnitureManager.instance().isFurnitureRealEntity(entityId)) {
                event.setCancelled(true);
            }
        } catch (Exception e) {
            CraftEngine.instance().logger().warn("Failed to handle ClientboundEntityPositionSyncPacket", e);
        }
    };

    public static final TriConsumer<NetWorkUser, NMSPacketEvent, Object> MOVE_ENTITY = (user, event, packet) -> {
        try {
            int entityId = BukkitInjector.internalFieldAccessor().field$ClientboundMoveEntityPacket$entityId(packet);
            if (BukkitFurnitureManager.instance().isFurnitureRealEntity(entityId)) {
                event.setCancelled(true);
            }
        } catch (Exception e) {
            CraftEngine.instance().logger().warn("Failed to handle ClientboundMoveEntityPacket$Pos", e);
        }
    };

    public static final BiConsumer<NetWorkUser, ByteBufPacketEvent> REMOVE_ENTITY = (user, event) -> {
        try {
            FriendlyByteBuf buf = event.getBuffer();
            boolean isChange = false;
            IntList intList = buf.readIntIdList();
            for (int i = 0, size = intList.size(); i < size; i++) {
                int entityId = intList.getInt(i);
                user.entityView().remove(entityId);
                List<Integer> entities = user.furnitureView().remove(entityId);
                if (entities == null) continue;
                for (int subEntityId : entities) {
                    isChange = true;
                    intList.add(subEntityId);
                }
            }
            if (isChange) {
                event.setChanged(true);
                buf.clear();
                buf.writeVarInt(event.packetID());
                buf.writeIntIdList(intList);
            }
        } catch (Exception e) {
            CraftEngine.instance().logger().warn("Failed to handle ClientboundRemoveEntitiesPacket", e);
        }
    };

    public static final TriConsumer<NetWorkUser, NMSPacketEvent, Object> INTERACT_ENTITY = (user, event, packet) -> {
        try {
            Player player = (Player) user.platformPlayer();
            if (player == null) return;
            int entityId;
            if (BukkitNetworkManager.hasModelEngine()) {
                int fakeId = FastNMS.INSTANCE.field$ServerboundInteractPacket$entityId(packet);
                entityId = ModelEngineUtils.interactionToBaseEntity(fakeId);
            } else {
                entityId = FastNMS.INSTANCE.field$ServerboundInteractPacket$entityId(packet);
            }
            LoadedFurniture furniture = BukkitFurnitureManager.instance().loadedFurnitureByEntityId(entityId);
            if (furniture == null) return;
            Object action = Reflections.field$ServerboundInteractPacket$action.get(packet);
            Object actionType = Reflections.method$ServerboundInteractPacket$Action$getType.invoke(action);
            if (actionType == null) return;
            Location location = furniture.baseEntity().getLocation();
            BukkitServerPlayer serverPlayer = (BukkitServerPlayer) user;
            if (serverPlayer.isSpectatorMode() || serverPlayer.isAdventureMode()) return;
            BukkitCraftEngine.instance().scheduler().sync().run(() -> {
                if (actionType == Reflections.instance$ServerboundInteractPacket$ActionType$ATTACK) {
                    if (furniture.isValid()) {
                        if (!BukkitCraftEngine.instance().antiGrief().canBreak(player, location)) {
                            return;
                        }
                        FurnitureBreakEvent breakEvent = new FurnitureBreakEvent(serverPlayer.platformPlayer(), furniture);
                        if (EventUtils.fireAndCheckCancel(breakEvent)) {
                            return;
                        }
                        CraftEngineFurniture.remove(furniture, serverPlayer, !serverPlayer.isCreativeMode(), true);
                    }
                } else if (actionType == Reflections.instance$ServerboundInteractPacket$ActionType$INTERACT_AT) {
                    InteractionHand hand;
                    Location interactionPoint;
                    try {
                        Object interactionHand = Reflections.field$ServerboundInteractPacket$InteractionAtLocationAction$hand.get(action);
                        hand = interactionHand == Reflections.instance$InteractionHand$MAIN_HAND ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
                        Object vec3 = Reflections.field$ServerboundInteractPacket$InteractionAtLocationAction$location.get(action);

                        double x = FastNMS.INSTANCE.field$Vec3$x(vec3);
                        double y = FastNMS.INSTANCE.field$Vec3$y(vec3);
                        double z = FastNMS.INSTANCE.field$Vec3$z(vec3);
                        interactionPoint = new Location(location.getWorld(), x, y, z);
                    } catch (ReflectiveOperationException e) {
                        throw new RuntimeException("Failed to get interaction hand from interact packet", e);
                    }
                    FurnitureInteractEvent interactEvent = new FurnitureInteractEvent(serverPlayer.platformPlayer(), furniture, hand, interactionPoint);
                    if (EventUtils.fireAndCheckCancel(interactEvent)) {
                        return;
                    }
                    if (player.isSneaking())
                        return;
                    furniture.findFirstAvailableSeat(entityId).ifPresent(seatPos -> {
                        if (furniture.tryOccupySeat(seatPos)) {
                            furniture.spawnSeatEntityForPlayer(Objects.requireNonNull(player.getPlayer()), seatPos);
                        }
                    });
                }
            }, player.getWorld(), location.getBlockX() >> 4, location.getBlockZ() >> 4);
        } catch (Exception e) {
            CraftEngine.instance().logger().warn("Failed to handle ServerboundInteractPacket", e);
        }
    };

    public static final BiConsumer<NetWorkUser, ByteBufPacketEvent> SOUND = (user, event) -> {
        try {
            FriendlyByteBuf buf = event.getBuffer();
            int id = buf.readVarInt();
            if (id == 0) {
                Key soundId = buf.readKey();
                Float range = null;
                if (buf.readBoolean()) {
                    range = buf.readFloat();
                }
                int source = buf.readVarInt();
                int x = buf.readInt();
                int y = buf.readInt();
                int z = buf.readInt();
                float volume = buf.readFloat();
                float pitch = buf.readFloat();
                long seed = buf.readLong();
                Key mapped = BukkitBlockManager.instance().replaceSoundIfExist(soundId);
                if (mapped != null) {
                    event.setChanged(true);
                    buf.clear();
                    buf.writeVarInt(event.packetID());
                    buf.writeVarInt(0);
                    buf.writeKey(mapped);
                    if (range != null) {
                        buf.writeBoolean(true);
                        buf.writeFloat(range);
                    } else {
                        buf.writeBoolean(false);
                    }
                    buf.writeVarInt(source);
                    buf.writeInt(x);
                    buf.writeInt(y);
                    buf.writeInt(z);
                    buf.writeFloat(volume);
                    buf.writeFloat(pitch);
                    buf.writeLong(seed);
                }
            } else {
                Optional<Object> optionalSound = FastNMS.INSTANCE.method$BuiltInRegistries$byId(Reflections.instance$BuiltInRegistries$SOUND_EVENT, id - 1);
                if (optionalSound.isEmpty()) return;
                Object soundEvent = optionalSound.get();
                Key soundId = Key.of(FastNMS.INSTANCE.method$SoundEvent$location(soundEvent));
                int source = buf.readVarInt();
                int x = buf.readInt();
                int y = buf.readInt();
                int z = buf.readInt();
                float volume = buf.readFloat();
                float pitch = buf.readFloat();
                long seed = buf.readLong();
                Key mapped = BukkitBlockManager.instance().replaceSoundIfExist(soundId);
                if (mapped != null) {
                    event.setChanged(true);
                    buf.clear();
                    buf.writeVarInt(event.packetID());
                    buf.writeVarInt(0);
                    Object newId = KeyUtils.toResourceLocation(mapped);
                    Object newSoundEvent = FastNMS.INSTANCE.constructor$SoundEvent(newId, FastNMS.INSTANCE.method$SoundEvent$fixedRange(soundEvent));
                    FastNMS.INSTANCE.method$SoundEvent$directEncode(buf, newSoundEvent);
                    buf.writeVarInt(source);
                    buf.writeInt(x);
                    buf.writeInt(y);
                    buf.writeInt(z);
                    buf.writeFloat(volume);
                    buf.writeFloat(pitch);
                    buf.writeLong(seed);
                }
            }
        } catch (Exception e) {
            CraftEngine.instance().logger().warn("Failed to handle ClientboundSoundPacket", e);
        }
    };

    // we handle it on packet level to prevent it from being captured by plugins
    public static final TriConsumer<NetWorkUser, NMSPacketEvent, Object> RENAME_ITEM = (user, event, packet) -> {
        try {
            if (!Config.filterAnvil()) return;
            if (((BukkitServerPlayer) user).hasPermission(FontManager.BYPASS_ANVIL)) {
                return;
            }
            String message = (String) Reflections.field$ServerboundRenameItemPacket$name.get(packet);
            if (message != null && !message.isEmpty()) {
                // check bypass
                FontManager manager = CraftEngine.instance().imageManager();
                IllegalCharacterProcessResult result = manager.processIllegalCharacters(message);
                if (result.has()) {
                    try {
                        Reflections.field$ServerboundRenameItemPacket$name.set(packet, result.text());
                    } catch (ReflectiveOperationException e) {
                        CraftEngine.instance().logger().warn("Failed to replace chat", e);
                    }
                }
            }
        } catch (Exception e) {
            CraftEngine.instance().logger().warn("Failed to handle ServerboundRenameItemPacket", e);
        }
    };

    // we handle it on packet level to prevent it from being captured by plugins
    public static final TriConsumer<NetWorkUser, NMSPacketEvent, Object> SIGN_UPDATE = (user, event, packet) -> {
        try {
            if (!Config.filterSign()) return;
            // check bypass
            if (((BukkitServerPlayer) user).hasPermission(FontManager.BYPASS_SIGN)) {
                return;
            }
            String[] lines = (String[]) Reflections.field$ServerboundSignUpdatePacket$lines.get(packet);
            FontManager manager = CraftEngine.instance().imageManager();
            if (!manager.isDefaultFontInUse()) return;
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];
                if (line != null && !line.isEmpty()) {
                    IllegalCharacterProcessResult result = manager.processIllegalCharacters(line);
                    if (result.has()) {
                        lines[i] = result.text();
                    }
                }
            }
        } catch (Exception e) {
            CraftEngine.instance().logger().warn("Failed to handle ServerboundSignUpdatePacket", e);
        }
    };

    // we handle it on packet level to prevent it from being captured by plugins
    @SuppressWarnings("unchecked")
    public static final TriConsumer<NetWorkUser, NMSPacketEvent, Object> EDIT_BOOK = (user, event, packet) -> {
        try {
            if (!Config.filterBook()) return;
            FontManager manager = CraftEngine.instance().imageManager();
            if (!manager.isDefaultFontInUse()) return;
            // check bypass
            if (((BukkitServerPlayer) user).hasPermission(FontManager.BYPASS_BOOK)) {
                return;
            }

            boolean changed = false;

            List<String> pages = (List<String>) Reflections.field$ServerboundEditBookPacket$pages.get(packet);
            List<String> newPages = new ArrayList<>(pages.size());
            Optional<String> title = (Optional<String>) Reflections.field$ServerboundEditBookPacket$title.get(packet);
            Optional<String> newTitle;

            if (title.isPresent()) {
                String titleStr = title.get();
                Pair<Boolean, String> result = processClientString(titleStr, manager);
                newTitle = Optional.of(result.right());
                if (result.left()) {
                    changed = true;
                }
            } else {
                newTitle = Optional.empty();
            }

            for (String page : pages) {
                Pair<Boolean, String> result = processClientString(page, manager);
                newPages.add(result.right());
                if (result.left()) {
                    changed = true;
                }
            }

            if (changed) {
                Object newPacket = Reflections.constructor$ServerboundEditBookPacket.newInstance(
                        Reflections.field$ServerboundEditBookPacket$slot.get(packet),
                        newPages,
                        newTitle
                );
                event.setNewPacket(newPacket);
            }
        } catch (Exception e) {
            CraftEngine.instance().logger().warn("Failed to handle ServerboundEditBookPacket", e);
        }
    };

    private static Pair<Boolean, String> processClientString(String original, FontManager manager) {
        if (original.isEmpty()) {
            return Pair.of(false, original);
        }
        int[] codepoints = CharacterUtils.charsToCodePoints(original.toCharArray());
        int[] newCodepoints = new int[codepoints.length];
        boolean hasIllegal = false;
        for (int i = 0; i < codepoints.length; i++) {
            int codepoint = codepoints[i];
            if (manager.isIllegalCodepoint(codepoint)) {
                newCodepoints[i] = '*';
                hasIllegal = true;
            } else {
                newCodepoints[i] = codepoint;
            }
        }
        return hasIllegal ? Pair.of(true, new String(newCodepoints, 0, newCodepoints.length)) : Pair.of(false, original);
    }

    public static final TriConsumer<NetWorkUser, NMSPacketEvent, Object> CUSTOM_PAYLOAD = (user, event, packet) -> {
        try {
            if (!VersionHelper.isVersionNewerThan1_20_5()) return;
            Object payload = Reflections.field$ServerboundCustomPayloadPacket$payload.get(packet);
            if (payload.getClass().equals(Reflections.clazz$DiscardedPayload)) {
                Object type = Reflections.method$CustomPacketPayload$type.invoke(payload);
                Object id = Reflections.method$CustomPacketPayload$Type$id.invoke(type);
                String channel = id.toString();
                if (!channel.equals(NetworkManager.MOD_CHANNEL)) return;
                byte[] data;
                if (Reflections.method$DiscardedPayload$data != null) {
                    ByteBuf buf = (ByteBuf) Reflections.method$DiscardedPayload$data.invoke(payload);
                    data = new byte[buf.readableBytes()];
                    buf.readBytes(data);
                } else {
                    data = (byte[]) Reflections.method$DiscardedPayload$dataByteArray.invoke(payload);
                }
                String decodeData = new String(data, StandardCharsets.UTF_8);
                if (!decodeData.endsWith("init")) return;
                int firstColon = decodeData.indexOf(':');
                if (firstColon == -1) return;
                int secondColon = decodeData.indexOf(':', firstColon + 1);
                if (secondColon == -1) return;
                int clientBlockRegistrySize = Integer.parseInt(decodeData.substring(firstColon + 1, secondColon));
                int serverBlockRegistrySize = RegistryUtils.currentBlockRegistrySize();
                if (clientBlockRegistrySize != serverBlockRegistrySize) {
                    Object kickPacket = Reflections.constructor$ClientboundDisconnectPacket.newInstance(
                            ComponentUtils.adventureToMinecraft(
                                    Component.translatable(
                                            "disconnect.craftengine.block_registry_mismatch",
                                            TranslationArgument.numeric(clientBlockRegistrySize),
                                            TranslationArgument.numeric(serverBlockRegistrySize)
                                    )
                            )
                    );
                    user.nettyChannel().writeAndFlush(kickPacket);
                    user.nettyChannel().disconnect();
                    return;
                }
                user.setClientModState(true);
            }
        } catch (Exception e) {
            CraftEngine.instance().logger().warn("Failed to handle ServerboundCustomPayloadPacket", e);
        }
    };

    @SuppressWarnings("unchecked")
    public static final BiConsumer<NetWorkUser, ByteBufPacketEvent> SET_ENTITY_DATA = (user, event) -> {
        try {
            FriendlyByteBuf buf = event.getBuffer();
            int id = buf.readVarInt();
            Object entityType = user.entityView().get(id);
            if (entityType == Reflections.instance$EntityType$BLOCK_DISPLAY) {
                boolean isChanged = false;
                List<Object> packedItems = FastNMS.INSTANCE.method$ClientboundSetEntityDataPacket$unpack(buf);
                for (int i = 0; i < packedItems.size(); i++) {
                    Object packedItem = packedItems.get(i);
                    int entityDataId = FastNMS.INSTANCE.field$SynchedEntityData$DataValue$id(packedItem);
                    if (entityDataId == EntityDataUtils.BLOCK_STATE_DATA_ID) {
                        Object blockState = FastNMS.INSTANCE.field$SynchedEntityData$DataValue$value(packedItem);
                        int stateId = BlockStateUtils.blockStateToId(blockState);
                        int newStateId;
                        if (!user.clientModEnabled()) {
                            newStateId = remap(stateId);
                        } else {
                            newStateId = remapMOD(stateId);
                        }
                        Object serializer = FastNMS.INSTANCE.field$SynchedEntityData$DataValue$serializer(packedItem);
                        packedItems.set(i, FastNMS.INSTANCE.constructor$SynchedEntityData$DataValue(
                                entityDataId, serializer, BlockStateUtils.idToBlockState(newStateId)
                        ));
                        isChanged = true;
                    } else if (Config.interceptEntityName() && entityDataId == EntityDataUtils.CUSTOM_NAME_DATA_ID) {
                        Optional<Object> optionalTextComponent = (Optional<Object>) FastNMS.INSTANCE.field$SynchedEntityData$DataValue$value(packedItem);
                        if (optionalTextComponent.isPresent()) {
                            Object textComponent = optionalTextComponent.get();
                            String json = ComponentUtils.minecraftToJson(textComponent);
                            Map<String, Component> tokens = CraftEngine.instance().imageManager().matchTags(json);
                            if (!tokens.isEmpty()) {
                                Component component = AdventureHelper.jsonToComponent(json);
                                for (Map.Entry<String, Component> token : tokens.entrySet()) {
                                    component = component.replaceText(b -> b.matchLiteral(token.getKey()).replacement(token.getValue()));
                                }
                                Object serializer = FastNMS.INSTANCE.field$SynchedEntityData$DataValue$serializer(packedItem);
                                packedItems.set(i, FastNMS.INSTANCE.constructor$SynchedEntityData$DataValue(
                                        entityDataId, serializer, Optional.of(ComponentUtils.adventureToMinecraft(component))
                                ));
                                isChanged = true;
                            }
                        }
                    }
                }
                if (isChanged) {
                    event.setChanged(true);
                    buf.clear();
                    buf.writeVarInt(event.packetID());
                    buf.writeVarInt(id);
                    FastNMS.INSTANCE.method$ClientboundSetEntityDataPacket$pack(packedItems, buf);
                }
            } else if (entityType == Reflections.instance$EntityType$TEXT_DISPLAY) {
                if (Config.interceptTextDisplay()) {
                    boolean isChanged = false;
                    List<Object> packedItems = FastNMS.INSTANCE.method$ClientboundSetEntityDataPacket$unpack(buf);
                    for (int i = 0; i < packedItems.size(); i++) {
                        Object packedItem = packedItems.get(i);
                        int entityDataId = FastNMS.INSTANCE.field$SynchedEntityData$DataValue$id(packedItem);
                        if (entityDataId == EntityDataUtils.TEXT_DATA_ID) {
                            Object textComponent = FastNMS.INSTANCE.field$SynchedEntityData$DataValue$value(packedItem);
                            if (textComponent == Reflections.instance$Component$empty) break;
                            String json = ComponentUtils.minecraftToJson(textComponent);
                            Map<String, Component> tokens = CraftEngine.instance().imageManager().matchTags(json);
                            if (!tokens.isEmpty()) {
                                Component component = AdventureHelper.jsonToComponent(json);
                                for (Map.Entry<String, Component> token : tokens.entrySet()) {
                                    component = component.replaceText(b -> b.matchLiteral(token.getKey()).replacement(token.getValue()));
                                }
                                Object serializer = FastNMS.INSTANCE.field$SynchedEntityData$DataValue$serializer(packedItem);
                                packedItems.set(i, FastNMS.INSTANCE.constructor$SynchedEntityData$DataValue(entityDataId, serializer, ComponentUtils.adventureToMinecraft(component)));
                                isChanged = true;
                                break;
                            }
                        }
                    }
                    if (isChanged) {
                        event.setChanged(true);
                        buf.clear();
                        buf.writeVarInt(event.packetID());
                        buf.writeVarInt(id);
                        FastNMS.INSTANCE.method$ClientboundSetEntityDataPacket$pack(packedItems, buf);
                    }
                }
            } else if (entityType == Reflections.instance$EntityType$ARMOR_STAND) {
                if (Config.interceptArmorStand()) {
                    boolean isChanged = false;
                    List<Object> packedItems = FastNMS.INSTANCE.method$ClientboundSetEntityDataPacket$unpack(buf);
                    for (int i = 0; i < packedItems.size(); i++) {
                        Object packedItem = packedItems.get(i);
                        int entityDataId = FastNMS.INSTANCE.field$SynchedEntityData$DataValue$id(packedItem);
                        if (entityDataId == EntityDataUtils.CUSTOM_NAME_DATA_ID) {
                            Optional<Object> optionalTextComponent = (Optional<Object>) FastNMS.INSTANCE.field$SynchedEntityData$DataValue$value(packedItem);
                            if (optionalTextComponent.isPresent()) {
                                Object textComponent = optionalTextComponent.get();
                                String json = ComponentUtils.minecraftToJson(textComponent);
                                Map<String, Component> tokens = CraftEngine.instance().imageManager().matchTags(json);
                                if (!tokens.isEmpty()) {
                                    Component component = AdventureHelper.jsonToComponent(json);
                                    for (Map.Entry<String, Component> token : tokens.entrySet()) {
                                        component = component.replaceText(b -> b.matchLiteral(token.getKey()).replacement(token.getValue()));
                                    }
                                    Object serializer = FastNMS.INSTANCE.field$SynchedEntityData$DataValue$serializer(packedItem);
                                    packedItems.set(i, FastNMS.INSTANCE.constructor$SynchedEntityData$DataValue(entityDataId, serializer, Optional.of(ComponentUtils.adventureToMinecraft(component))));
                                    isChanged = true;
                                    break;
                                }
                            }
                        }
                    }
                    if (isChanged) {
                        event.setChanged(true);
                        buf.clear();
                        buf.writeVarInt(event.packetID());
                        buf.writeVarInt(id);
                        FastNMS.INSTANCE.method$ClientboundSetEntityDataPacket$pack(packedItems, buf);
                    }
                }
            } else if (Config.interceptEntityName()) {
                boolean isChanged = false;
                List<Object> packedItems = FastNMS.INSTANCE.method$ClientboundSetEntityDataPacket$unpack(buf);
                for (int i = 0; i < packedItems.size(); i++) {
                    Object packedItem = packedItems.get(i);
                    int entityDataId = FastNMS.INSTANCE.field$SynchedEntityData$DataValue$id(packedItem);
                    if (entityDataId == EntityDataUtils.CUSTOM_NAME_DATA_ID) {
                        Optional<Object> optionalTextComponent = (Optional<Object>) FastNMS.INSTANCE.field$SynchedEntityData$DataValue$value(packedItem);
                        if (optionalTextComponent.isPresent()) {
                            Object textComponent = optionalTextComponent.get();
                            String json = ComponentUtils.minecraftToJson(textComponent);
                            Map<String, Component> tokens = CraftEngine.instance().imageManager().matchTags(json);
                            if (!tokens.isEmpty()) {
                                Component component = AdventureHelper.jsonToComponent(json);
                                for (Map.Entry<String, Component> token : tokens.entrySet()) {
                                    component = component.replaceText(b -> b.matchLiteral(token.getKey()).replacement(token.getValue()));
                                }
                                Object serializer = FastNMS.INSTANCE.field$SynchedEntityData$DataValue$serializer(packedItem);
                                packedItems.set(i, FastNMS.INSTANCE.constructor$SynchedEntityData$DataValue(entityDataId, serializer, Optional.of(ComponentUtils.adventureToMinecraft(component))));
                                isChanged = true;
                                break;
                            }
                        }
                    }
                }
                if (isChanged) {
                    event.setChanged(true);
                    buf.clear();
                    buf.writeVarInt(event.packetID());
                    buf.writeVarInt(id);
                    FastNMS.INSTANCE.method$ClientboundSetEntityDataPacket$pack(packedItems, buf);
                }
            }
        } catch (Exception e) {
            CraftEngine.instance().logger().warn("Failed to handle ClientboundSetEntityDataPacket", e);
        }
    };
}
