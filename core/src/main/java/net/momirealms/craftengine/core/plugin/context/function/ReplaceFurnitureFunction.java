package net.momirealms.craftengine.core.plugin.context.function;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import net.momirealms.craftengine.core.entity.furniture.AnchorType;
import net.momirealms.craftengine.core.entity.furniture.CustomFurniture;
import net.momirealms.craftengine.core.entity.furniture.Furniture;
import net.momirealms.craftengine.core.entity.furniture.FurnitureExtraData;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.number.NumberProvider;
import net.momirealms.craftengine.core.plugin.context.number.NumberProviders;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import net.momirealms.craftengine.core.world.WorldPosition;

public class ReplaceFurnitureFunction<CTX extends Context> extends AbstractConditionalFunction<CTX> {
    private final Key newFurnitureId;
    private final NumberProvider x;
    private final NumberProvider y;
    private final NumberProvider z;
    private final NumberProvider pitch;
    private final NumberProvider yaw;
    private final AnchorType anchorType;
    private final boolean dropLoot;
    private final boolean playSound;

    public ReplaceFurnitureFunction(Key newFurnitureId, NumberProvider x, NumberProvider y, NumberProvider z,
                                   NumberProvider pitch, NumberProvider yaw, AnchorType anchorType,
                                   boolean dropLoot, boolean playSound, List<Condition<CTX>> predicates) {
        super(predicates);
        this.newFurnitureId = newFurnitureId;
        this.x = x;
        this.y = y;
        this.z = z;
        this.pitch = pitch;
        this.yaw = yaw;
        this.anchorType = anchorType;
        this.dropLoot = dropLoot;
        this.playSound = playSound;
    }

    @Override
    public void runInternal(CTX ctx) {
        Optional<WorldPosition> optionalWorldPosition = ctx.getOptionalParameter(DirectContextParameters.POSITION);
        Optional<Furniture> optionalOldFurniture = ctx.getOptionalParameter(DirectContextParameters.FURNITURE);
        
        if (optionalWorldPosition.isPresent() && optionalOldFurniture.isPresent()) {
            Furniture oldFurniture = optionalOldFurniture.get();
            
            // Obtener la nueva posición o usar la actual del mueble
            double xPos = this.x.getDouble(ctx);
            double yPos = this.y.getDouble(ctx);
            double zPos = this.z.getDouble(ctx);
            float pitchValue = this.pitch.getFloat(ctx);
            float yawValue = this.yaw.getFloat(ctx);
            
            WorldPosition newPosition = new WorldPosition(optionalWorldPosition.get().world(), xPos, yPos, zPos, pitchValue, yawValue);
            
            // Obtener el nuevo mueble
            Optional<CustomFurniture> optionalNewFurniture = CraftEngine.instance().furnitureManager().furnitureById(this.newFurnitureId);
            if (optionalNewFurniture.isPresent()) {
                CustomFurniture newFurniture = optionalNewFurniture.get();
                AnchorType anchor = this.anchorType != null ? this.anchorType : newFurniture.getAnyAnchorType();
                
                // Remover el mueble antiguo
                if (oldFurniture.isValid()) {
                    oldFurniture.destroy();
                    // TODO: Implementar lógica para dropear loot usando this.dropLoot
                }
                
                // Colocar el nuevo mueble
                FurnitureExtraData extraData = FurnitureExtraData.builder().anchorType(anchor).build();
                CraftEngine.instance().furnitureManager().place(newPosition, newFurniture, extraData, this.playSound);
            }
        }
    }

    @Override
    public Key type() {
        return CommonFunctions.REPLACE_FURNITURE;
    }

    public static class FactoryImpl<CTX extends Context> extends AbstractFactory<CTX> {

        public FactoryImpl(java.util.function.Function<Map<String, Object>, Condition<CTX>> factory) {
            super(factory);
        }

        @Override
        public Function<CTX> create(Map<String, Object> arguments) {
            String furnitureIdStr = ResourceConfigUtils.requireNonEmptyStringOrThrow(arguments.get("furniture-id"), "warning.config.function.replace_furniture.missing_furniture_id");
            Key furnitureId = Key.of(furnitureIdStr);
            NumberProvider x = NumberProviders.fromObject(arguments.getOrDefault("x", "<arg:furniture.x>"));
            NumberProvider y = NumberProviders.fromObject(arguments.getOrDefault("y", "<arg:furniture.y>"));
            NumberProvider z = NumberProviders.fromObject(arguments.getOrDefault("z", "<arg:furniture.z>"));
            NumberProvider pitch = NumberProviders.fromObject(arguments.getOrDefault("pitch", "<arg:furniture.pitch>"));
            NumberProvider yaw = NumberProviders.fromObject(arguments.getOrDefault("yaw", "<arg:furniture.yaw>"));
            AnchorType anchorType = Optional.ofNullable(arguments.get("anchor-type")).map(o -> AnchorType.valueOf(o.toString().toUpperCase())).orElse(null);
            boolean dropLoot = ResourceConfigUtils.getAsBoolean(arguments.getOrDefault("drop-loot", true), "drop-loot");
            boolean playSound = ResourceConfigUtils.getAsBoolean(arguments.getOrDefault("play-sound", true), "play-sound");
            return new ReplaceFurnitureFunction<>(furnitureId, x, y, z, pitch, yaw, anchorType, dropLoot, playSound, getPredicates(arguments));
        }
    }
}
