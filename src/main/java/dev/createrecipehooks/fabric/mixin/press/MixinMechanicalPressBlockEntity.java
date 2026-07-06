package dev.createrecipehooks.fabric.mixin.press;

import com.simibubi.create.content.kinetics.press.MechanicalPressBlockEntity;
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
 * Fabric port of the Forge {@code MixinMechanicalPressBlockEntity}.
 *
 * <p>Simplification over the Forge version: instead of wrapping the individual
 * {@code RecipeApplier.applyRecipeOn} call sites, {@link CrhOwnerContext} covers the whole
 * {@code tryProcessOnBelt} / {@code tryProcessInWorld} methods (HEAD/RETURN). This also
 * covers the bulk-processing branch of {@code tryProcessInWorld}, which delegates through
 * the {@code applyRecipeOn(ItemEntity, ...)} overload.
 *
 * <p>Signatures verified against Create Fabric 6.0.8.1 (lines 116 and 150) — identical
 * to Forge.
 */
@Mixin(MechanicalPressBlockEntity.class)
public abstract class MixinMechanicalPressBlockEntity implements ICrhOwnable {

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

    @Inject(method = {
                "tryProcessOnBelt(Lcom/simibubi/create/content/kinetics/belt/transport/TransportedItemStack;Ljava/util/List;Z)Z",
                "tryProcessInWorld(Lnet/minecraft/world/entity/item/ItemEntity;Z)Z"
            },
            at = @At("HEAD"))
    private void crh$setPressOwnerContext(CallbackInfoReturnable<Boolean> cir) {
        CrhOwnerContext.set(crh$ownerUUID);
    }

    @Inject(method = {
                "tryProcessOnBelt(Lcom/simibubi/create/content/kinetics/belt/transport/TransportedItemStack;Ljava/util/List;Z)Z",
                "tryProcessInWorld(Lnet/minecraft/world/entity/item/ItemEntity;Z)Z"
            },
            at = @At("RETURN"))
    private void crh$clearPressOwnerContext(CallbackInfoReturnable<Boolean> cir) {
        CrhOwnerContext.clear();
    }
}
