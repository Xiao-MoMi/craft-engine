package net.momirealms.craftengine.bukkit.font;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.papermc.paper.event.player.AsyncChatCommandDecorateEvent;
import io.papermc.paper.event.player.AsyncChatDecorateEvent;
import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.plugin.reflection.bukkit.CraftBukkitReflections;
import net.momirealms.craftengine.bukkit.plugin.reflection.paper.PaperReflections;
import net.momirealms.craftengine.bukkit.util.ComponentUtils;
import net.momirealms.craftengine.bukkit.util.LegacyInventoryUtils;
import net.momirealms.craftengine.core.font.*;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.util.AdventureHelper;
import net.momirealms.craftengine.core.util.VersionHelper;
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

import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class BukkitFontManager extends AbstractFontManager implements Listener {
    private final BukkitCraftEngine plugin;

    public BukkitFontManager(BukkitCraftEngine plugin) {
        super(plugin);
        this.plugin = plugin;
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

    @Override
    public void delayedLoad() {
        Collection<? extends Player> players = Bukkit.getOnlinePlayers();
        for (Player player : players) {
            removeEmojiSuggestions(player);
        }
        super.delayedLoad();
        for (Player player : players) {
            this.addEmojiSuggestions(player, getEmojiSuggestion(player));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        plugin.scheduler().async().execute(() -> this.addEmojiSuggestions(event.getPlayer(), getEmojiSuggestion(event.getPlayer())));
    }

    @Override
    public void refreshEmojiSuggestions(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) return;
        removeEmojiSuggestions(player);
        addEmojiSuggestions(player, getEmojiSuggestion(player));
    }

    private List<String> getEmojiSuggestion(Player player) {
        List<String> suggestions = new ArrayList<>();
        for (Emoji emoji : super.emojiList) {
            if (emoji.permission() == null || player.hasPermission(Objects.requireNonNull(emoji.permission()))) {
                suggestions.addAll(emoji.keywords());
            }
        }
        return suggestions;
    }

    private void addEmojiSuggestions(Player player, List<String> suggestions) {
        player.addCustomChatCompletions(suggestions);
    }

    private void removeEmojiSuggestions(Player player) {
        if (super.allEmojiSuggestions != null) {
            player.removeCustomChatCompletions(super.allEmojiSuggestions);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    @SuppressWarnings("UnstableApiUsage")
    public void onChat(AsyncChatDecorateEvent event) {
        if (!Config.filterChat()) return;
        this.processChatEvent(event);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    @SuppressWarnings("UnstableApiUsage")
    public void onChatCommand(AsyncChatCommandDecorateEvent event) {
        if (!Config.filterChat()) return;
        this.processChatEvent(event);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (!Config.filterCommand()) return;
        if (!player.hasPermission(FontManager.BYPASS_COMMAND)) {
            IllegalCharacterProcessResult result = processIllegalCharacters(event.getMessage());
            if (result.has()) {
                event.setMessage(result.text());
            }
        }
    }

    @SuppressWarnings("UnstableApiUsage")
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onAnvilRename(PrepareAnvilEvent event) {
        if (!Config.allowEmojiAnvil() || super.emojiKeywordTrie == null) {
            return;
        }
        ItemStack result = event.getResult();
        if (result == null) return;
        Player player;
        try {
            player = (Player) CraftBukkitReflections.method$InventoryView$getPlayer.invoke(VersionHelper.isOrAbove1_21() ? event.getView() : LegacyInventoryUtils.getView(event));
        } catch (ReflectiveOperationException e) {
            this.plugin.logger().warn("Failed to get inventory viewer", e);
            return;
        }

        String renameText;
        if (VersionHelper.isOrAbove1_21_2()) {
            AnvilView anvilView = event.getView();
            renameText = anvilView.getRenameText();
        } else {
            renameText = LegacyInventoryUtils.getRenameText(event.getInventory());
        }

        if (renameText == null || renameText.isEmpty()) return;
        Component itemName = Component.text(renameText);
        EmojiComponentProcessResult replaceProcessResult = replaceComponentEmoji(itemName, plugin.adapt(player), renameText);
        if (replaceProcessResult.changed()) {
            Item<ItemStack> wrapped = this.plugin.itemManager().wrap(result);
            wrapped.customNameJson(AdventureHelper.componentToJson(replaceProcessResult.newText()));
            event.setResult(wrapped.getItem());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onSignChange(SignChangeEvent event) {
        if (!Config.allowEmojiSign()) return;
        Player player = event.getPlayer();
        List<Component> lines = event.lines();
        for (int i = 0; i < lines.size(); i++) {
            JsonElement json = ComponentUtils.paperAdventureToJsonElement(lines.get(i));
            if (json == null) continue;
            Component line = AdventureHelper.jsonElementToComponent(json);
            EmojiComponentProcessResult result = replaceComponentEmoji(line, plugin.adapt(player));
            if (result.changed()) {
                try {
                    PaperReflections.method$SignChangeEvent$line.invoke(event, i, ComponentUtils.jsonElementToPaperAdventure(AdventureHelper.componentToJsonElement(result.newText())));
                } catch (IllegalAccessException | InvocationTargetException e) {
                    plugin.logger().warn("Failed to set sign line", e);
                }
            } else if (AdventureHelper.isPureTextComponent(line)) {
                String plainText = AdventureHelper.plainTextContent(line);
                try {
                    JsonObject jo = new JsonObject();
                    jo.addProperty("text", plainText);
                    PaperReflections.method$SignChangeEvent$line.invoke(event, i, ComponentUtils.jsonElementToPaperAdventure(jo));
                } catch (IllegalAccessException | InvocationTargetException e) {
                    plugin.logger().warn("Failed to reset sign line", e);
                }
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
            EmojiComponentProcessResult result = replaceComponentEmoji(page, plugin.adapt(player));
            if (result.changed()) {
                changed = true;
                try {
                    PaperReflections.method$BookMeta$page.invoke(newBookMeta, i + 1, ComponentUtils.jsonElementToPaperAdventure(AdventureHelper.componentToJsonElement(result.newText())));
                } catch (IllegalAccessException | InvocationTargetException e) {
                    this.plugin.logger().warn("Failed to set book page", e);
                }
            }
        }
        if (changed) {
            event.setNewBookMeta(newBookMeta);
        }
    }

    @SuppressWarnings("UnstableApiUsage")
    private void processChatEvent(AsyncChatDecorateEvent event) {
        Player player = event.player();
        if (player == null) return;
        try {
            Object originalMessage = PaperReflections.field$AsyncChatDecorateEvent$originalMessage.get(event);
            String rawJsonMessage = ComponentUtils.paperAdventureToJson(originalMessage);
            if (Config.allowEmojiChat()) {
                EmojiTextProcessResult processResult = replaceJsonEmoji(rawJsonMessage, this.plugin.adapt(player));
                boolean hasChanged = processResult.replaced();
                if (!player.hasPermission(FontManager.BYPASS_CHAT))  {
                    IllegalCharacterProcessResult result = processIllegalCharacters(processResult.text());
                    if (result.has()) {
                        Object component = ComponentUtils.jsonToPaperAdventure(result.text());
                        PaperReflections.method$AsyncChatDecorateEvent$result.invoke(event, component);
                    } else if (hasChanged) {
                        Object component = ComponentUtils.jsonToPaperAdventure(processResult.text());
                        PaperReflections.method$AsyncChatDecorateEvent$result.invoke(event, component);
                    }
                } else if (hasChanged) {
                    Object component = ComponentUtils.jsonToPaperAdventure(processResult.text());
                    PaperReflections.method$AsyncChatDecorateEvent$result.invoke(event, component);
                }
            } else {
                if (!player.hasPermission(FontManager.BYPASS_CHAT))  {
                    IllegalCharacterProcessResult result = processIllegalCharacters(rawJsonMessage);
                    if (result.has()) {
                        Object component = ComponentUtils.jsonToPaperAdventure(result.text());
                        PaperReflections.method$AsyncChatDecorateEvent$result.invoke(event, component);
                    }
                }
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}
