package net.momirealms.craftengine.bedrock.geyser;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

final class GeyserLocaleBridge {

    private GeyserLocaleBridge() {
    }

    static void reloadLoaded(Set<String> locales) throws ReflectiveOperationException {
        if (locales.isEmpty()) {
            return;
        }
        Class<?> type = Class.forName("org.geysermc.geyser.text.MinecraftLocale");
        Method isLoaded = type.getMethod("isLocaleLoaded", String.class);
        Method load = type.getDeclaredMethod("loadLocale", String.class);
        load.setAccessible(true);
        Map<?, ?> cache = (Map<?, ?>) type.getField("LOCALE_MAPPINGS").get(null);
        for (String locale : locales) {
            String cacheKey = locale.equals("no_no") ? "nb_no" : locale;
            // Unloaded locales must follow Geyser's normal download-and-load path.
            if ((boolean) isLoaded.invoke(null, cacheKey) && !(boolean) load.invoke(null, locale)) {
                cache.remove(cacheKey);
            }
        }
    }
}
