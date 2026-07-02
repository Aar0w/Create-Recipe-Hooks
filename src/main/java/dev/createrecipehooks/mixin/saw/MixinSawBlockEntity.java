package dev.createrecipehooks.mixin.saw;

import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.kinetics.saw.SawBlockEntity;
import dev.createrecipehooks.api.ICrhOwnable;
import dev.createrecipehooks.api.RecipeFinishedContext;
import dev.createrecipehooks.api.RecipeSource;
import dev.createrecipehooks.core.RecipeEventDispatcher;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.UUID;

@Mixin(value = SawBlockEntity.class, remap = false)
public abstract class MixinSawBlockEntity implements ICrhOwnable {

    @Unique private @Nullable UUID crh$ownerUUID = null;

    @Override public @Nullable UUID crh$getOwnerUUID() { return crh$ownerUUID; }
    @Override public void crh$setOwnerUUID(@Nullable UUID uuid) { this.crh$ownerUUID = uuid; }

    @Inject(method = "write(Lnet/minecraft/nbt/CompoundTag;Z)V", at = @At("TAIL"))
    private void crh$saveOwner(CompoundTag tag, boolean clientPacket, CallbackInfo ci) {
        if (!clientPacket && crh$ownerUUID != null)
            tag.putUUID("crh:owner", crh$ownerUUID);
    }

    @Inject(method = "read(Lnet/minecraft/nbt/CompoundTag;Z)V", at = @At("TAIL"))
    private void crh$loadOwner(CompoundTag tag, boolean clientPacket, CallbackInfo ci) {
        if (!clientPacket)
            crh$ownerUUID = tag.hasUUID("crh:owner") ? tag.getUUID("crh:owner") : null;
    }

    @Shadow private int recipeIndex;
    @Shadow public com.simibubi.create.content.processing.recipe.ProcessingInventory inventory;

    @Inject(
        method = "applyRecipe()V",
        at = @At(value = "RETURN", ordinal = 2)
    )
    private void crh$onSawApplied(
            CallbackInfo ci,
            @Local(ordinal = 1) List<? extends Recipe<?>> recipes
    ) {
        SawBlockEntity self = (SawBlockEntity)(Object)this;
        Level level = self.getLevel();
        if (level == null || level.isClientSide()) return;
        if (recipes == null || recipes.isEmpty()) return;

        int idx = recipeIndex;
        if (idx < 0 || idx >= recipes.size()) return;

        Recipe<?> recipe = recipes.get(idx);

        List<ItemStack> outputs = new java.util.ArrayList<>();
        for (int s = 1; s < inventory.getSlots(); s++) {
            ItemStack out = inventory.getStackInSlot(s);
            if (!out.isEmpty()) outputs.add(out.copy());
        }

        RecipeFinishedContext.Builder builder = RecipeFinishedContext.of(RecipeSource.MECHANICAL_SAW, level)
            .blockPos(self.getBlockPos())
            .recipe(recipe)
            .recipeId(recipe.getId())
            .itemOutputs(outputs);

        if (crh$ownerUUID != null) {
            builder.meta("createrecipehooks:owner_uuid", crh$ownerUUID.toString());
        }

        RecipeEventDispatcher.dispatch(builder.build());
    }
}
