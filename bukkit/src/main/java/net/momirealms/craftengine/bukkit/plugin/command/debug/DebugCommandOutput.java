package net.momirealms.craftengine.bukkit.plugin.command.debug;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

/**
 * Shared presentation helpers for developer-facing command output.
 */
public final class DebugCommandOutput {

    private static final TextColor BRAND_START = TextColor.color(0xF4A460);
    private static final TextColor BRAND_END = TextColor.color(0xFFD700);
    private static final TextColor TEXT = TextColor.color(0xF5F5F5);
    private static final TextColor MUTED = TextColor.color(0xA0A0A0);
    private static final TextColor TRACK = TextColor.color(0x555555);
    private static final TextColor SUCCESS = TextColor.color(0x6FCF97);
    private static final TextColor ERROR = TextColor.color(0xFF5555);
    private static final TextColor WARNING = TextColor.color(0xFFB347);
    private static final Component PREFIX = Component.text("» ", TRACK);
    private static final Component COPY_HINT = Component.translatable("chat.copy.click", TEXT);

    private DebugCommandOutput() {
    }

    public static Component title(String title) {
        return PREFIX
                .append(Component.text("Debug", BRAND_END).decorate(TextDecoration.BOLD))
                .append(Component.text(" • ", TRACK))
                .append(Component.text(title, TEXT).decorate(TextDecoration.BOLD));
    }

    public static Component section(String label) {
        return section(1, label);
    }

    public static Component section(int depth, String label) {
        return Component.text("  ".repeat(Math.max(0, depth)) + "› ", TRACK)
                .append(Component.text(label, BRAND_START));
    }

    public static Component value(String label, Object value) {
        return value(1, label, String.valueOf(value));
    }

    public static Component value(String label, Component value) {
        return value(1, label, value);
    }

    public static Component value(int depth, String label, Object value) {
        return value(depth, label, copyable(String.valueOf(value)));
    }

    public static Component value(int depth, String label, Component value) {
        return Component.text("  ".repeat(Math.max(0, depth)), TRACK)
                .append(Component.text(label, MUTED))
                .append(Component.text(": ", TRACK))
                .append(value);
    }

    public static Component status(String label, boolean enabled) {
        return value(label, booleanValue(enabled));
    }

    public static Component booleanValue(boolean value) {
        return Component.text(Boolean.toString(value), value ? SUCCESS : ERROR);
    }

    public static Component copyable(String value) {
        return copyable(value, value);
    }

    public static Component copyable(String display, String value) {
        return Component.text(display, BRAND_END)
                .hoverEvent(HoverEvent.showText(COPY_HINT))
                .clickEvent(ClickEvent.copyToClipboard(value));
    }

    public static Component listItem(String value) {
        return listItem(1, copyable(value));
    }

    public static Component listItem(int depth, String value) {
        return listItem(depth, copyable(value));
    }

    public static Component listItem(int depth, Component value) {
        return Component.text("  ".repeat(Math.max(0, depth)) + "• ", TRACK)
                .append(value);
    }

    public static Component empty(int depth) {
        return Component.text("  ".repeat(Math.max(0, depth)) + "— none —", MUTED)
                .decorate(TextDecoration.ITALIC);
    }

    public static Component success(String message) {
        return notice("✓", SUCCESS, message);
    }

    public static Component error(String message) {
        return notice("✕", ERROR, message);
    }

    public static Component warning(String message) {
        return notice("!", WARNING, message);
    }

    public static Component info(String message) {
        return notice("•", BRAND_START, message);
    }

    public static Component stateLegend() {
        return Component.text("  Legend: ", MUTED)
                .append(legendEntry(NamedTextColor.GREEN, "Available"))
                .append(Component.text("  ", TRACK))
                .append(legendEntry(NamedTextColor.YELLOW, "Auto"))
                .append(Component.text("  ", TRACK))
                .append(legendEntry(NamedTextColor.RED, "Forced"))
                .append(Component.text("  ", TRACK))
                .append(legendEntry(NamedTextColor.GRAY, "Inactive"));
    }

    static TextColor accentColor() {
        return BRAND_END;
    }

    static TextColor textColor() {
        return TEXT;
    }

    private static Component notice(String symbol, TextColor color, String message) {
        return PREFIX.append(Component.text(symbol + " ", color).decorate(TextDecoration.BOLD))
                .append(Component.text(message, TEXT));
    }

    private static Component legendEntry(TextColor color, String label) {
        return Component.text("|", color)
                .append(Component.text(" " + label, MUTED));
    }
}
