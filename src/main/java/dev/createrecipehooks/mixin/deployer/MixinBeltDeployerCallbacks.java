package dev.createrecipehooks.mixin.deployer;

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

/**
 * Dispatches {@code DEPLOYER_BELT} events directly from the static
 * {@code BeltDeployerCallbacks.activate()} which is the single convergence point
 * for both world-mode and belt-mode Deployer recipe application.
 *
 * <p>World-mode: {@code DeployerBlockEntity.activate()} calls this method at bytecode
 * offset 125. Belt-mode: {@code BeltDeployerCallbacks.whenItemHeld()} calls this method
 * once the deployment timer expires.
 *
 * <p>{@code activate()} calls {@code RecipeApplier.applyRecipeOn()} at bytecode offset 14
 * (confirmed via javap). The UUID is read directly from the {@code deployer} parameter
 * (cast to {@link ICrhOwnable}, wired by {@link MixinDeployerBlockEntity}) to avoid
 * dependency on {@link dev.createrecipehooks.internal.CrhOwnerContext} and the ordering
 * issues that come with multiple RETURN injections.
 *
 * <p>{@code MixinRecipeApplier.resolveSource()} returns {@code null} for
 * {@code DEPLOYING} and {@code ITEM_APPLICATION} recipe types so the event is only
 * dispatched once — here, not in the shared RecipeApplier hook.
 */
@Mixin(value = BeltDeployerCallbacks.class, remap = false)
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
