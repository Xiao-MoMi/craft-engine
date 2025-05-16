package net.momirealms.craftengine.fabric.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.fluid.FluidState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static net.momirealms.craftengine.fabric.client.CraftEngineFabricModClient.serverInstalled;
import static net.momirealms.craftengine.fabric.client.config.ModConfig.enableCancelBlockUpdate;

@Environment(EnvType.CLIENT)
@Mixin(FluidState.class)
public class FluidStateMixin {

    @Inject(method = "onScheduledTick", at = @At("HEAD"), cancellable = true)
    private void cancelScheduledTick(ServerWorld world, BlockPos pos, BlockState state, CallbackInfo ci) {
        if (!enableCancelBlockUpdate || !serverInstalled) return;
        ci.cancel();
    }
}
