package dev.createrecipehooks.fabric.mixin.drain;

import com.simibubi.create.content.fluids.drain.ItemDrainBlockEntity;
import dev.createrecipehooks.api.ICrhOwnable;
import dev.createrecipehooks.internal.CrhOwnerContext;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

/**
 * Fabric port of the Forge {@code MixinItemDrainBlockEntity}: {@link ICrhOwnable} UUID
 * tracking plus {@link CrhOwnerContext} coverage of {@code continueProcessing()}
 * (verified at line 235 of Create Fabric 6.0.8.1, drives GenericItemEmptying).
 */
@Mixin(ItemDrainBlockEntity.class)
public abstract class MixinItemDrainBlockEntity implements ICrhOwnable {

    @Unique private @Nullable UUID crh$ownerUUID = null;

    @Override public @Nullable UUID crh$getOwnerUUID() { return crh$ownerUUID; }
    @Override public void crh$setOwnerUUID(@Nullable UUID uuid) { this.crh$ownerUUID = uuid; }

    @Inject(method = "write(Lnet/minecraft/nbt/CompoundTag;Z)V", at = @At("HEAD"))
    private void crh$writeOwner(CompoundTag tag, boolean clientPacket, CallbackInfo ci) {
        if (!clientPacket && crh$ownerUUID != null)
            tag.putUUID("crh:owner", crh$ownerUUID);
    }

    @Inject(method = "read(Lnet/minecraft/nbt/CompoundTag;Z)V", at = @At("HEAD"))
    private void crh$readOwner(CompoundTag tag, boolean clientPacket, CallbackInfo ci) {
        if (!clientPacket)
            crh$ownerUUID = tag.hasUUID("crh:owner") ? tag.getUUID("crh:owner") : null;
    }

    @Inject(method = "continueProcessing()Z", at = @At("HEAD"))
    private void crh$setOwnerContext(CallbackInfoReturnable<Boolean> ci) {
        CrhOwnerContext.set(crh$ownerUUID);
    }

    @Inject(method = "continueProcessing()Z", at = @At("RETURN"))
    private void crh$clearOwnerContext(CallbackInfoReturnable<Boolean> ci) {
        CrhOwnerContext.clear();
    }
}
