package net.momirealms.craftengine.bukkit.compatibility.model.bettermodel;

import net.momirealms.craftengine.core.block.entity.render.element.BlockEntityElementConfig;
import net.momirealms.craftengine.core.block.entity.render.element.BlockEntityElementConfigFactory;
import net.momirealms.craftengine.core.plugin.config.ConfigConstants;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.context.CommonConditions;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.PlayerContext;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.chunk.CEChunk;
import org.joml.Vector3f;

import java.util.List;
import java.util.function.Predicate;

public final class BetterModelBlockEntityElementConfig implements BlockEntityElementConfig<BetterModelBlockEntityElement> {
    public final Vector3f position;
    public final float yaw;
    public final float pitch;
    public final String model;
    public final boolean sightTrace;
    public final Predicate<PlayerContext> predicate;
    public final boolean hasCondition;

    public BetterModelBlockEntityElementConfig(String model,
                                               Vector3f position,
                                               float yaw,
                                               float pitch,
                                               boolean sightTrace,
                                               Predicate<PlayerContext> predicate,
                                               boolean hasCondition) {
        this.pitch = pitch;
        this.position = position;
        this.yaw = yaw;
        this.model = model;
        this.sightTrace = sightTrace;
        this.predicate = predicate;
        this.hasCondition = hasCondition;
    }

    public String model() {
        return this.model;
    }

    public float pitch() {
        return this.pitch;
    }

    public Vector3f position() {
        return this.position;
    }

    public float yaw() {
        return this.yaw;
    }

    public boolean sightTrace() {
        return this.sightTrace;
    }

    @Override
    public BetterModelBlockEntityElement create(CEChunk chunk, BlockPos pos) {
        return new BetterModelBlockEntityElement(this, chunk.world.world, pos);
    }

    @Override
    public Class<BetterModelBlockEntityElement> elementClass() {
        return BetterModelBlockEntityElement.class;
    }

    public static class Factory implements BlockEntityElementConfigFactory<BetterModelBlockEntityElement> {
        private static final String[] SIGHT_TRACE = new String[] {"sight_trace", "sight-trace"};

        @Override
        public BetterModelBlockEntityElementConfig create(ConfigSection section) {
            String model = section.getNonEmptyString("model");
            List<Condition<PlayerContext>> conditions = section.getSectionList("conditions", CommonConditions::fromConfig);
            return new BetterModelBlockEntityElementConfig(
                    model,
                    section.getVector3f("position", ConfigConstants.CENTER_VECTOR3),
                    section.getFloat("yaw"),
                    section.getFloat("pitch"),
                    section.getBoolean(SIGHT_TRACE, true),
                    MiscUtils.allOf(conditions),
                    !conditions.isEmpty()
            );
        }
    }
}
