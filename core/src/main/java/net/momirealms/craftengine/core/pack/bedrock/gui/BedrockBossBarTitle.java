package net.momirealms.craftengine.core.pack.bedrock.gui;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.bedrock.text.BossBarStyle;
import net.momirealms.craftengine.bedrock.text.BossBarTitleCodec;

import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import static net.momirealms.craftengine.core.pack.bedrock.gui.BedrockTitleLayers.*;
import static net.momirealms.craftengine.core.util.GsonHelper.createArray;
import static net.momirealms.craftengine.core.util.GsonHelper.createObject;

final class BedrockBossBarTitle {
    private static final String HEADER = "ce_boss_header";
    private static final String ORIGIN = "ce_boss_origin";
    private static final String TEMPLATE = "ce_boss_name_";

    private BedrockBossBarTitle() {
    }

    static JsonObject panel(JsonObject hud, List<Integer> offsets) {
        int entryBytes = BossBarTitleCodec.entryBytes(offsets.size() + 1);
        JsonObject templates = layerTemplates(offsets, entryBytes);
        for (Map.Entry<String, JsonElement> template : templates.entrySet()) {
            hud.add(TEMPLATE + template.getKey(), template.getValue());
        }

        int compactEntryBytes = entryBytes - BossBarTitleCodec.POSITION_BYTES + BossBarTitleCodec.COMPACT_POSITION_BYTES;
        int maxLayers = (BossBarTitleCodec.MAX_NAME - 2 - BossBarTitleCodec.MARKER.length() - BossBarTitleCodec.COUNT_BYTES) / (compactEntryBytes + 1);
        JsonArray siblings = new JsonArray(1 + maxLayers * templates.size());
        JsonObject header = topPanel(0, 0);
        header.add("bindings", headerBindings(entryBytes));
        siblings.add(createObject(HEADER, header));
        // 每一行共用一个 1px 原点。逐层插入数据控件和标签，重叠文字按原顺序绘制。
        for (int index = 0; index < maxLayers; index++) {
            JsonObject data = new JsonObject();
            data.addProperty("$ce_index", index);
            data.addProperty("$ce_has_previous", index == 0 ? 0 : 1);
            JsonObject face = new JsonObject();
            face.addProperty("$ce_boss_data", LAYER_DATA + "_" + index);
            for (String name : templates.keySet()) {
                siblings.add(createObject(name + "_" + index + "@hud." + TEMPLATE + name, name.equals(LAYER_DATA) ? data : face));
            }
        }
        JsonObject origin = topPanel(1, 1);
        origin.add("controls", siblings);
        // 原点从本行的表头子控件取可见性，原生标题再从同级原点取反。
        JsonArray mode = new JsonArray(1);
        mode.add(fromSibling(HEADER, "#ce_encoded", "#visible"));
        origin.add("bindings", mode);

        JsonObject nativeName = new JsonObject();
        JsonArray nativeVisibility = new JsonArray(1);
        nativeVisibility.add(fromSibling(ORIGIN, "(not #visible)", "#visible"));
        nativeName.add("bindings", nativeVisibility);
        JsonArray controls = new JsonArray(2);
        controls.add(createObject(ORIGIN, origin));
        controls.add(createObject("native_name@hud.boss_name_panel", nativeName));
        JsonObject panel = new JsonObject();
        panel.addProperty("type", "panel");
        panel.add("controls", controls);
        return panel;
    }

    private static JsonObject layerTemplates(List<Integer> offsets, int entryBytes) {
        int baselineCount = offsets.size() + 1;
        JsonObject data = topPanel(0, 0);
        data.addProperty("$ce_half", 0.5);
        JsonArray bindings = new JsonArray();
        bindings.add(fromSibling(HEADER, "#bossName", "#bossName"));
        bindings.add(fromSibling(HEADER, "#ce_entry_bytes", "#ce_entry_bytes"));
        bindings.add(fromSibling(HEADER, "#ce_bias", "#ce_bias"));
        bindings.add(fromSibling(HEADER, "#ce_body", "#ce_body"));
        bindings.add(fromSibling(HEADER, "(#ce_table + ($ce_index * #ce_entry_bytes))", "#ce_entry"));
        bindings.add(fromSibling(HEADER, "(#ce_count > $ce_index)", "#ce_positioned"));
        int trailerBytes = entryBytes - BossBarTitleCodec.POSITION_BYTES;
        bind(bindings, "(#ce_entry + #ce_entry_bytes - " + trailerBytes + ")", "#ce_middle");
        bind(bindings, "(" + slice("#ce_entry", "#ce_middle") + " + 0)", "#ce_x");
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
        bind(bindings, "(#ce_end_start - ($ce_has_previous * #ce_entry_bytes))", "#ce_previous_start");
        bind(bindings, "(#ce_previous_start + 3)", "#ce_previous_end");
        bind(bindings, "(" + slice("#ce_previous_start", "#ce_previous_end") + " + 0)", "#ce_previous_value");
        bind(bindings, "($ce_has_previous * #ce_previous_value)", "#ce_previous_code");
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

        return BedrockTitleLayers.faceTemplates(data, label, !offsets.isEmpty(), "$ce_boss_data");
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

    private static JsonArray headerBindings(int entryBytes) {
        JsonArray bindings = new JsonArray();
        bindings.add(BedrockBossBars.collection("#bossName"));
        int plain = BossBarTitleCodec.PLAIN_PREFIX_BYTES;
        int extra = BossBarTitleCodec.STYLE_PREFIX_BYTES - plain;
        bind(bindings, "(" + plain + " + (" + BossBarStyle.titleMatches(BossBarStyle.PREFIX) + " * " + extra + "))", "#ce_prefix");
        bind(bindings, "(#ce_prefix + " + BossBarTitleCodec.MARKER_BYTES + ")", "#ce_count_start");
        bind(bindings, slice("#ce_prefix", "#ce_count_start"), "#ce_marker");
        bind(bindings, "(#ce_marker = '" + BossBarTitleCodec.COMPACT_MARKER + "')", "#ce_compact");
        bind(bindings, "(#ce_compact or (#ce_marker = '" + BossBarTitleCodec.MARKER + "'))", "#ce_encoded");
        bind(bindings, "(#ce_count_start + " + BossBarTitleCodec.COUNT_BYTES + ")", "#ce_table");
        bind(bindings, "(" + slice("#ce_count_start", "#ce_table") + " + 0)", "#ce_count");
        int widthDifference = BossBarTitleCodec.POSITION_BYTES - BossBarTitleCodec.COMPACT_POSITION_BYTES;
        int biasDifference = BossBarTitleCodec.POSITION_BIAS - BossBarTitleCodec.COMPACT_POSITION_BIAS;
        bind(bindings, "(" + entryBytes + " - (#ce_compact * " + widthDifference + "))", "#ce_entry_bytes");
        bind(bindings, "(" + BossBarTitleCodec.POSITION_BIAS + " - (#ce_compact * " + biasDifference + "))", "#ce_bias");
        bind(bindings, "(#ce_table + (#ce_count * #ce_entry_bytes))", "#ce_body");
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
