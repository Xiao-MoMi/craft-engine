package net.momirealms.craftengine.bukkit.compatibility.model.bettermodel;

import kr.toxicity.model.api.util.function.BonePredicate;
import net.momirealms.craftengine.core.entity.furniture.Furniture;
import net.momirealms.craftengine.core.entity.furniture.data.FurnitureDataSourceConfig;
import net.momirealms.craftengine.core.entity.furniture.data.SourceItemColorDataSourceConfig;
import net.momirealms.craftengine.core.entity.furniture.element.FurnitureElementConfig;
import net.momirealms.craftengine.core.entity.furniture.element.FurnitureElementConfigFactory;
import net.momirealms.craftengine.core.plugin.config.ConfigConstants;
import net.momirealms.craftengine.core.plugin.config.ConfigKeys;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.plugin.context.CommonConditions;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.PlayerContext;
import net.momirealms.craftengine.core.util.Color;
import net.momirealms.craftengine.core.util.MiscUtils;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public final class BetterModelFurnitureElementConfig implements FurnitureElementConfig<BetterModelFurnitureElement> {
    public static final FurnitureElementConfigFactory<BetterModelFurnitureElement> FACTORY = new Factory();
    public final Vector3f position;
    public final float yaw;
    public final float pitch;
    public final String model;
    public final boolean sightTrace;
    public final List<BetterModelTintLayerConfig> tintLayers;
    public final Predicate<PlayerContext> predicate;
    public final boolean hasCondition;

    private BetterModelFurnitureElementConfig(String model,
                                              Vector3f position,
                                              float yaw,
                                              float pitch,
                                              boolean sightTrace,
                                              List<BetterModelTintLayerConfig> tintLayers,
                                              Predicate<PlayerContext> predicate,
                                              boolean hasCondition) {
        this.pitch = pitch;
        this.position = position;
        this.yaw = yaw;
        this.model = model;
        this.sightTrace = sightTrace;
        this.tintLayers = List.copyOf(tintLayers);
        this.predicate = predicate;
        this.hasCondition = hasCondition;
    }

    @Override
    public BetterModelFurnitureElement create(@NotNull Furniture furniture) {
        return new BetterModelFurnitureElement(furniture, this);
    }

    @Override
    public Class<BetterModelFurnitureElement> elementClass() {
        return BetterModelFurnitureElement.class;
    }

    private static class Factory implements FurnitureElementConfigFactory<BetterModelFurnitureElement> {
        private static final String[] SIGHT_TRACE = ConfigKeys.of("sight_trace");
        private static final String[] TINTS = ConfigKeys.of("tint(s)");
        private static final String[] TINT_SOURCE = ConfigKeys.of("tint_source");
        private static final String[] TINT_BONES = ConfigKeys.of("tint_bone(s)");
        private static final String[] TINT_CHILDREN = ConfigKeys.of("tint_children");
        private static final String[] LAYER_SOURCE = ConfigKeys.of("source", "tint_source");
        private static final String[] LAYER_BONES = ConfigKeys.of("bone(s)", "tint_bone(s)");
        private static final String[] LAYER_CHILDREN = ConfigKeys.of("children", "tint_children");

        @Override
        public FurnitureElementConfig<BetterModelFurnitureElement> create(ConfigSection section) {
            List<Condition<PlayerContext>> conditions = section.getSectionList(ConfigKeys.of("condition(s)"), CommonConditions::fromConfig);
            return new BetterModelFurnitureElementConfig(
                    section.getNonEmptyString("model"),
                    section.getVector3f("position", ConfigConstants.ZERO_VECTOR3),
                    section.getFloat("yaw"),
                    section.getFloat("pitch"),
                    section.getBoolean(SIGHT_TRACE, true),
                    tintLayers(section),
                    MiscUtils.allOf(conditions),
                    !conditions.isEmpty()
            );
        }

        private static List<BetterModelTintLayerConfig> tintLayers(ConfigSection section) {
            if (section.getValue(TINTS) != null) {
                return section.getSectionList(TINTS, Factory::tintLayer);
            }
            FurnitureDataSourceConfig<Color> source = section.getValue(TINT_SOURCE, SourceItemColorDataSourceConfig::fromConfig);
            if (source == null) {
                return List.of();
            }
            return List.of(new BetterModelTintLayerConfig(source, bonePredicate(section, TINT_BONES, TINT_CHILDREN)));
        }

        private static BetterModelTintLayerConfig tintLayer(ConfigSection section) {
            FurnitureDataSourceConfig<Color> source = section.getNonNullValue(
                    LAYER_SOURCE,
                    ConfigConstants.ARGUMENT_ANY,
                    SourceItemColorDataSourceConfig::fromConfig
            );
            return new BetterModelTintLayerConfig(source, bonePredicate(section, LAYER_BONES, LAYER_CHILDREN));
        }

        private static BonePredicate bonePredicate(ConfigSection section, String[] boneKeys, String[] childrenKeys) {
            ConfigValue value = section.getValue(boneKeys);
            if (value == null) {
                return BonePredicate.TRUE;
            }
            Set<String> bones = Set.copyOf(value.getAsStringList());
            if (bones.isEmpty()) {
                return BonePredicate.FALSE;
            }
            BonePredicate.State children = section.getBoolean(childrenKeys, true)
                    ? BonePredicate.State.TRUE
                    : BonePredicate.State.FALSE;
            return BonePredicate.of(children, bone -> bones.contains(bone.name().name()));
        }
    }
}
