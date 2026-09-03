package dev.createrecipehooks.mixin.crafter;

import com.simibubi.create.content.kinetics.crafter.MechanicalCrafterBlockEntity;
import dev.createrecipehooks.api.ICrhOwnable;
import dev.createrecipehooks.internal.CrhOwnerContext;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

// Owner tracking for the Mechanical Crafter. The output crafter's UUID is handed to MixinRecipeGridHandler through CrhOwnerContext around the tryToApplyRecipe call in tick().
@Mixin(value = MechanicalCrafterBlockEntity.class, remap = false)
public abstract class MixinMechanicalCrafterBlockEntity implements ICrhOwnable {

    @Unique private @Nullable UUID crh$ownerUUID = null;

    @Override public @Nullable UUID crh$getOwnerUUID() { return crh$ownerUUID; }
    @Override public void crh$setOwnerUUID(@Nullable UUID uuid) { this.crh$ownerUUID = uuid; }

    @Inject(method = "write(Lnet/minecraft/nbt/CompoundTag;Lnet/minecraft/core/HolderLookup$Provider;Z)V", at = @At("HEAD"))
    private void crh$writeOwner(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket, CallbackInfo ci) {
        if (!clientPacket && crh$ownerUUID != null)
            tag.putUUID("crh:owner", crh$ownerUUID);
    }

    @Inject(method = "read(Lnet/minecraft/nbt/CompoundTag;Lnet/minecraft/core/HolderLookup$Provider;Z)V", at = @At("HEAD"))
    private void crh$readOwner(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket, CallbackInfo ci) {
        if (!clientPacket && tag.hasUUID("crh:owner"))
            crh$ownerUUID = tag.getUUID("crh:owner");
    }

    // Only the output crafter (the one with no targeting crafter) reaches this call site.
    @Inject(
        method = "tick()V",
        at = @At(
            value = "INVOKE",
            target = "Lcom/simibubi/create/content/kinetics/crafter/RecipeGridHandler;" +
                     "tryToApplyRecipe(Lnet/minecraft/world/level/Level;" +
                     "Lcom/simibubi/create/content/kinetics/crafter/RecipeGridHandler$GroupedItems;)" +
                     "Lnet/minecraft/world/item/ItemStack;"
        )
    )
    private void crh$setOwnerContextBeforeTry(CallbackInfo ci) {
        CrhOwnerContext.set(crh$ownerUUID);
    }

    @Inject(
        method = "tick()V",
        at = @At(
            value = "INVOKE",
            target = "Lcom/simibubi/create/content/kinetics/crafter/RecipeGridHandler;" +
                     "tryToApplyRecipe(Lnet/minecraft/world/level/Level;" +
                     "Lcom/simibubi/create/content/kinetics/crafter/RecipeGridHandler$GroupedItems;)" +
                     "Lnet/minecraft/world/item/ItemStack;",
            shift = At.Shift.AFTER
        )
    )
    private void crh$clearOwnerContextAfterTry(CallbackInfo ci) {
        CrhOwnerContext.clear();
    }
}
