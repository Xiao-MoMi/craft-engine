package net.momirealms.craftengine.core.pack.model.select;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;

public class LocalTimeSelectProperty implements SelectProperty {
    public static final Factory FACTORY = new Factory();
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
        jsonObject.addProperty("pattern", pattern);
        if (locale != null) {
            jsonObject.addProperty("locale", locale);
        }
        if (timeZone != null) {
            jsonObject.addProperty("time_zone", timeZone);
        }
    }

    public static class Factory implements SelectPropertyFactory {

        @Override
        public SelectProperty create(Map<String, Object> arguments) {
            String pattern = Objects.requireNonNull(arguments.get("pattern")).toString();
            String locale = (String) arguments.get("locale");
            String timeZone = (String) arguments.get("time-zone");
            return new LocalTimeSelectProperty(pattern, locale, timeZone);
        }
    }
}
