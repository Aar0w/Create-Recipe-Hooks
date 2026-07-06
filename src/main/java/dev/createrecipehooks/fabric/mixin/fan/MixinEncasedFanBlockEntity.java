package dev.createrecipehooks.fabric.mixin.fan;

import com.simibubi.create.content.kinetics.fan.EncasedFanBlockEntity;
import dev.createrecipehooks.api.ICrhOwnable;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

/**
 * Fabric port of the Forge {@code MixinEncasedFanBlockEntity}: {@link ICrhOwnable}
 * UUID tracking persisted via Create's {@code write/read(CompoundTag, boolean)}
 * (verified in Create Fabric 6.0.8.1: write at line 51, read at line 44).
 */
@Mixin(EncasedFanBlockEntity.class)
public abstract class MixinEncasedFanBlockEntity implements ICrhOwnable {

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
        if (!clientPacket && tag.hasUUID("crh:owner"))
            crh$ownerUUID = tag.getUUID("crh:owner");
    }
}
