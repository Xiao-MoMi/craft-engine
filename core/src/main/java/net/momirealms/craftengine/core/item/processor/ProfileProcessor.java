package net.momirealms.craftengine.core.item.processor;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.component.DataComponentKeys;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.plugin.context.text.TextProvider;
import net.momirealms.craftengine.core.plugin.context.text.TextProviders;
import net.momirealms.craftengine.core.util.Base64Utils;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.VersionHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public final class ProfileProcessor implements SimpleNetworkItemProcessor {
    public static final ItemProcessorFactory<ProfileProcessor> FACTORY = new Factory();
    private static final Object[] NBT_PATH = new Object[] {"SkullOwner"};
    private final @Nullable TextProvider profileName;
    private final @Nullable String base64Data;
    private final @Nullable Key texture;

    public ProfileProcessor(@Nullable TextProvider profileName,
                            @Nullable String base64Data,
                            @Nullable Key texture) {
        this.profileName = profileName;
        this.base64Data = base64Data;
        this.texture = texture;
    }

    @Override
    public Item apply(Item item, ItemBuildContext context) {
        if (this.profileName != null) {
            String resultString = this.profileName.get(context);
            if (VersionHelper.isOrAbove1_20_5) {
                item.setJavaComponent(DataComponentKeys.PROFILE, resultString);
            } else {
                item.setTag(resultString, "SkullOwner");
            }
        } else if (this.base64Data != null) {
            item.skull(this.base64Data);
        } else if (VersionHelper.isOrAbove1_20_5 && this.texture != null) {
            item.setJavaComponent(DataComponentKeys.PROFILE, Map.of("texture", this.texture.asString()));
        }
        return item;
    }

    private static class Factory implements ItemProcessorFactory<ProfileProcessor> {

        @Override
        public ProfileProcessor create(ConfigValue value) {
            if (value.is(Map.class)) {
                ConfigSection section = value.getAsSection();
                String base64Data = section.getString("base64");
                if (base64Data == null && section.containsKey("url")) {
                    base64Data = Base64Utils.encode("{\"textures\":{\"SKIN\":{\"url\":\"" + section.getString("url") + "\"}}}");
                }
                if (base64Data != null) {
                    return new ProfileProcessor(null, base64Data, null);
                }
                if (VersionHelper.isOrAbove1_20_5) {
                    Key texture = section.getAssetPath("texture");
                    if (texture != null) {
                        return new ProfileProcessor(null, null, texture);
                    }
                }
                return new ProfileProcessor(TextProviders.fromString(section.getString("name", "<arg:player.name>")), null, null);
            } else {
                String guess = value.getAsString();
                String base64Data = null;
                if (guess.startsWith("http://") || guess.startsWith("https://")) {
                    base64Data = Base64Utils.encode("{\"textures\":{\"SKIN\":{\"url\":\"" + guess + "\"}}}");
                } else if (guess.length() > 16 && guess.matches("^[-A-Za-z0-9+/]*={0,3}$")) {
                    base64Data = guess;
                }
                if (base64Data != null) {
                    return new ProfileProcessor(null, base64Data, null);
                } else if (VersionHelper.isOrAbove1_20_5 && (guess.contains(":") || guess.contains("/")) && !guess.contains("<") && !guess.contains(">")) {
                    return new ProfileProcessor(null, null, Key.of(guess));
                } else {
                    return new ProfileProcessor(TextProviders.fromString(guess), null, null);
                }
            }
        }
    }

    @Override
    public String nbtPathString(Item item, ItemBuildContext context) {
        return "SkullOwner";
    }

    @Override
    public @NotNull Object[] nbtPath(Item item, ItemBuildContext context) {
        return NBT_PATH;
    }

    @Override
    public @NotNull Key componentType(Item item, ItemBuildContext context) {
        return DataComponentKeys.PROFILE;
    }
}
