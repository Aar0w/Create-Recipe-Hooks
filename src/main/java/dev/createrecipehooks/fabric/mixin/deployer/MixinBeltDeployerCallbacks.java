package dev.createrecipehooks.fabric.mixin.deployer;

import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.content.kinetics.deployer.BeltDeployerCallbacks;
import com.simibubi.create.content.kinetics.deployer.DeployerBlockEntity;
import dev.createrecipehooks.api.ICrhOwnable;
import dev.createrecipehooks.api.RecipeFinishedContext;
import dev.createrecipehooks.api.RecipeSource;
import dev.createrecipehooks.core.RecipeEventDispatcher;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

// Fires the DEPLOYER_BELT event when a Deployer applies a recipe, attributed to the
// Deployer's owner. Deployer recipe types are excluded from the shared RecipeApplier
// hook so the event fires exactly once.
@Mixin(BeltDeployerCallbacks.class)
public abstract class MixinBeltDeployerCallbacks {

    @Inject(
        method = "activate(" +
                 "Lcom/simibubi/create/content/kinetics/belt/transport/TransportedItemStack;" +
                 "Lcom/simibubi/create/content/kinetics/belt/behaviour/TransportedItemStackHandlerBehaviour;" +
                 "Lcom/simibubi/create/content/kinetics/deployer/DeployerBlockEntity;" +
                 "Lnet/minecraft/world/item/crafting/Recipe;" +
                 ")V",
        at = @At("RETURN")
    )
    private static void crh$onDeployerActivated(
            TransportedItemStack stack,
            TransportedItemStackHandlerBehaviour handler,
            DeployerBlockEntity deployer,
            Recipe<?> recipe,
            CallbackInfo ci
    ) {
        Level level = deployer.getLevel();
        if (level == null || level.isClientSide()) return;

        UUID ownerUUID = (deployer instanceof ICrhOwnable ownable)
                ? ownable.crh$getOwnerUUID() : null;

        RecipeFinishedContext.Builder builder =
                RecipeFinishedContext.of(RecipeSource.DEPLOYER_BELT, level)
                        .recipe(recipe);

        if (ownerUUID != null) builder.meta("createrecipehooks:owner_uuid", ownerUUID.toString());

        RecipeEventDispatcher.dispatch(builder.build());
    }
}
