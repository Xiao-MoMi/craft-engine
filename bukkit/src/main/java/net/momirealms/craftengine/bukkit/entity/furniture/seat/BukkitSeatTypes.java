package net.momirealms.craftengine.bukkit.entity.furniture.seat;

import net.momirealms.craftengine.core.entity.furniture.SeatType;

public class BukkitSeatTypes extends SeatType {

	public static void init() {
		register(SIT, SitSeat.FACTORY);
		register(LAY, LaySeat.FACTORY);
		register(CRAWL, CrawlSeat.FACTORY);
	}
}
