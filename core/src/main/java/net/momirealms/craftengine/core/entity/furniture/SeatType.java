package net.momirealms.craftengine.core.entity.furniture;

import com.google.common.collect.Lists;
import net.momirealms.craftengine.core.plugin.locale.LocalizedResourceConfigException;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Holder;
import net.momirealms.craftengine.core.registry.Registries;
import net.momirealms.craftengine.core.registry.WritableRegistry;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceKey;

import java.util.List;

public class SeatType {
	public static final Key SIT = Key.of("craftengine:sit");
	public static final Key LAY = Key.of("craftengine:lay");
	public static final Key CRAWL = Key.of("craftengine:crawl");

	public static void register(Key key, SeatFactory factory) {
		Holder.Reference<SeatFactory> holder =((WritableRegistry<SeatFactory>) BuiltInRegistries.SEAT_FACTORY)
				.registerForHolder(new ResourceKey<>(Registries.SEAT_FACTORY.location(), key));
		holder.bindValue(factory);
	}

	public static Seat fromString(String s) {
		List<String> split = Lists.newArrayList(s.split(" "));
		int last = split.size() - 1;
		Key type = SIT;
		SeatFactory factory;
		try {
			Float.parseFloat(split.get(last));
		} catch (NullPointerException | NumberFormatException e) {
			type = Key.withDefaultNamespace(split.get(last), "craftengine");
			split.remove(last);
		}
		factory = BuiltInRegistries.SEAT_FACTORY.getValue(type);
		if (factory == null) {
			throw new LocalizedResourceConfigException("warning.config.furniture.seat.invalid_type", type.toString());
		}
		return factory.create(split);
	}
}
