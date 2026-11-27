import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.SharedConstants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.EntityAttachments;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;

public final class GeneratePassengerSeats {
    public static void main(String[] args) throws Exception {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        Map<String, JsonArray> seats = new TreeMap<>();
        Map<String, JsonArray> babies = new TreeMap<>();
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            seats.put(EntityType.getKey(type).toString(), points(type.getDimensions()));
        }
        Class<?> declarations;
        try {
            declarations = Class.forName("net.minecraft.world.entity.EntityTypes");
        } catch (ClassNotFoundException olderVersion) {
            declarations = EntityType.class;
        }
        for (Field field : declarations.getFields()) {
            if (field.getType() != EntityType.class || !(field.getGenericType() instanceof ParameterizedType generic)) {
                continue;
            }
            if (!(generic.getActualTypeArguments()[0] instanceof Class<?> entityClass)) {
                continue;
            }
            EntityDimensions baby = babyDimensions(entityClass);
            if (baby != null) {
                babies.put(EntityType.getKey((EntityType<?>) field.get(null)).toString(), points(baby));
            }
        }
        JsonObject rider = new JsonObject();
        JsonObject adults = new JsonObject();
        JsonObject young = new JsonObject();
        seats.forEach(adults::add);
        babies.forEach(young::add);
        rider.add("default", adults);
        rider.add("baby", young);
        Path directory = Path.of(args[0]);
        Files.createDirectories(directory);
        Path file = directory.resolve("passenger_seats.json");
        Files.writeString(file, rider + "\n");
    }

    private static EntityDimensions babyDimensions(Class<?> type) throws ReflectiveOperationException {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
                if (field.getType() == EntityDimensions.class && (field.getName().equals("BABY_DIMENSIONS") || field.getName().equals("BABY_STANDING_DIMENSIONS"))) {
                    field.setAccessible(true);
                    return (EntityDimensions) field.get(null);
                }
            }
        }
        return null;
    }

    private static JsonArray points(EntityDimensions dimensions) {
        EntityAttachments attachments = dimensions.attachments();
        JsonArray points = new JsonArray();
        for (int index = 0; ; index++) {
            Vec3 point = attachments.getNullable(EntityAttachment.PASSENGER, index, 0);
            if (point == null) {
                return points;
            }
            JsonArray coordinates = new JsonArray();
            coordinates.add((float) point.x);
            coordinates.add((float) point.y);
            coordinates.add((float) point.z);
            points.add(coordinates);
        }
    }
}
