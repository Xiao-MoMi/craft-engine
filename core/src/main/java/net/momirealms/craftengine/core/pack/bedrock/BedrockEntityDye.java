package net.momirealms.craftengine.core.pack.bedrock;

import net.momirealms.craftengine.core.plugin.config.ConfigKeys;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.util.Color;

import java.util.List;
import java.util.StringJoiner;

public record BedrockEntityDye(int maxLayers, List<Integer> prebakedColors) {
    public static final BedrockEntityDye DEFAULT = new BedrockEntityDye(-1, List.of(0xff8080));
    private static final String[] DYE_MAX_LAYERS = ConfigKeys.of("dye_max_layers");
    private static final String[] PREBAKED_COLORS = ConfigKeys.of("prebaked_colors");

    public BedrockEntityDye {
        if (maxLayers != -1 && maxLayers < 1) {
            throw new IllegalArgumentException("dye-max-layers must be -1 or positive");
        }
        prebakedColors = List.copyOf(prebakedColors);
    }

    public static BedrockEntityDye readDisplay(ConfigSection section, boolean override) {
        int layers = override ? section.getNonNullInt(DYE_MAX_LAYERS) : section.getInt(DYE_MAX_LAYERS, 10);
        return new BedrockEntityDye(layers, List.of());
    }

    public static BedrockEntityDye read(ConfigSection section, List<String> defaultColors) {
        List<Integer> colors = section.getStringList(PREBAKED_COLORS, defaultColors).stream().map(value -> Color.fromHex(value).color() & 0xffffff).distinct().sorted().toList();
        return new BedrockEntityDye(section.getInt(DYE_MAX_LAYERS, -1), colors);
    }

    public static String matches(String property, int rgb) {
        return "(q.property('" + property + "_r') == " + (rgb >>> 16) + " && q.property('" + property + "_gb') == " + (rgb & 65535) + ")";
    }

    public String baseCondition(String property, boolean whiteIsBase) {
        String base = "q.property('" + property + "_r') < 0";
        if (whiteIsBase) {
            base = "(" + base + " || " + matches(property, 0xffffff) + ")";
        }
        if (this.maxLayers == -1) {
            return this.unmatched(property);
        }
        return this.prebakedColors.isEmpty() ? base : base + " && " + this.unmatched(property);
    }

    public String dynamicCondition(String property, boolean whiteIsBase) {
        String dynamic = "q.property('" + property + "_r') >= 0";
        if (whiteIsBase) {
            dynamic += " && !" + matches(property, 0xffffff);
        }
        return this.prebakedColors.isEmpty() ? dynamic : dynamic + " && " + this.unmatched(property);
    }

    private String unmatched(String property) {
        StringJoiner matches = new StringJoiner(" || ");
        for (int rgb : this.prebakedColors) {
            matches.add(matches(property, rgb));
        }
        return this.prebakedColors.isEmpty() ? "" : "!(" + matches + ")";
    }
}
