package dev.createrecipehooks.fabric.mixin.millstone;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.kinetics.millstone.MillingRecipe;
import com.simibubi.create.content.kinetics.millstone.MillstoneBlockEntity;
import dev.createrecipehooks.api.ICrhOwnable;
import dev.createrecipehooks.api.RecipeFinishedContext;
import dev.createrecipehooks.api.RecipeSource;
import dev.createrecipehooks.core.RecipeEventDispatcher;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.UUID;

// Fires the MILLSTONE event when a milling recipe completes and tracks the
// Millstone's owner for attribution.
@Mixin(MillstoneBlockEntity.class)
public abstract class MixinMillstoneBlockEntity implements ICrhOwnable {

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

    @WrapOperation(
        method = "process()V",
        at = @At(
            value = "INVOKE",
            target = "Lcom/simibubi/create/content/kinetics/millstone/MillingRecipe;" +
                     "rollResults()Ljava/util/List;"
        )
    )
    private List<ItemStack> crh$onMillstoneProcess(
            MillingRecipe recipe,
            Operation<List<ItemStack>> original
    ) {
        List<ItemStack> results = original.call(recipe);

        MillstoneBlockEntity self = (MillstoneBlockEntity)(Object)this;
        Level level = self.getLevel();

        if (level != null && !level.isClientSide() && !results.isEmpty()) {
            RecipeFinishedContext.Builder builder = RecipeFinishedContext.of(RecipeSource.MILLSTONE, level)
                .blockPos(self.getBlockPos())
                .recipe(recipe)
                .itemOutputs(results);

            if (crh$ownerUUID != null) {
                builder.meta("createrecipehooks:owner_uuid", crh$ownerUUID.toString());
            }

            RecipeEventDispatcher.dispatch(builder.build());
        }

        return results;
    }
}
