package dev.createrecipehooks.mixin.crafter;

import com.simibubi.create.content.kinetics.crafter.MechanicalCrafterBlockEntity;
import dev.createrecipehooks.api.ICrhOwnable;
import dev.createrecipehooks.internal.CrhOwnerContext;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

/**
 * Adds {@link ICrhOwnable} UUID tracking to {@link MechanicalCrafterBlockEntity}.
 *
 * <h3>Why @At("INVOKE") on tick() instead of checkCompletedRecipe()</h3>
 * <p>{@code tryToApplyRecipe(Level, GroupedItems)} is called from {@code tick()}, not from
 * {@code checkCompletedRecipe(boolean)} — confirmed via javap of Create 6.0.8:
 * {@code tick()} offset 192 calls {@code RecipeGridHandler.tryToApplyRecipe}.
 * {@code checkCompletedRecipe} instead calls {@code RecipeGridHandler.getAllCraftersOfChainIf}
 * and then {@code List.forEach} with a lambda to propagate the check across the crafter network.
 *
 * <p>Only the "output" crafter (the last one in the chain that has no targeting crafter)
 * reaches the {@code tryToApplyRecipe} call site in its own {@code tick()} invocation.
 * Setting {@link CrhOwnerContext} immediately before that call ensures the correct UUID
 * is available when {@link MixinRecipeGridHandler#crh$onCrafterResult} reads it.
 *
 * <p>NBT: only SET when {@code "crh:owner"} is present — never clear if absent.
 * Create calls {@code read()} during network formation before any {@code write()} has
 * persisted the key; the old "else clear" logic would wipe the UUID set by
 * {@code NeoForgeAdapter.onOwnableBlockPlaced()}.
 */
@Mixin(value = MechanicalCrafterBlockEntity.class, remap = false)
public abstract class MixinMechanicalCrafterBlockEntity implements ICrhOwnable {

    @Unique private @Nullable UUID crh$ownerUUID = null;

    @Override public @Nullable UUID crh$getOwnerUUID() { return crh$ownerUUID; }
    @Override public void crh$setOwnerUUID(@Nullable UUID uuid) { this.crh$ownerUUID = uuid; }

    @Inject(method = "write(Lnet/minecraft/nbt/CompoundTag;Z)V", at = @At("TAIL"))
    private void crh$writeOwner(CompoundTag tag, boolean clientPacket, CallbackInfo ci) {
        if (!clientPacket && crh$ownerUUID != null)
            tag.putUUID("crh:owner", crh$ownerUUID);
    }

    @Inject(method = "read(Lnet/minecraft/nbt/CompoundTag;Z)V", at = @At("TAIL"))
    private void crh$readOwner(CompoundTag tag, boolean clientPacket, CallbackInfo ci) {
        if (!clientPacket && tag.hasUUID("crh:owner"))
            crh$ownerUUID = tag.getUUID("crh:owner");
    }

    // Set CrhOwnerContext from THIS instance right before tryToApplyRecipe executes.
    // tryToApplyRecipe is called from tick(), not checkCompletedRecipe — confirmed via javap.
    // Only the "output" crafter (the one with no targeting crafter) reaches this call site.
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
