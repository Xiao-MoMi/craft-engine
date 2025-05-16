package net.momirealms.craftengine.fabric.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.AbstractRailBlock;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static net.momirealms.craftengine.fabric.client.CraftEngineFabricModClient.serverInstalled;
import static net.momirealms.craftengine.fabric.client.config.ModConfig.enableCancelBlockUpdate;

@Environment(EnvType.CLIENT)
@Mixin(AbstractRailBlock.class)
public abstract class AbstractRailBlockMixin {

    @Inject(method = "updateCurves", at = @At("HEAD"), cancellable = true)
    private void cancelUpdateCurves(BlockState state, World world, BlockPos pos, boolean notify, CallbackInfoReturnable<BlockState> cir) {
        if (!enableCancelBlockUpdate || !serverInstalled) return;
        cir.setReturnValue(state);
    }
}
