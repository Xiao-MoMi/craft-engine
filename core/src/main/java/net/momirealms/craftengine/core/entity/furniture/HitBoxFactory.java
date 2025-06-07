package net.momirealms.craftengine.core.entity.furniture;

import net.momirealms.craftengine.core.util.MiscUtils;

import java.util.List;
import java.util.Map;

public interface HitBoxFactory {

    HitBox create(Map<String, Object> arguments);

    @SuppressWarnings("unchecked")
    static Seat[] getSeats(Map<String, Object> arguments) {
        List<String> seats = (List<String>) arguments.getOrDefault("seats", List.of());
        return seats.stream()
                .map(SeatType::fromString)
                .toArray(Seat[]::new);
    }
}
