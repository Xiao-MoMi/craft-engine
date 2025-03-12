package net.momirealms.craftengine.core.plugin.dependency;

import com.google.gson.JsonElement;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DependencyRegistry {
    private static final Set<String> DO_NOT_AUTO_LOAD = Stream.of(
            Dependencies.ASM, Dependencies.ASM_COMMONS, Dependencies.JAR_RELOCATOR, Dependencies.ZSTD
    ).map(Dependency::id).collect(Collectors.toSet());

    private static final String GROUP_ID = "net.momirealms";

    public boolean shouldAutoLoad(Dependency dependency) {
        return !DO_NOT_AUTO_LOAD.contains(dependency.id());
    }

    @SuppressWarnings("ConstantConditions")
    public static boolean isGsonRelocated() {
        return JsonElement.class.getName().startsWith(GROUP_ID);
    }

    private static boolean classExists(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private static boolean slf4jPresent() {
        return classExists("org.slf4j.Logger") && classExists("org.slf4j.LoggerFactory");
    }
}
