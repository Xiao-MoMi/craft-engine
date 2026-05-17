package net.momirealms.craftengine.bukkit.font;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.papermc.paper.event.player.AsyncChatCommandDecorateEvent;
import io.papermc.paper.event.player.AsyncChatDecorateEvent;
import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.bukkit.api.BukkitAdaptor;
import net.momirealms.craftengine.bukkit.item.BukkitItem;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.bukkit.util.ComponentUtils;
import net.momirealms.craftengine.bukkit.util.InventoryUtils;
import net.momirealms.craftengine.bukkit.util.ItemStackUtils;
import net.momirealms.craftengine.bukkit.util.LegacyInventoryUtils;
import net.momirealms.craftengine.core.font.*;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.network.IllegalCharacterProcessResult;
import net.momirealms.craftengine.core.util.AdventureHelper;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.proxy.bukkit.event.block.SignChangeEventProxy;
import net.momirealms.craftengine.proxy.bukkit.inventory.meta.BookMetaProxy;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.game.ClientboundCustomChatCompletionsPacketProxy;
import net.momirealms.craftengine.proxy.paper.event.player.AsyncChatDecorateEventProxy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.view.AnvilView;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class BukkitFontManager extends AbstractFontManager implements Listener {
    private static BukkitFontManager instance;
    private final BukkitCraftEngine plugin;

    public BukkitFontManager(BukkitCraftEngine plugin) {
        super(plugin);
        this.plugin = plugin;
        instance = this;
    }

    public static BukkitFontManager instance() {
        return instance;
    }

    @Override
    public void delayedInit() {
        Bukkit.getPluginManager().registerEvents(this, plugin.javaPlugin());
    }

    @Override
    public void disable() {
        super.disable();
        HandlerList.unregisterAll(this);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        this.plugin.scheduler().async().execute(() -> {
            BukkitServerPlayer serverPlayer = BukkitAdaptor.adapt(event.getPlayer());
            if (serverPlayer == null) return;
            refreshEmojiSuggestions(serverPlayer);
        });
    }

    @Override
    public void addEmojiSuggestions(@Nullable net.momirealms.craftengine.core.entity.player.Player player) {
        if (player == null || super.emojiList == null) return;
        Object packet = ClientboundCustomChatCompletionsPacketProxy.INSTANCE.newInstance(
                ClientboundCustomChatCompletionsPacketProxy.ActionProxy.ADD,
                super.getEmojiSuggestions(player)
        );
        player.sendPacket(packet, false);
    }

    @Override
    public void removeEmojiSuggestions(@Nullable net.momirealms.craftengine.core.entity.player.Player player) {
        if (player == null || super.allEmojiSuggestions == null) return;
        Object packet = ClientboundCustomChatCompletionsPacketProxy.INSTANCE.newInstance(
                ClientboundCustomChatCompletionsPacketProxy.ActionProxy.REMOVE,
                super.allEmojiSuggestions
        );
        player.sendPacket(packet, false);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    @SuppressWarnings("UnstableApiUsage")
    public void onChat(AsyncChatDecorateEvent event) {
        if (!Config.filterChat()) return;
        this.processChatEvent(event, EmojiUseCase.CHAT);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    @SuppressWarnings("UnstableApiUsage")
    public void onChatCommand(AsyncChatCommandDecorateEvent event) {
        if (!Config.filterChat()) return;
        this.processChatEvent(event, EmojiUseCase.COMMAND);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (!Config.filterCommand()) return;
        if (!player.hasPermission(FontManager.BYPASS_COMMAND)) {
            IllegalCharacterProcessResult result = this.plugin.networkManager().processIllegalCharacters(event.getMessage());
            if (result.has()) {
                event.setMessage(result.text());
            }
        }
    }

    @SuppressWarnings("UnstableApiUsage")
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onAnvilRename(PrepareAnvilEvent event) {
        if (!Config.allowEmojiAnvil() || super.emojiKeywordTrie == null) {
            return;
        }
        ItemStack result = event.getResult();
        if (ItemStackUtils.isEmpty(result)) return;
        Player player = InventoryUtils.getPlayerFromInventoryEvent(event);
        String renameText;
        if (VersionHelper.isOrAbove1_21_2) {
            AnvilView anvilView = event.getView();
            renameText = anvilView.getRenameText();
        } else {
            renameText = LegacyInventoryUtils.getRenameText(event.getInventory());
        }

        if (renameText == null || renameText.isEmpty()) return;
        Component itemName = Component.text(renameText);
        EmojiComponentProcessResult replaceProcessResult = replaceComponentEmoji(itemName, BukkitAdaptor.adapt(player), renameText, EmojiUseCase.ANVIL);
        if (replaceProcessResult.changed()) {
            BukkitItem wrapped = this.plugin.itemManager().wrap(result);
            wrapped.customNameJson(AdventureHelper.componentToJson(replaceProcessResult.newText()));
            event.setResult(wrapped.getBukkitItem());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onSignChange(SignChangeEvent event) {
        if (!Config.allowEmojiSign()) return;
        Player player = event.getPlayer();
        List<Object> lines = SignChangeEventProxy.INSTANCE.getAdventure$lines(event);
        for (int i = 0; i < lines.size(); i++) {
            JsonElement json = ComponentUtils.paperAdventureToJsonElement(lines.get(i));
            if (json == null) continue;
            Component line = AdventureHelper.jsonElementToComponent(json);
            EmojiComponentProcessResult result = replaceComponentEmoji(line, BukkitAdaptor.adapt(player), EmojiUseCase.SIGN);
            if (result.changed()) {
                SignChangeEventProxy.INSTANCE.line(event, i, ComponentUtils.jsonElementToPaperAdventure(AdventureHelper.componentToJsonElement(result.newText())));
            } else if (AdventureHelper.isPureTextComponent(line)) {
                String plainText = AdventureHelper.plainTextContent(line);
                JsonObject jo = new JsonObject();
                jo.addProperty("text", plainText);
                SignChangeEventProxy.INSTANCE.line(event, i, ComponentUtils.jsonElementToPaperAdventure(jo));
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerEditBook(PlayerEditBookEvent event) {
        if (!event.isSigning()) return;
        if (!Config.allowEmojiBook()) return;
        Player player = event.getPlayer();
        BookMeta newBookMeta = event.getNewBookMeta();
        List<?> pages = newBookMeta.pages();
        boolean changed = false;
        for (int i = 0; i < pages.size(); i++) {
            JsonElement json = ComponentUtils.paperAdventureToJsonElement(pages.get(i));
            Component page = AdventureHelper.jsonElementToComponent(json);
            EmojiComponentProcessResult result = replaceComponentEmoji(page, BukkitAdaptor.adapt(player), EmojiUseCase.BOOK);
            if (result.changed()) {
                changed = true;
                BookMetaProxy.INSTANCE.page(newBookMeta, i + 1, ComponentUtils.jsonElementToPaperAdventure(AdventureHelper.componentToJsonElement(result.newText())));
            }
        }
        if (changed) {
            event.setNewBookMeta(newBookMeta);
        }
    }

    // fixme 这些做法其实是错误的，我们只应该修改字体为minecraft:default的部分
    @SuppressWarnings("UnstableApiUsage")
    private void processChatEvent(AsyncChatDecorateEvent event, EmojiUseCase useCase) {
        Player player = event.player();
        if (player == null) return;
        Object originalMessage = AsyncChatDecorateEventProxy.INSTANCE.getResult(event);
        String rawJsonMessage = ComponentUtils.paperAdventureToJson(originalMessage);
        boolean changed = false;
        if (!player.hasPermission(FontManager.BYPASS_CHAT)) {
            IllegalCharacterProcessResult result = this.plugin.networkManager().processIllegalCharacters(rawJsonMessage);
            if (result.has()) {
                rawJsonMessage = result.text();
                changed = true;
            }
        }
        if (Config.allowEmojiChat()/* && !Config.disableChatReport()*/) {
            EmojiTextProcessResult result = replaceJsonEmoji(rawJsonMessage, BukkitAdaptor.adapt(player), useCase);
            if (result.replaced()) {
                rawJsonMessage = result.text();
                changed = true;
            }
        }
        if (changed) {
            AsyncChatDecorateEventProxy.INSTANCE.setResult(event, ComponentUtils.jsonToPaperAdventure(rawJsonMessage));
        }
    }
}
