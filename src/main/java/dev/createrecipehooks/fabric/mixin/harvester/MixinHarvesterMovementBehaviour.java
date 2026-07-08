package dev.createrecipehooks.fabric.mixin.harvester;

import com.simibubi.create.content.contraptions.actors.harvester.HarvesterMovementBehaviour;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
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

// Mechanical Harvester (contraption actor): fires a blockProcessed event once per
// harvested plant, after all of Create's validity checks have passed.
@Mixin(HarvesterMovementBehaviour.class)
public class MixinHarvesterMovementBehaviour {

    @Inject(
        method = "visitNewPosition(Lcom/simibubi/create/content/contraptions/behaviour/MovementContext;Lnet/minecraft/core/BlockPos;)V",
        at = @At(value = "INVOKE",
            target = "Lcom/simibubi/create/foundation/utility/BlockHelper;destroyBlockAs(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;FLjava/util/function/Consumer;)V")
    )
    private void crh$onHarvest(MovementContext context, BlockPos pos, CallbackInfo ci) {
        Level level = context.world;
        if (level == null || level.isClientSide()) return;

        BlockState harvested = level.getBlockState(pos);

        BlockProcessedContext.Builder builder =
            BlockProcessedContext.of(RecipeSource.MECHANICAL_HARVESTER, level, harvested)
                .blockPos(pos)
                .contraption(true);

        UUID owner = ContraptionOwner.fromBlockEntityData(context.blockEntityData);
        if (owner != null)
            builder.meta("createrecipehooks:owner_uuid", owner.toString());

        RecipeEventDispatcher.dispatchBlockProcessed(builder.build());
    }
}
