package dev.createrecipehooks.mixin.saw;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.kinetics.saw.SawBlockEntity;
import com.simibubi.create.content.kinetics.saw.TreeCutter;
import com.simibubi.create.foundation.utility.AbstractBlockBreakQueue;
import dev.createrecipehooks.api.BlockProcessedContext;
import dev.createrecipehooks.api.ICrhOwnable;
import dev.createrecipehooks.api.RecipeFinishedContext;
import dev.createrecipehooks.api.RecipeSource;
import dev.createrecipehooks.core.RecipeEventDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Mechanical Saw hooks: fires MECHANICAL_SAW recipe events for the upward-facing saw,
 * treeCut and blockProcessed events for the horizontal world-cutting saw, and tracks
 * the Saw's owner for attribution.
 */
@Mixin(value = SawBlockEntity.class, remap = false)
public abstract class MixinSawBlockEntity implements ICrhOwnable {

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

    // Tree cutting (horizontal saw): non-empty logs = treeCut, empty = blockProcessed.
    @WrapOperation(
        method = "onBlockBroken(Lnet/minecraft/world/level/block/state/BlockState;)V",
        at = @At(value = "INVOKE",
            target = "Lcom/simibubi/create/content/kinetics/saw/TreeCutter;findDynamicTree(Lnet/minecraft/world/level/block/Block;Lnet/minecraft/core/BlockPos;)Ljava/util/Optional;")
    )
    private Optional<AbstractBlockBreakQueue> crh$wrapFindDynamicTree(
            Block startBlock, BlockPos pos,
            Operation<Optional<AbstractBlockBreakQueue>> original,
            @Local(argsOnly = true) BlockState stateToBreak) {
        Optional<AbstractBlockBreakQueue> result = original.call(startBlock, pos);
        if (result.isPresent())
            crh$dispatchStationaryCut(pos, stateToBreak, true, -1, -1);
        return result;
    }

    @WrapOperation(
        method = "onBlockBroken(Lnet/minecraft/world/level/block/state/BlockState;)V",
        at = @At(value = "INVOKE",
            target = "Lcom/simibubi/create/content/kinetics/saw/TreeCutter;findTree(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Lcom/simibubi/create/content/kinetics/saw/TreeCutter$Tree;")
    )
    private TreeCutter.Tree crh$wrapFindTree(
            BlockGetter reader, BlockPos pos, BlockState state,
            Operation<TreeCutter.Tree> original) {
        TreeCutter.Tree tree = original.call(reader, pos, state);

        CrhTreeAccessor accessor = (CrhTreeAccessor) tree;
        if (accessor.crh$getLogs().isEmpty()) {
            crh$dispatchStationaryCut(pos, state, false, -1, -1);
        } else {
            crh$dispatchStationaryCut(pos, state, true,
                accessor.crh$getLogs().size(), accessor.crh$getLeaves().size());
        }
        return tree;
    }

    @Unique
    private void crh$dispatchStationaryCut(BlockPos pos, BlockState state, boolean isTree,
                                           int logCount, int leafCount) {
        SawBlockEntity self = (SawBlockEntity)(Object)this;
        Level level = self.getLevel();
        if (level == null || level.isClientSide()) return;

        BlockProcessedContext.Builder builder =
            BlockProcessedContext.of(RecipeSource.MECHANICAL_SAW, level, state)
                .blockPos(pos)
                .contraption(false);

        if (crh$ownerUUID != null)
            builder.meta("createrecipehooks:owner_uuid", crh$ownerUUID.toString());

        if (isTree) {
            builder.treeSize(logCount, leafCount);
            RecipeEventDispatcher.dispatchTreeCut(builder.build());
        } else {
            RecipeEventDispatcher.dispatchBlockProcessed(builder.build());
        }
    }
}
