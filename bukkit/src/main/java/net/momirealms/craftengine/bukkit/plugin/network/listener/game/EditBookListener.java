package net.momirealms.craftengine.bukkit.plugin.network.listener.game;

import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.core.font.FontManager;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.network.NetWorkUser;
import net.momirealms.craftengine.core.plugin.network.event.ByteBufPacketEvent;
import net.momirealms.craftengine.core.plugin.network.listener.ByteBufferPacketListener;
import net.momirealms.craftengine.core.util.CharacterUtils;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.core.util.Pair;

import java.util.List;
import java.util.Optional;

public final class EditBookListener implements ByteBufferPacketListener {
    public static final ByteBufferPacketListener INSTANCE = new EditBookListener();

    private EditBookListener() {}

    @Override
    public void onPacketReceive(NetWorkUser user, ByteBufPacketEvent event) {
        if (!Config.filterBook()) return;
        FontManager manager = CraftEngine.instance().fontManager();
        if (!manager.isDefaultFontInUse()) return;
        // check bypass
        if (((BukkitServerPlayer) user).hasPermission(FontManager.BYPASS_BOOK)) {
            return;
        }

        FriendlyByteBuf buf = event.getBuffer();
        int slot = buf.readVarInt();
        List<String> pages = buf.readStringList();
        Optional<String> title = buf.readOptional(FriendlyByteBuf::readUtf);

        boolean changed = false;

        if (title.isPresent()) {
            String titleStr = title.get();
            Pair<Boolean, String> result = processClientString(titleStr, manager);
            if (result.left()) {
                title = Optional.of(result.right());
                changed = true;
            }
        }

        for (int i = 0; i < pages.size(); i++) {
            String page = pages.get(i);
            Pair<Boolean, String> result = processClientString(page, manager);
            if (result.left()) {
                pages.set(i, result.right());
                changed = true;
            }
        }

        if (changed) {
            event.setChanged(true);
            buf.clear();
            buf.writeVarInt(event.packetID());
            buf.writeVarInt(slot);
            buf.writeStringList(pages);
            buf.writeOptional(title, FriendlyByteBuf::writeUtf);
        }
    }

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
}
