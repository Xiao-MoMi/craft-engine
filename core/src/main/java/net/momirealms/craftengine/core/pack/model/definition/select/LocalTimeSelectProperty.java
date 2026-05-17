package net.momirealms.craftengine.core.pack.model.definition.select;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class LocalTimeSelectProperty implements SelectProperty {
    public static final SelectPropertyFactory<LocalTimeSelectProperty> FACTORY = new Factory();
    public static final SelectPropertyReader<LocalTimeSelectProperty> READER = new Reader();
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

    public String pattern() {
        return this.pattern;
    }

    public String locale() {
        return this.locale;
    }

    public String timeZone() {
        return this.timeZone;
    }

    @Override
    public void writeProperty(JsonObject model) {
        model.addProperty("property", "local_time");
        model.addProperty("pattern", this.pattern);
        if (this.locale != null) {
            model.addProperty("locale", this.locale);
        }
        if (this.timeZone != null) {
            model.addProperty("time_zone", this.timeZone);
        }
    }

    private static class Factory implements SelectPropertyFactory<LocalTimeSelectProperty> {
        private static final String[] TIME_ZONE = new String[] {"time-zone", "time_zone"};

        @Override
        public LocalTimeSelectProperty create(ConfigSection section) {
            return new LocalTimeSelectProperty(
                    section.getNonNullString("pattern"),
                    section.getString("locale"),
                    section.getString(TIME_ZONE)
            );
        }
    }

    private static class Reader implements SelectPropertyReader<LocalTimeSelectProperty> {
        @Override
        public LocalTimeSelectProperty read(JsonObject json) {
            return new LocalTimeSelectProperty(
                    json.get("pattern").getAsString(),
                    json.has("locale") ? json.get("locale").getAsString() : null,
                    json.has("time_zone") ? json.get("time_zone").getAsString() : null
            );
        }
    }
}
