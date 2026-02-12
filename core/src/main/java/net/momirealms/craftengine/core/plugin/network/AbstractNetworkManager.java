package net.momirealms.craftengine.core.plugin.network;

import net.kyori.adventure.text.minimessage.internal.parser.Token;
import net.kyori.adventure.text.minimessage.internal.parser.TokenParser;
import net.momirealms.craftengine.core.font.BitmapImage;
import net.momirealms.craftengine.core.font.Image;
import net.momirealms.craftengine.core.font.OffsetFont;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.text.component.ComponentProvider;
import net.momirealms.craftengine.core.util.CharacterUtils;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;

import java.util.*;

public abstract class AbstractNetworkManager implements NetworkManager {
    protected Map<String, ComponentProvider> networkTagMapper = new HashMap<>(1024);
    protected final CraftEngine plugin;

    public AbstractNetworkManager(CraftEngine plugin) {
        this.plugin = plugin;
    }

    @Override
    public void unload() {
        this.networkTagMapper.clear();
    }

    @Override
    public void delayedLoad() {
        this.registerImageTags();
        this.registerShiftTags();
        this.registerGlobalTags();
        this.registerL10nTags();
    }

    private static final Set<String> NETWORK_TAGS = MiscUtils.init(new HashSet<>(), it -> {
        it.add("image");
        it.add("l10n");
        it.add("shift");
        it.add("global");
        it.add("papi");
    });

    private static String imageTag(String text) {
        return "<image:" + text + ">";
    }

    private static String globalTag(String text) {
        return "<global:" + text + ">";
    }

    private static String l10nTag(String text) {
        return "<l10n:" + text + ">";
    }

    private void registerL10nTags() {
        for (String key : this.plugin.translationManager().translationKeys()) {
            String l10nTag = l10nTag(key);
            this.networkTagMapper.put(l10nTag, ComponentProvider.l10n(key));
        }
    }

    private void registerGlobalTags() {
        for (Map.Entry<String, String> entry : this.plugin.globalVariableManager().globalVariables().entrySet()) {
            String globalTag = globalTag(entry.getKey());
            this.networkTagMapper.put(globalTag, ComponentProvider.miniMessageOrConstant(entry.getValue()));
        }
    }

    private void registerShiftTags() {
        OffsetFont offsetFont = this.plugin.fontManager().offsetFont();
        if (offsetFont == null) return;
        for (int i = -256; i <= 256; i++) {
            String shiftTag = "<shift:" + i + ">";
            this.networkTagMapper.put(shiftTag, ComponentProvider.constant(offsetFont.createOffset(i)));
        }
    }

    private void registerImageTags() {
        for (Image image : this.plugin.fontManager().loadedImages().values()) {
            Key key = image.id();
            String id = key.toString();
            String simpleImageTag = imageTag(id);
            this.networkTagMapper.put(simpleImageTag, ComponentProvider.constant(image.componentAt(0, 0)));
            String simplerImageTag = imageTag(key.value());
            this.networkTagMapper.put(simplerImageTag, ComponentProvider.constant(image.componentAt(0, 0)));
            if (image instanceof BitmapImage bitmapImage) {
                for (int i = 0; i < bitmapImage.rows(); i++) {
                    String partialArgs = id + ":" + i;
                    this.networkTagMapper.put(imageTag(partialArgs), ComponentProvider.constant(image.componentAt(i, 0)));
                    for (int j = 0; j < bitmapImage.columns(); j++) {
                        String imageArgs = id + ":" + i + ":" + j;
                        String imageTag = imageTag(imageArgs);
                        this.networkTagMapper.put(imageTag, ComponentProvider.constant(image.componentAt(i, j)));
                    }
                }
            }
        }
    }

    @SuppressWarnings("UnstableApiUsage")
    @Override
    public Map<String, ComponentProvider> matchNetworkTags(String text) {
        Map<String, ComponentProvider> tags = new HashMap<>();
        List<Token> root = TokenParser.tokenize(text, true);
        for (final net.kyori.adventure.text.minimessage.internal.parser.Token token : root) {
            switch (token.type()) {
                case TEXT: break;
                case OPEN_TAG:
                case CLOSE_TAG:
                case OPEN_CLOSE_TAG:
                    if (token.childTokens().isEmpty()) {
                        continue;
                    }
                    final String sanitized = TokenParser.TagProvider.sanitizePlaceholderName(token.childTokens().getFirst().get(text).toString());
                    if (NETWORK_TAGS.contains(sanitized)) {
                        String tag = text.substring(token.startIndex(), token.endIndex());
                        tags.computeIfAbsent(tag, k -> {
                            ComponentProvider provider = this.networkTagMapper.get(k);
                            if (provider != null) {
                                return provider;
                            }
                            return ComponentProvider.miniMessage(k);
                        });
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported token type " + token.type());
            }
        }
        return tags;
    }

    @Override
    public IllegalCharacterProcessResult processIllegalCharacters(String raw, char replacement) {
        boolean hasIllegal = false;
        // replace illegal image usage
        Map<String, ComponentProvider> tokens = matchNetworkTags(raw);
        if (!tokens.isEmpty()) {
            for (Map.Entry<String, ComponentProvider> entry : tokens.entrySet()) {
                raw = raw.replace(entry.getKey(), String.valueOf(replacement));
                hasIllegal = true;
            }
        }

        if (this.plugin.fontManager().isDefaultFontInUse()) {
            // replace illegal codepoint
            char[] chars = raw.toCharArray();
            int[] codepoints = CharacterUtils.charsToCodePoints(chars);
            int[] newCodepoints = new int[codepoints.length];

            for (int i = 0; i < codepoints.length; i++) {
                int codepoint = codepoints[i];
                if (!this.plugin.fontManager().isIllegalCodepoint(codepoint)) {
                    newCodepoints[i] = codepoint;
                } else {
                    newCodepoints[i] = replacement;
                    hasIllegal = true;
                }
            }

            if (hasIllegal) {
                return IllegalCharacterProcessResult.has(new String(newCodepoints, 0, newCodepoints.length));
            }
        } else if (hasIllegal) {
            return IllegalCharacterProcessResult.has(raw);
        }
        return IllegalCharacterProcessResult.not();
    }
}
