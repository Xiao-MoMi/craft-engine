package net.momirealms.craftengine.core.plugin;

import com.google.gson.JsonElement;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.world.Container;
import net.momirealms.craftengine.core.world.World;
import net.momirealms.craftengine.core.world.particle.ParticleType;
import net.momirealms.sparrow.nbt.Tag;

import java.util.UUID;

public interface Platform {

    void dispatchCommand(String command);

    Tag jsonToSparrowNBT(JsonElement json);

    Tag javaToSparrowNBT(Object object);

    World getWorld(String name);

    Player getPlayer(UUID uuid);

    ParticleType getParticleType(Key name);

    int biomeCount();

    Object createContainer(Container container);
}
