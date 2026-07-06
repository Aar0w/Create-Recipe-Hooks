package dev.createrecipehooks.mixin.basin;

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

/**
 * Persists the UUID of the player who placed this Basin into the BlockEntity's NBT,
 * allowing CRH to attribute recipe completions to a specific player.
 *
 * <p>NBT key: {@code crh:owner} (stored as two longs via {@link CompoundTag#putUUID}).
 * Null if the basin was placed by a piston, command, or other non-player entity.
 */
@Mixin(value = BasinBlockEntity.class, remap = false)
public abstract class MixinBasinBlockEntity implements ICrhOwnable {

    @Unique
    private @Nullable UUID crh$ownerUUID = null;

    @Override
    public @Nullable UUID crh$getOwnerUUID() {
        return crh$ownerUUID;
    }

    @Override
    public void crh$setOwnerUUID(@Nullable UUID uuid) {
        this.crh$ownerUUID = uuid;
    }

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
