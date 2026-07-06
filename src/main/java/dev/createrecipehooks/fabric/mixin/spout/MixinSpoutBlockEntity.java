package dev.createrecipehooks.fabric.mixin.spout;

import com.simibubi.create.content.fluids.spout.SpoutBlockEntity;
import com.simibubi.create.content.kinetics.belt.behaviour.BeltProcessingBehaviour;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
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
 * Fabric port of the Forge {@code MixinSpoutBlockEntity}: {@link ICrhOwnable} UUID tracking
 * plus {@link CrhOwnerContext} coverage of {@code whenItemHeld} (the single caller of
 * {@code FillingBySpout.fillItem}, verified at line 120 of Create Fabric 6.0.8.1).
 */
@Mixin(SpoutBlockEntity.class)
public abstract class MixinSpoutBlockEntity implements ICrhOwnable {

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

    @Inject(
        method = "whenItemHeld(" +
                 "Lcom/simibubi/create/content/kinetics/belt/transport/TransportedItemStack;" +
                 "Lcom/simibubi/create/content/kinetics/belt/behaviour/TransportedItemStackHandlerBehaviour;" +
                 ")Lcom/simibubi/create/content/kinetics/belt/behaviour/BeltProcessingBehaviour$ProcessingResult;",
        at = @At("HEAD")
    )
    private void crh$setOwnerContext(
            TransportedItemStack stack,
            TransportedItemStackHandlerBehaviour handler,
            CallbackInfoReturnable<BeltProcessingBehaviour.ProcessingResult> ci
    ) {
        CrhOwnerContext.set(crh$ownerUUID);
    }

    @Inject(
        method = "whenItemHeld(" +
                 "Lcom/simibubi/create/content/kinetics/belt/transport/TransportedItemStack;" +
                 "Lcom/simibubi/create/content/kinetics/belt/behaviour/TransportedItemStackHandlerBehaviour;" +
                 ")Lcom/simibubi/create/content/kinetics/belt/behaviour/BeltProcessingBehaviour$ProcessingResult;",
        at = @At("RETURN")
    )
    private void crh$clearOwnerContext(
            TransportedItemStack stack,
            TransportedItemStackHandlerBehaviour handler,
            CallbackInfoReturnable<BeltProcessingBehaviour.ProcessingResult> ci
    ) {
        CrhOwnerContext.clear();
    }
}
