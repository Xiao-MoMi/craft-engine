package net.momirealms.craftengine.core.entity.furniture;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.context.number.NumberProvider;
import net.momirealms.craftengine.core.plugin.scheduler.SchedulerTask;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.world.Position;
import net.momirealms.craftengine.core.world.Vec3d;
import net.momirealms.craftengine.core.world.particle.ParticleData;

public class ParticleEmitter extends AbstractFurnitureEmitter {
    private final Key particleType;
    private final NumberProvider x;
    private final NumberProvider y;
    private final NumberProvider z;
    private final NumberProvider count;
    private final NumberProvider xOffset;
    private final NumberProvider yOffset;
    private final NumberProvider zOffset;
    private final NumberProvider speed;
    private final ParticleData particleData;
    private final NumberProvider interval; // Ticks between emissions
    private final NumberProvider duration; // Total emission duration in ticks, -1 for infinite
    
    // Advanced physics and motion properties
    private final NumberProvider gravityX;
    private final NumberProvider gravityY;
    private final NumberProvider gravityZ;
    private final NumberProvider velocityX;
    private final NumberProvider velocityY;
    private final NumberProvider velocityZ;
    private final NumberProvider accelerationX;
    private final NumberProvider accelerationY;
    private final NumberProvider accelerationZ;
    private final NumberProvider randomness;
    private final boolean inheritMotion;
    private final NumberProvider maxAge;
    private final NumberProvider fadeInTime;
    private final NumberProvider fadeOutTime;
    private final NumberProvider rotationSpeed;
    private final NumberProvider scaleModifier;
    
    private SchedulerTask emissionTask;
    private int elapsedTicks = 0;

    public ParticleEmitter(Vector3f position, Key particleType, NumberProvider x, NumberProvider y, NumberProvider z,
                          NumberProvider count, NumberProvider xOffset, NumberProvider yOffset, NumberProvider zOffset,
                          NumberProvider speed, ParticleData particleData, NumberProvider interval, NumberProvider duration,
                          NumberProvider gravityX, NumberProvider gravityY, NumberProvider gravityZ,
                          NumberProvider velocityX, NumberProvider velocityY, NumberProvider velocityZ,
                          NumberProvider accelerationX, NumberProvider accelerationY, NumberProvider accelerationZ,
                          NumberProvider randomness, boolean inheritMotion, NumberProvider maxAge,
                          NumberProvider fadeInTime, NumberProvider fadeOutTime, NumberProvider rotationSpeed,
                          NumberProvider scaleModifier) {
        super(position);
        this.particleType = particleType;
        this.x = x;
        this.y = y;
        this.z = z;
        this.count = count;
        this.xOffset = xOffset;
        this.yOffset = yOffset;
        this.zOffset = zOffset;
        this.speed = speed;
        this.particleData = particleData;
        this.interval = interval;
        this.duration = duration;
        this.gravityX = gravityX;
        this.gravityY = gravityY;
        this.gravityZ = gravityZ;
        this.velocityX = velocityX;
        this.velocityY = velocityY;
        this.velocityZ = velocityZ;
        this.accelerationX = accelerationX;
        this.accelerationY = accelerationY;
        this.accelerationZ = accelerationZ;
        this.randomness = randomness;
        this.inheritMotion = inheritMotion;
        this.maxAge = maxAge;
        this.fadeInTime = fadeInTime;
        this.fadeOutTime = fadeOutTime;
        this.rotationSpeed = rotationSpeed;
        this.scaleModifier = scaleModifier;
    }

    @Override
    public Key type() {
        return FurnitureEmitterTypes.PARTICLE;
    }

    @Override
    public void startEmission(Furniture furniture, @NotNull Quaternionf conjugated) {
        if (isEmitting()) {
            return;
        }
        
        setEmitting(true);
        this.elapsedTicks = 0;
        
        // Get the emission interval in ticks
        int intervalTicks = Math.max(1, interval.getInt(null)); // Default context, could be improved
        
        // Schedule the emission task
        emissionTask = CraftEngine.instance().scheduler().sync().runRepeating(() -> {
            if (!isEmitting() || !furniture.isValid()) {
                stopEmission(furniture);
                return;
            }
            
            // Check if duration has elapsed (if not infinite)
            int maxDuration = this.duration.getInt(null);
            if (maxDuration > 0 && this.elapsedTicks >= maxDuration) {
                stopEmission(furniture);
                return;
            }
            
            // Emit particles
            emitParticles(furniture, conjugated);
            this.elapsedTicks += intervalTicks;
        }, 0, intervalTicks);
    }

    @Override
    public void stopEmission(Furniture furniture) {
        if (!isEmitting()) {
            return;
        }
        
        setEmitting(false);
        
        if (emissionTask != null) {
            emissionTask.cancel();
            emissionTask = null;
        }
        
        this.elapsedTicks = 0;
    }

    private void emitParticles(Furniture furniture, Quaternionf conjugated) {
        // Transform the emission position based on furniture rotation
        Vector3f worldOffset = conjugated.transform(new Vector3f(position()));
        
        // Get furniture position
        double furniturePosX = furniture.position().x();
        double furniturePosY = furniture.position().y();
        double furniturePosZ = furniture.position().z();
        
        // Calculate base particle spawn position
        double baseX = furniturePosX + worldOffset.x + x.getDouble(null);
        double baseY = furniturePosY + worldOffset.y + y.getDouble(null);
        double baseZ = furniturePosZ + worldOffset.z + z.getDouble(null);
        
        // Apply randomness to position if specified
        double randomnessValue = randomness.getDouble(null);
        double finalX = baseX + (Math.random() - 0.5) * randomnessValue;
        double finalY = baseY + (Math.random() - 0.5) * randomnessValue;
        double finalZ = baseZ + (Math.random() - 0.5) * randomnessValue;
        
        // Apply gravity effects to offset calculations
        double gravityOffsetX = xOffset.getDouble(null) + gravityX.getDouble(null) * (elapsedTicks / 20.0);
        double gravityOffsetY = yOffset.getDouble(null) + gravityY.getDouble(null) * (elapsedTicks / 20.0);
        double gravityOffsetZ = zOffset.getDouble(null) + gravityZ.getDouble(null) * (elapsedTicks / 20.0);
        
        // Apply velocity and acceleration
        double timeInSeconds = elapsedTicks / 20.0;
        double velocityAdjustedX = velocityX.getDouble(null) * timeInSeconds + 0.5 * accelerationX.getDouble(null) * timeInSeconds * timeInSeconds;
        double velocityAdjustedY = velocityY.getDouble(null) * timeInSeconds + 0.5 * accelerationY.getDouble(null) * timeInSeconds * timeInSeconds;
        double velocityAdjustedZ = velocityZ.getDouble(null) * timeInSeconds + 0.5 * accelerationZ.getDouble(null) * timeInSeconds * timeInSeconds;
        
        finalX += velocityAdjustedX;
        finalY += velocityAdjustedY;
        finalZ += velocityAdjustedZ;
        
        // Calculate dynamic speed based on scale modifier and rotation
        double dynamicSpeed = speed.getDouble(null) * scaleModifier.getDouble(null);
        if (rotationSpeed.getDouble(null) != 0) {
            // Add circular motion effect
            double angle = (elapsedTicks * rotationSpeed.getDouble(null)) % (2 * Math.PI);
            finalX += Math.cos(angle) * 0.5;
            finalZ += Math.sin(angle) * 0.5;
        }
        
        // Apply age-based effects for fading
        int particleCount = count.getInt(null);
        double fadeInTimeValue = fadeInTime.getDouble(null);
        double fadeOutTimeValue = fadeOutTime.getDouble(null);
        double maxAgeValue = maxAge.getDouble(null);
        
        if (maxAgeValue > 0) {
            double ageRatio = elapsedTicks / maxAgeValue;
            if (ageRatio < fadeInTimeValue) {
                // Fade in effect - reduce particle count
                particleCount = (int) (particleCount * (ageRatio / fadeInTimeValue));
            } else if (ageRatio > (1.0 - fadeOutTimeValue)) {
                // Fade out effect - reduce particle count
                double fadeRatio = (1.0 - ageRatio) / fadeOutTimeValue;
                particleCount = (int) (particleCount * fadeRatio);
            }
        }
        
        // Spawn particles with calculated properties
        Position spawnPos = new Vec3d(finalX, finalY, finalZ);
        furniture.position().world().spawnParticle(
            spawnPos,
            particleType,
            Math.max(0, particleCount),
            gravityOffsetX,
            gravityOffsetY,
            gravityOffsetZ,
            dynamicSpeed,
            particleData,
            null // Could pass a context here for advanced functionality
        );
    }

    public static class Builder implements FurnitureEmitter.Builder {
        private Vector3f position = new Vector3f();
        private Key particleType;
        private NumberProvider x;
        private NumberProvider y;
        private NumberProvider z;
        private NumberProvider count;
        private NumberProvider xOffset;
        private NumberProvider yOffset;
        private NumberProvider zOffset;
        private NumberProvider speed;
        private ParticleData particleData;
        private NumberProvider interval;
        private NumberProvider duration;
        
        // Advanced properties with defaults
        private NumberProvider gravityX;
        private NumberProvider gravityY;
        private NumberProvider gravityZ;
        private NumberProvider velocityX;
        private NumberProvider velocityY;
        private NumberProvider velocityZ;
        private NumberProvider accelerationX;
        private NumberProvider accelerationY;
        private NumberProvider accelerationZ;
        private NumberProvider randomness;
        private boolean inheritMotion = false;
        private NumberProvider maxAge;
        private NumberProvider fadeInTime;
        private NumberProvider fadeOutTime;
        private NumberProvider rotationSpeed;
        private NumberProvider scaleModifier;

        @Override
        public Builder position(Vector3f position) {
            this.position = position;
            return this;
        }

        public Builder particleType(Key particleType) {
            this.particleType = particleType;
            return this;
        }

        public Builder x(NumberProvider x) {
            this.x = x;
            return this;
        }

        public Builder y(NumberProvider y) {
            this.y = y;
            return this;
        }

        public Builder z(NumberProvider z) {
            this.z = z;
            return this;
        }

        public Builder count(NumberProvider count) {
            this.count = count;
            return this;
        }

        public Builder xOffset(NumberProvider xOffset) {
            this.xOffset = xOffset;
            return this;
        }

        public Builder yOffset(NumberProvider yOffset) {
            this.yOffset = yOffset;
            return this;
        }

        public Builder zOffset(NumberProvider zOffset) {
            this.zOffset = zOffset;
            return this;
        }

        public Builder speed(NumberProvider speed) {
            this.speed = speed;
            return this;
        }

        public Builder particleData(@Nullable ParticleData particleData) {
            this.particleData = particleData;
            return this;
        }

        public Builder interval(NumberProvider interval) {
            this.interval = interval;
            return this;
        }

        public Builder duration(NumberProvider duration) {
            this.duration = duration;
            return this;
        }

        // Advanced property setters
        public Builder gravityX(NumberProvider gravityX) {
            this.gravityX = gravityX;
            return this;
        }

        public Builder gravityY(NumberProvider gravityY) {
            this.gravityY = gravityY;
            return this;
        }

        public Builder gravityZ(NumberProvider gravityZ) {
            this.gravityZ = gravityZ;
            return this;
        }

        public Builder velocityX(NumberProvider velocityX) {
            this.velocityX = velocityX;
            return this;
        }

        public Builder velocityY(NumberProvider velocityY) {
            this.velocityY = velocityY;
            return this;
        }

        public Builder velocityZ(NumberProvider velocityZ) {
            this.velocityZ = velocityZ;
            return this;
        }

        public Builder accelerationX(NumberProvider accelerationX) {
            this.accelerationX = accelerationX;
            return this;
        }

        public Builder accelerationY(NumberProvider accelerationY) {
            this.accelerationY = accelerationY;
            return this;
        }

        public Builder accelerationZ(NumberProvider accelerationZ) {
            this.accelerationZ = accelerationZ;
            return this;
        }

        public Builder randomness(NumberProvider randomness) {
            this.randomness = randomness;
            return this;
        }

        public Builder inheritMotion(boolean inheritMotion) {
            this.inheritMotion = inheritMotion;
            return this;
        }

        public Builder maxAge(NumberProvider maxAge) {
            this.maxAge = maxAge;
            return this;
        }

        public Builder fadeInTime(NumberProvider fadeInTime) {
            this.fadeInTime = fadeInTime;
            return this;
        }

        public Builder fadeOutTime(NumberProvider fadeOutTime) {
            this.fadeOutTime = fadeOutTime;
            return this;
        }

        public Builder rotationSpeed(NumberProvider rotationSpeed) {
            this.rotationSpeed = rotationSpeed;
            return this;
        }

        public Builder scaleModifier(NumberProvider scaleModifier) {
            this.scaleModifier = scaleModifier;
            return this;
        }

        @Override
        public FurnitureEmitter build() {
            return new ParticleEmitter(position, particleType, x, y, z, count, xOffset, yOffset, zOffset, speed, 
                particleData, interval, duration, gravityX, gravityY, gravityZ, velocityX, velocityY, velocityZ,
                accelerationX, accelerationY, accelerationZ, randomness, inheritMotion, maxAge, fadeInTime, 
                fadeOutTime, rotationSpeed, scaleModifier);
        }
    }
}
