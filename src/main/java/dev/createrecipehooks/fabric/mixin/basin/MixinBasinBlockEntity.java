package dev.createrecipehooks.fabric.mixin.basin;

import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import dev.createrecipehooks.api.ICrhOwnable;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

// Owner tracking for the Basin: persists the placing player's UUID in the block
// entity NBT so BASIN events can be attributed.
@Mixin(BasinBlockEntity.class)
public abstract class MixinBasinBlockEntity implements ICrhOwnable {

    @Unique private @Nullable UUID crh$ownerUUID = null;

    @Override public @Nullable UUID crh$getOwnerUUID() { return crh$ownerUUID; }
    @Override public void crh$setOwnerUUID(@Nullable UUID uuid) { this.crh$ownerUUID = uuid; }

    @Inject(method = "write(Lnet/minecraft/nbt/CompoundTag;Z)V", at = @At("HEAD"))
    private void crh$saveOwner(CompoundTag tag, boolean clientPacket, CallbackInfo ci) {
        if (!clientPacket && crh$ownerUUID != null)
            tag.putUUID("crh:owner", crh$ownerUUID);
    }

    @Inject(method = "read(Lnet/minecraft/nbt/CompoundTag;Z)V", at = @At("HEAD"))
    private void crh$loadOwner(CompoundTag tag, boolean clientPacket, CallbackInfo ci) {
        if (!clientPacket)
            crh$ownerUUID = tag.hasUUID("crh:owner") ? tag.getUUID("crh:owner") : null;
    }
}
