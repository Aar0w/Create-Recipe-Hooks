package dev.createrecipehooks.fabric.mixin.fan;

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
 * Fabric port of the Forge {@code MixinAirCurrent}: sets {@link CrhOwnerContext} for the
 * entire duration of {@code AirCurrent.tick()}, covering both fan processing modes
 * (world items via {@code tickAffectedEntities}, belt items via {@code tickAffectedHandlers}).
 *
 * <p>Verified against Create Fabric 6.0.8.1: {@code tick()} calls both methods at lines 75/76;
 * {@code public final IAirCurrentSource source} exists (line 47).
 */
@Mixin(AirCurrent.class)
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
