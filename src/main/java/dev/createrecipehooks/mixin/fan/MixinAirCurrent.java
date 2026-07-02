package dev.createrecipehooks.mixin.fan;

import com.simibubi.create.content.kinetics.fan.AirCurrent;
import com.simibubi.create.content.kinetics.fan.IAirCurrentSource;
import dev.createrecipehooks.api.ICrhOwnable;
import dev.createrecipehooks.internal.CrhOwnerContext;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Sets {@link CrhOwnerContext} for the entire duration of {@code AirCurrent.tick()},
 * covering both fan processing modes confirmed via javap of Create 6.0.8:
 *
 * <pre>
 * AirCurrent.tick()  [offset 149]
 *   → tickAffectedEntities(Level)          ← world-mode: ItemEntity in air current
 *       → FanProcessing.applyProcessing(ItemEntity, FanProcessingType)
 *   → tickAffectedHandlers()  [offset 153] ← belt-mode: TransportedItemStack
 *       → handleProcessingOnAllItems(lambda)
 *           → FanProcessing.applyProcessing(TransportedItemStack, Level, FanProcessingType)
 *   Both paths → RecipeApplier.applyRecipeOn() ← MixinRecipeApplier reads CrhOwnerContext
 * </pre>
 *
 * <p>The previous implementation injected only into {@code tickAffectedHandlers()} at
 * {@code INVOKE handleProcessingOnAllItems}, which missed world-mode items processed
 * through {@code tickAffectedEntities(Level)}. Injecting into {@code tick()} HEAD/RETURN
 * is the single stable point that covers both paths.
 *
 * <p>{@code AirCurrent.source} is the {@code EncasedFanBlockEntity} that drives this
 * air current (confirmed: only implementor of {@code IAirCurrentSource} in Create).
 * The {@code instanceof ICrhOwnable} guard handles the case where a third-party mod
 * provides another {@code IAirCurrentSource} implementation.
 */
@Mixin(value = AirCurrent.class, remap = false)
public abstract class MixinAirCurrent {

    @Shadow @Final public IAirCurrentSource source;

    @Inject(method = "tick()V", at = @At("HEAD"))
    private void crh$setFanOwnerContext(CallbackInfo ci) {
        if (source instanceof ICrhOwnable ownable)
            CrhOwnerContext.set(ownable.crh$getOwnerUUID());
    }

    @Inject(method = "tick()V", at = @At("RETURN"))
    private void crh$clearFanOwnerContext(CallbackInfo ci) {
        CrhOwnerContext.clear();
    }
}
