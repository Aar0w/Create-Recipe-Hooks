package dev.createrecipehooks.fabric.mixin.drill;

import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.kinetics.base.BlockBreakingMovementBehaviour;
import com.simibubi.create.content.kinetics.drill.DrillMovementBehaviour;
import dev.createrecipehooks.api.BlockProcessedContext;
import dev.createrecipehooks.api.RecipeSource;
import dev.createrecipehooks.core.ContraptionOwner;
import dev.createrecipehooks.core.RecipeEventDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

/**
 * Fabric port of the Forge {@code MixinBlockBreakingMovementBehaviour}: Mechanical Drill
 * as a contraption actor. The {@code instanceof DrillMovementBehaviour} filter excludes
 * the Plough (which calls {@code super.onBlockBroken}); the Saw replaces the method
 * without a super call and never reaches this hook.
 */
@Mixin(BlockBreakingMovementBehaviour.class)
public class MixinBlockBreakingMovementBehaviour {

    @Inject(
        method = "onBlockBroken(Lcom/simibubi/create/content/contraptions/behaviour/MovementContext;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V",
        at = @At("HEAD")
    )
    private void crh$onActorBlockBroken(MovementContext context, BlockPos pos, BlockState brokenState, CallbackInfo ci) {
        if (!((Object) this instanceof DrillMovementBehaviour)) return;

        Level level = context.world;
        if (level == null || level.isClientSide()) return;

        BlockProcessedContext.Builder builder =
            BlockProcessedContext.of(RecipeSource.MECHANICAL_DRILL, level, brokenState)
                .blockPos(pos)
                .contraption(true);

        UUID owner = ContraptionOwner.fromBlockEntityData(context.blockEntityData);
        if (owner != null)
            builder.meta("createrecipehooks:owner_uuid", owner.toString());

        RecipeEventDispatcher.dispatchBlockProcessed(builder.build());
    }
}
