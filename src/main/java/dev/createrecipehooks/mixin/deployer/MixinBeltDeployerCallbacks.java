package dev.createrecipehooks.mixin.deployer;

import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.content.kinetics.deployer.BeltDeployerCallbacks;
import com.simibubi.create.content.kinetics.deployer.DeployerBlockEntity;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import dev.createrecipehooks.api.ICrhOwnable;
import dev.createrecipehooks.api.RecipeFinishedContext;
import dev.createrecipehooks.api.RecipeSource;
import dev.createrecipehooks.core.RecipeEventDispatcher;
import dev.createrecipehooks.internal.CrhOwnerContext;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

// Fires the DEPLOYER_BELT event when a Deployer applies a recipe, attributed to the Deployer's owner.
// Deployer recipe types are excluded from the shared RecipeApplier hook so the event fires exactly once.
@Mixin(value = BeltDeployerCallbacks.class, remap = false)
public abstract class MixinBeltDeployerCallbacks {

    // The owner context covers the whole activate call so the Sequenced Assembly hook
    // can attribute assembly completions happening inside it.
    @Inject(
        method = "activate(" +
                 "Lcom/simibubi/create/content/kinetics/belt/transport/TransportedItemStack;" +
                 "Lcom/simibubi/create/content/kinetics/belt/behaviour/TransportedItemStackHandlerBehaviour;" +
                 "Lcom/simibubi/create/content/kinetics/deployer/DeployerBlockEntity;" +
                 "Lnet/minecraft/world/item/crafting/Recipe;" +
                 ")V",
        at = @At("HEAD")
    )
    private static void crh$setOwnerContext(
            TransportedItemStack stack,
            TransportedItemStackHandlerBehaviour handler,
            DeployerBlockEntity deployer,
            Recipe<?> recipe,
            CallbackInfo ci
    ) {
        if (deployer instanceof ICrhOwnable ownable)
            CrhOwnerContext.set(ownable.crh$getOwnerUUID());
    }

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
        try {
            // Sequenced Assembly wraps its steps as deployer recipes and marks them with a
            // forced result; those steps stay silent, the assembly fires its own event.
            if (recipe instanceof ProcessingRecipe<?, ?>
                    && ((CrhProcessingRecipeAccessor) recipe).crh$getForcedResult() != null)
                return;

            Level level = deployer.getLevel();
            if (level == null || level.isClientSide()) return;

            UUID ownerUUID = (deployer instanceof ICrhOwnable ownable)
                    ? ownable.crh$getOwnerUUID() : null;

            RecipeFinishedContext.Builder builder =
                    RecipeFinishedContext.of(RecipeSource.DEPLOYER_BELT, level)
                            .recipe(recipe);

            if (ownerUUID != null) builder.meta("createrecipehooks:owner_uuid", ownerUUID.toString());

            RecipeEventDispatcher.dispatch(builder.build());
        } finally {
            CrhOwnerContext.clear();
        }
    }
}
