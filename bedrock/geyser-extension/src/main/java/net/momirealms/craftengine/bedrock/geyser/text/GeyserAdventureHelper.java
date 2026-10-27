package net.momirealms.craftengine.bedrock.geyser.text;

import net.momirealms.craftengine.bedrock.geyser.proxy.text.AdventureComponentProxy;
import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.field.matcher.FieldMatcher;

public final class GeyserAdventureHelper {

    private GeyserAdventureHelper() {
    }

    public static void disableLegacyFormattingWarning() {
        Class<?> implementation = AdventureComponentProxy.INSTANCE.text("").getClass();
        SparrowClass.of(implementation).getDeclaredSparrowField(FieldMatcher.named("WARN_WHEN_LEGACY_FORMATTING_DETECTED")).mh().set(null, false);
    }
}
