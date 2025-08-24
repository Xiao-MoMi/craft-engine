package net.momirealms.craftengine.bukkit.util;

import com.google.gson.JsonElement;
import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.reflection.paper.PaperReflections;
import net.momirealms.craftengine.core.util.AdventureHelper;

public final class ComponentUtils {

    private ComponentUtils() {}

    public static Object adventureToMinecraft(Component component) {
        return jsonElementToMinecraft(AdventureHelper.componentToJsonElement(component));
    }

    public static Object adventureToPaperAdventure(Component component) {
        return jsonElementToPaperAdventure(AdventureHelper.componentToJsonElement(component));
    }

    public static Object jsonElementToMinecraft(JsonElement json) {
        return FastNMS.INSTANCE.method$Component$Serializer$fromJson(json);
    }

    public static Object jsonToMinecraft(String json) {
        return FastNMS.INSTANCE.method$Component$Serializer$fromJson(json);
    }

    public static String minecraftToJson(Object component) {
        return FastNMS.INSTANCE.method$Component$Serializer$toJson(component);
    }

    public static String paperAdventureToJson(Object component) {
        return PaperReflections.instance$GsonComponentSerializer$Gson.toJson(component);
    }

    public static Object jsonToPaperAdventure(String json) {
        return PaperReflections.instance$GsonComponentSerializer$Gson.fromJson(json, PaperReflections.clazz$AdventureComponent);
    }

    public static JsonElement paperAdventureToJsonElement(Object component) {
        return PaperReflections.instance$GsonComponentSerializer$Gson.toJsonTree(component);
    }

    public static Object jsonElementToPaperAdventure(JsonElement json) {
        return PaperReflections.instance$GsonComponentSerializer$Gson.fromJson(json, PaperReflections.clazz$AdventureComponent);
    }
}
