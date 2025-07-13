package net.momirealms.craftengine.core.pack.model.select;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class LocalTimeSelectProperty implements SelectProperty {
    public static final Factory FACTORY = new Factory();
    public static final Reader READER = new Reader();
    private final String pattern;
    private final String locale;
    private final String timeZone;

    public LocalTimeSelectProperty(@NotNull String pattern,
                                   @Nullable String locale,
                                   @Nullable String timeZone) {
        this.pattern = pattern;
        this.locale = locale;
        this.timeZone = timeZone;
    }

    @Override
    public Key type() {
        return SelectProperties.LOCAL_TIME;
    }

    @Override
    public void accept(JsonObject jsonObject) {
        jsonObject.addProperty("property", type().toString());
        jsonObject.addProperty("pattern", this.pattern);
        if (this.locale != null) {
            jsonObject.addProperty("locale", this.locale);
        }
        if (this.timeZone != null) {
            jsonObject.addProperty("time_zone", this.timeZone);
        }
    }

    public static class Factory implements SelectPropertyFactory {
        @Override
        public SelectProperty create(Map<String, Object> arguments) {
            String pattern = ResourceConfigUtils.requireNonEmptyStringOrThrow(arguments.get("pattern"), "warning.config.item.model.select.local_time.missing_pattern");
            String locale = (String) arguments.get("locale");
            String timeZone = (String) arguments.get("time-zone");
            return new LocalTimeSelectProperty(pattern, locale, timeZone);
        }
    }

    public static class Reader implements SelectPropertyReader {
        @Override
        public SelectProperty read(JsonObject json) {
            String pattern = json.get("pattern").getAsString();
            String locale = json.has("locale") ? json.get("locale").getAsString() : null;
            String timeZone = json.has("time_zone") ? json.get("time_zone").getAsString() : null;
            return new LocalTimeSelectProperty(pattern, locale, timeZone);
        }
    }
}
