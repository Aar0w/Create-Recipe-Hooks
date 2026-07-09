package dev.createrecipehooks.fabric.mixin.saw;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.kinetics.saw.SawMovementBehaviour;
import com.simibubi.create.content.kinetics.saw.TreeCutter;
import com.simibubi.create.foundation.utility.AbstractBlockBreakQueue;
import dev.createrecipehooks.api.BlockProcessedContext;
import dev.createrecipehooks.api.RecipeSource;
import dev.createrecipehooks.core.ContraptionOwner;
import dev.createrecipehooks.core.RecipeEventDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Optional;
import java.util.UUID;

// Mechanical Saw as a contraption actor: fires treeCut when the cut block is part of a tree (with exact log and leaf counts) and blockProcessed for a lone block.
// Dynamic Trees mod and others that modifies trees fire treeCut with counts of -1. (tested with only Dinamic Trees tho)
@Mixin(SawMovementBehaviour.class)
public class MixinSawMovementBehaviour {

    @WrapOperation(
        method = "onBlockBroken(Lcom/simibubi/create/content/contraptions/behaviour/MovementContext;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V",
        at = @At(value = "INVOKE",
            target = "Lcom/simibubi/create/content/kinetics/saw/TreeCutter;findDynamicTree(Lnet/minecraft/world/level/block/Block;Lnet/minecraft/core/BlockPos;)Ljava/util/Optional;")
    )
    private Optional<AbstractBlockBreakQueue> crh$wrapFindDynamicTree(
            Block startBlock, BlockPos pos,
            Operation<Optional<AbstractBlockBreakQueue>> original,
            @Local(argsOnly = true) MovementContext context,
            @Local(argsOnly = true) BlockState brokenState) {
        Optional<AbstractBlockBreakQueue> result = original.call(startBlock, pos);
        if (result.isPresent())
            crh$dispatchTree(context, pos, brokenState, -1, -1);
        return result;
    }

    @WrapOperation(
        method = "onBlockBroken(Lcom/simibubi/create/content/contraptions/behaviour/MovementContext;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V",
        at = @At(value = "INVOKE",
            target = "Lcom/simibubi/create/content/kinetics/saw/TreeCutter;findTree(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Lcom/simibubi/create/content/kinetics/saw/TreeCutter$Tree;")
    )
    private TreeCutter.Tree crh$wrapFindTree(
            BlockGetter reader, BlockPos pos, BlockState state,
            Operation<TreeCutter.Tree> original,
            @Local(argsOnly = true) MovementContext context) {
        TreeCutter.Tree tree = original.call(reader, pos, state);

        CrhTreeAccessor accessor = (CrhTreeAccessor) tree;
        if (accessor.crh$getLogs().isEmpty()) {
            crh$dispatchLoneBlock(context, pos, state);
        } else {
            crh$dispatchTree(context, pos, state, accessor.crh$getLogs().size(), accessor.crh$getLeaves().size());
        }
        return tree;
    }

    @Unique
    private static void crh$dispatchTree(MovementContext context, BlockPos pos, BlockState state,
                                         int logCount, int leafCount) {
        Level level = context.world;
        if (level == null || level.isClientSide()) return;

        BlockProcessedContext.Builder builder =
            BlockProcessedContext.of(RecipeSource.MECHANICAL_SAW, level, state)
                .blockPos(pos)
                .contraption(true)
                .treeSize(logCount, leafCount);

        UUID owner = ContraptionOwner.fromBlockEntityData(context.blockEntityData);
        if (owner != null)
            builder.meta("createrecipehooks:owner_uuid", owner.toString());

        RecipeEventDispatcher.dispatchTreeCut(builder.build());
    }

    @Unique
    private static void crh$dispatchLoneBlock(MovementContext context, BlockPos pos, BlockState state) {
        Level level = context.world;
        if (level == null || level.isClientSide()) return;

        BlockProcessedContext.Builder builder =
            BlockProcessedContext.of(RecipeSource.MECHANICAL_SAW, level, state)
                .blockPos(pos)
                .contraption(true);

        UUID owner = ContraptionOwner.fromBlockEntityData(context.blockEntityData);
        if (owner != null)
            builder.meta("createrecipehooks:owner_uuid", owner.toString());

        RecipeEventDispatcher.dispatchBlockProcessed(builder.build());
    }
}
