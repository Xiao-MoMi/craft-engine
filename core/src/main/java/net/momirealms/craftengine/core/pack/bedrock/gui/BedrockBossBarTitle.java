package net.momirealms.craftengine.core.pack.bedrock.gui;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.bedrock.text.BossBarStyle;
import net.momirealms.craftengine.bedrock.text.BossBarTitleCodec;

import java.util.List;
import java.util.StringJoiner;

import static net.momirealms.craftengine.core.pack.bedrock.gui.BedrockTitleLayers.bind;
import static net.momirealms.craftengine.core.util.GsonHelper.createArray;
import static net.momirealms.craftengine.core.util.GsonHelper.createObject;

final class BedrockBossBarTitle {
    private static final String TEMPLATE = "ce_boss_name_layer";

    private BedrockBossBarTitle() {
    }

    static JsonObject panel(JsonObject hud, List<Integer> offsets) {
        int entryBytes = BossBarTitleCodec.entryBytes(offsets.size() + 1);
        hud.add(TEMPLATE, layer(offsets, entryBytes));
        JsonObject nativeName = new JsonObject();
        nativeName.add("bindings", visibilityBindings("(not #ce_encoded)"));

        JsonObject layers = topPanel(0, 0);
        // BossBar 翻页时可能增加图层。在同一行预建协议允许的槽位，由当前页的层数控制显示。
        int compactEntryBytes = entryBytes - BossBarTitleCodec.POSITION_BYTES + BossBarTitleCodec.COMPACT_POSITION_BYTES;
        int maxLayers = (BossBarTitleCodec.MAX_NAME - 2 - BossBarTitleCodec.MARKER.length() - BossBarTitleCodec.COUNT_BYTES) / (compactEntryBytes + 1);
        JsonArray slots = new JsonArray(maxLayers);
        for (int index = 0; index < maxLayers; index++) {
            JsonObject slot = new JsonObject();
            slot.addProperty("$ce_entry_expression", "(#ce_table + (" + index + " * #ce_entry_bytes))");
            slot.addProperty("$ce_visible_expression", "(#ce_count > " + index + ")");
            slots.add(createObject("layer_" + index + "@hud." + TEMPLATE, slot));
        }
        layers.add("controls", slots);
        layers.add("bindings", visibilityBindings("#ce_encoded"));

        JsonArray controls = new JsonArray();
        controls.add(createObject("native_name@hud.boss_name_panel", nativeName));
        controls.add(createObject("layers", layers));
        JsonObject panel = new JsonObject();
        panel.addProperty("type", "panel");
        panel.add("controls", controls);
        return panel;
    }

    private static JsonArray visibilityBindings(String visible) {
        JsonArray bindings = markerBindings();
        bind(bindings, "(#ce_compact or (#ce_marker = '" + BossBarTitleCodec.MARKER + "'))", "#ce_encoded");
        bind(bindings, visible, "#visible");
        return bindings;
    }

    private static JsonArray entryBindings(int entryBytes) {
        JsonArray bindings = markerBindings();
        bind(bindings, "(#ce_count_start + " + BossBarTitleCodec.COUNT_BYTES + ")", "#ce_table");
        bind(bindings, "(" + slice("#ce_count_start", "#ce_table") + " + 0)", "#ce_count");
        int difference = BossBarTitleCodec.POSITION_BYTES - BossBarTitleCodec.COMPACT_POSITION_BYTES;
        bind(bindings, "(#ce_compact * " + difference + ")", "#ce_saved");
        bind(bindings, "(" + entryBytes + " - #ce_saved)", "#ce_entry_bytes");
        bind(bindings, "$ce_entry_expression", "#ce_entry");
        bind(bindings, "(#ce_entry + " + BossBarTitleCodec.POSITION_BYTES + " - #ce_saved)", "#ce_middle");
        return bindings;
    }

    private static JsonObject layer(List<Integer> offsets, int entryBytes) {
        int baselineCount = offsets.size() + 1;
        JsonObject data = topPanel(0, 0);
        data.addProperty("$ce_half", 0.5);
        JsonArray bindings = entryBindings(entryBytes);
        bind(bindings, "$ce_visible_expression", "#ce_positioned");
        bind(bindings, "(" + slice("#ce_entry", "#ce_middle") + " + 0)", "#ce_x");
        int biasDifference = BossBarTitleCodec.POSITION_BIAS - BossBarTitleCodec.COMPACT_POSITION_BIAS;
        bind(bindings, "(" + BossBarTitleCodec.POSITION_BIAS + " - (#ce_compact * " + biasDifference + "))", "#ce_bias");
        bind(bindings, "($ce_half * (#ce_x - #ce_bias))", "#ce_offset_x");
        bindTag(bindings, baselineCount);
        if (!offsets.isEmpty()) {
            bind(bindings, "(#ce_style - (#ce_unicode * " + baselineCount + "))", "#ce_baseline");
            StringJoiner offsetY = new StringJoiner(" + ");
            for (int i = 0; i < offsets.size(); i++) {
                int id = i + 1;
                String variable = "$ce_y_" + id;
                data.addProperty(variable, -(double) offsets.get(i));
                offsetY.add("(" + variable + " * (#ce_baseline = " + id + "))");
            }
            bind(bindings, offsets.size() == 1 ? offsetY.toString() : "(" + offsetY + ")", "#ce_offset_y");
        }
        bind(bindings, "(" + slice("#ce_end_start", "(#ce_entry + #ce_entry_bytes)") + " + 0)", "#ce_end_code");
        // 第零层从正文开头开始。后续层的起点取前一项的结束位置。
        bind(bindings, "(#ce_entry > #ce_table)", "#ce_has_previous");
        bind(bindings, "(#ce_end_start - (#ce_has_previous * #ce_entry_bytes))", "#ce_previous_start");
        bind(bindings, "(#ce_previous_start + 3)", "#ce_previous_end");
        bind(bindings, "(" + slice("#ce_previous_start", "#ce_previous_end") + " + 0)", "#ce_previous_value");
        bind(bindings, "(#ce_has_previous * #ce_previous_value)", "#ce_previous_code");
        bind(bindings, "(#ce_table + (#ce_count * #ce_entry_bytes))", "#ce_body");
        bind(bindings, "(#ce_body + #ce_previous_code)", "#ce_from");
        bind(bindings, "(#ce_body + #ce_end_code - (#ce_percent * 1))", "#ce_to");
        bind(bindings, "(('§r§f' + " + slice("#ce_from", "#ce_to") + ") + (('%.' + (#ce_percent * 1) + 's') * '%'))", "#ce_ink_text");
        data.add("bindings", bindings);

        JsonObject label = new JsonObject();
        label.addProperty("type", "label");
        label.addProperty("anchor_from", "top_left");
        label.addProperty("anchor_to", "top_left");
        label.addProperty("text_alignment", "left");
        label.addProperty("color", "$boss_name_text_color");
        label.addProperty("shadow", true);
        label.addProperty("enable_profanity_filter", true);
        label.addProperty("localize", false);

        // 零宽容器让各层重叠，1px 的父控件把相对偏移转换为 HUD 像素。
        JsonObject origin = topPanel(1, 1);
        origin.add("controls", BedrockTitleLayers.faceControls(data, label, !offsets.isEmpty()));
        JsonObject item = topPanel(0, 0);
        JsonArray originControls = new JsonArray(1);
        originControls.add(createObject("origin", origin));
        item.add("controls", originControls);
        return item;
    }

    private static void bindTag(JsonArray bindings, int baselineCount) {
        bind(bindings, "(#ce_entry + #ce_entry_bytes - 3)", "#ce_end_start");
        bind(bindings, "(" + slice("#ce_middle", "#ce_end_start") + " + 0)", "#ce_tag");
        int styles = baselineCount * 2;
        bind(bindings, "(#ce_tag > " + (2 * styles - 1) + ")", "#ce_shadow_override");
        bind(bindings, "(#ce_tag - (#ce_shadow_override * " + (2 * styles) + "))", "#ce_text_tag");
        bind(bindings, "(#ce_text_tag > " + (styles - 1) + ")", "#ce_percent");
        bind(bindings, "(#ce_text_tag - (#ce_percent * " + styles + "))", "#ce_style");
        bind(bindings, "(#ce_style > " + (baselineCount - 1) + ")", "#ce_unicode");
    }

    private static JsonArray markerBindings() {
        JsonArray bindings = new JsonArray();
        bindings.add(BedrockBossBars.collection("#bossName"));
        int plain = BossBarTitleCodec.PLAIN_PREFIX_BYTES;
        int extra = BossBarTitleCodec.STYLE_PREFIX_BYTES - plain;
        bind(bindings, "(" + plain + " + (" + BossBarStyle.titleMatches(BossBarStyle.PREFIX) + " * " + extra + "))", "#ce_prefix");
        bind(bindings, "(#ce_prefix + " + BossBarTitleCodec.MARKER_BYTES + ")", "#ce_count_start");
        bind(bindings, slice("#ce_prefix", "#ce_count_start"), "#ce_marker");
        bind(bindings, "(#ce_marker = '" + BossBarTitleCodec.COMPACT_MARKER + "')", "#ce_compact");
        return bindings;
    }

    private static String slice(String from, String to) {
        // 编码名称里没有百分号，给前缀加上 % 可确保字符串减法只移除开头一次。
        return "(('%' + (('%.' + " + to + " + 's') * #bossName)) - ('%' + (('%.' + " + from + " + 's') * #bossName)))";
    }

    private static JsonObject topPanel(int width, int height) {
        JsonObject panel = new JsonObject();
        panel.addProperty("type", "panel");
        panel.addProperty("anchor_from", "top_middle");
        panel.addProperty("anchor_to", "top_middle");
        panel.add("size", createArray(width, height));
        return panel;
    }
}
