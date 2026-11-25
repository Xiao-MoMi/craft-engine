package net.momirealms.craftengine.core.pack.bedrock.gui;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.bedrock.text.BossBarStyle;
import net.momirealms.craftengine.bedrock.text.BossBarTitleCodec;

import static net.momirealms.craftengine.core.pack.bedrock.gui.BedrockTitleLayers.bind;
import static net.momirealms.craftengine.core.util.GsonHelper.createArray;
import static net.momirealms.craftengine.core.util.GsonHelper.createObject;

final class BedrockBossBarTitle {
    private static final String TEMPLATE = "ce_boss_name_layer";

    private BedrockBossBarTitle() {
    }

    static JsonObject panel(JsonObject hud) {
        hud.add(TEMPLATE, layer());
        JsonObject nativeName = new JsonObject();
        JsonArray nativeBindings = source();
        bind(nativeBindings, "(not #ce_encoded)", "#visible");
        nativeName.add("bindings", nativeBindings);

        JsonObject layers = topPanel(0, 0);
        // BossBar 翻页时可能增加图层。在同一行预建协议允许的槽位，由当前页的层数控制显示。
        JsonArray slots = new JsonArray(BossBarTitleCodec.MAX_LAYERS);
        for (int index = 0; index < BossBarTitleCodec.MAX_LAYERS; index++) {
            JsonObject slot = new JsonObject();
            slot.addProperty("$ce_entry_expression", "(#ce_table + " + (index * BossBarTitleCodec.ENTRY_BYTES) + ")");
            slot.addProperty("$ce_visible_expression", "(#ce_count > " + index + ")");
            slots.add(createObject("layer_" + index + "@hud." + TEMPLATE, slot));
        }
        layers.add("controls", slots);
        JsonArray bindings = source();
        bind(bindings, "#ce_encoded", "#visible");
        layers.add("bindings", bindings);

        JsonArray controls = new JsonArray();
        controls.add(createObject("native_name@hud.boss_name_panel", nativeName));
        controls.add(createObject("layers", layers));
        JsonObject panel = new JsonObject();
        panel.addProperty("type", "panel");
        panel.add("controls", controls);
        return panel;
    }

    private static JsonArray entryBindings() {
        JsonArray bindings = source();
        bind(bindings, "$ce_entry_expression", "#ce_entry");
        bind(bindings, "(#ce_entry + " + BossBarTitleCodec.POSITION_BYTES + ")", "#ce_middle");
        return bindings;
    }

    private static JsonObject layer() {
        JsonArray bindings = entryBindings();
        bind(bindings, "(" + slice("#ce_middle", "(#ce_entry + " + BossBarTitleCodec.ENTRY_BYTES + ")") + " + 0)", "#ce_end_code");
        // 第零层从正文开头开始。后续层的起点取前一项的结束位置。
        bind(bindings, "(#ce_entry > #ce_table)", "#ce_has_previous");
        bind(bindings, "(#ce_middle - (#ce_has_previous * " + BossBarTitleCodec.ENTRY_BYTES + "))", "#ce_previous_start");
        bind(bindings, "(#ce_previous_start + " + (BossBarTitleCodec.ENTRY_BYTES - BossBarTitleCodec.POSITION_BYTES) + ")", "#ce_previous_end");
        bind(bindings, "(" + slice("#ce_previous_start", "#ce_previous_end") + " + 0)", "#ce_previous_value");
        bind(bindings, "(#ce_has_previous * #ce_previous_value)", "#ce_previous_code");
        bind(bindings, "(#ce_table + (#ce_count * " + BossBarTitleCodec.ENTRY_BYTES + "))", "#ce_body");
        int flag = BossBarTitleCodec.PERCENT_FLAG;
        bind(bindings, "(#ce_end_code > " + (flag - 1) + ")", "#ce_percent");
        bind(bindings, "(#ce_body + #ce_previous_code - ((#ce_previous_code > " + (flag - 1) + ") * " + flag + "))", "#ce_from");
        bind(bindings, "(#ce_body + #ce_end_code - (#ce_percent * " + (flag + 1) + "))", "#ce_to");
        bind(bindings, "(('§r§f' + " + slice("#ce_from", "#ce_to") + ") + (('%.' + (#ce_percent * 1) + 's') * '%'))", "#text");

        JsonObject label = new JsonObject();
        label.addProperty("type", "label");
        label.addProperty("anchor_from", "top_left");
        label.addProperty("anchor_to", "top_left");
        label.addProperty("text", "#text");
        label.addProperty("text_alignment", "left");
        label.addProperty("color", "$boss_name_text_color");
        label.addProperty("shadow", true);
        label.addProperty("enable_profanity_filter", true);
        label.addProperty("localize", false);
        label.add("bindings", bindings);

        JsonObject position = topPanel(1, 1);
        position.addProperty("anchor_from", "top_left");
        position.addProperty("anchor_to", "top_left");
        position.addProperty("use_anchored_offset", true);
        position.addProperty("$ce_half", 0.5);
        JsonObject properties = new JsonObject();
        properties.addProperty("#anchored_offset_value_x", 1.0);
        properties.addProperty("#anchored_offset_value_y", 0.0);
        position.add("property_bag", properties);
        JsonArray positionBindings = entryBindings();
        bind(positionBindings, "$ce_visible_expression", "#visible");
        bind(positionBindings, "(" + slice("#ce_entry", "#ce_middle") + " + 0)", "#ce_x");
        bind(positionBindings, "($ce_half * (#ce_x - " + BossBarTitleCodec.POSITION_BIAS + "))", "#anchored_offset_value_x");
        position.add("bindings", positionBindings);

        // 零宽容器让各层重叠，1px 的父控件把相对偏移转换为 HUD 像素。
        JsonObject origin = topPanel(1, 1);
        JsonArray labelControls = new JsonArray(1);
        labelControls.add(createObject("text", label));
        position.add("controls", labelControls);
        JsonArray positionControls = new JsonArray(1);
        positionControls.add(createObject("position", position));
        origin.add("controls", positionControls);
        JsonObject item = topPanel(0, 0);
        JsonArray originControls = new JsonArray(1);
        originControls.add(createObject("origin", origin));
        item.add("controls", originControls);
        return item;
    }

    private static JsonArray source() {
        JsonArray bindings = new JsonArray();
        bindings.add(BedrockBossBars.collection("#bossName"));
        int plain = BossBarTitleCodec.PLAIN_PREFIX_BYTES;
        int extra = BossBarTitleCodec.STYLE_PREFIX_BYTES - plain;
        bind(bindings, "(" + plain + " + (" + BossBarStyle.titleMatches(BossBarStyle.PREFIX) + " * " + extra + "))", "#ce_prefix");
        bind(bindings, "(#ce_prefix + " + BossBarTitleCodec.MARKER_BYTES + ")", "#ce_count_start");
        bind(bindings, "(" + slice("#ce_prefix", "#ce_count_start") + " = '" + BossBarTitleCodec.MARKER + "')", "#ce_encoded");
        bind(bindings, "(#ce_count_start + " + BossBarTitleCodec.COUNT_BYTES + ")", "#ce_table");
        bind(bindings, "(" + slice("#ce_count_start", "#ce_table") + " + 0)", "#ce_count");
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
