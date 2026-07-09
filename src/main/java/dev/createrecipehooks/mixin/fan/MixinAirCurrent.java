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

// Carries the Fan owner's UUID through CrhOwnerContext for the whole AirCurrent.tick(), covering both fan modes (items on belts and items lying in the air current) so MixinRecipeApplier can attribute FAN_* events.
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
