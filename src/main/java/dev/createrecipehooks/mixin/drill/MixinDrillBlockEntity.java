package dev.createrecipehooks.mixin.drill;

import com.simibubi.create.content.kinetics.base.BlockBreakingKineticBlockEntity;
import com.simibubi.create.content.kinetics.drill.DrillBlockEntity;
import dev.createrecipehooks.api.BlockProcessedContext;
import dev.createrecipehooks.api.ICrhOwnable;
import dev.createrecipehooks.api.RecipeSource;
import dev.createrecipehooks.core.ContraptionOwner;
import dev.createrecipehooks.core.RecipeEventDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

/**
 * Stationary Mechanical Drill: fires a blockProcessed event once per broken block and
 * tracks the placing player for attribution.
 *
 * <p>{@code DrillBlockEntity} does not declare {@code write}/{@code read}, so this mixin
 * extends {@code BlockBreakingKineticBlockEntity} (which does) and contributes real
 * overrides — same pattern as {@code MixinCrushingWheelBlockEntity}.
 *
 * <p>The event hook targets {@code DrillBlockEntity.onBlockBroken} at HEAD: this covers
 * both the normal drop path ({@code super.onBlockBroken}) and the cobblegen-optimised
 * path ({@code optimiseCobbleGen} feeding a belt/hopper/chute directly), which skips the
 * super call entirely.
 */
@Mixin(value = DrillBlockEntity.class, remap = false)
public abstract class MixinDrillBlockEntity extends BlockBreakingKineticBlockEntity implements ICrhOwnable {

    @Unique private @Nullable UUID crh$ownerUUID = null;

    // Never invoked — required only so javac accepts the superclass extension.
    private MixinDrillBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override public @Nullable UUID crh$getOwnerUUID() { return crh$ownerUUID; }
    @Override public void crh$setOwnerUUID(@Nullable UUID uuid) { this.crh$ownerUUID = uuid; }

    @Override
    public void write(CompoundTag tag, boolean clientPacket) {
        super.write(tag, clientPacket);
        if (!clientPacket && crh$ownerUUID != null)
            tag.putUUID(ContraptionOwner.NBT_KEY, crh$ownerUUID);
    }

    @Override
    protected void read(CompoundTag tag, boolean clientPacket) {
        super.read(tag, clientPacket);
        if (!clientPacket && tag.hasUUID(ContraptionOwner.NBT_KEY))
            crh$ownerUUID = tag.getUUID(ContraptionOwner.NBT_KEY);
    }

    @Inject(
        method = "onBlockBroken(Lnet/minecraft/world/level/block/state/BlockState;)V",
        at = @At("HEAD")
    )
    private void crh$onDrillBlockBroken(BlockState stateToBreak, CallbackInfo ci) {
        Level level = getLevel();
        if (level == null || level.isClientSide()) return;

        BlockProcessedContext.Builder builder =
            BlockProcessedContext.of(RecipeSource.MECHANICAL_DRILL, level, stateToBreak)
                .blockPos(breakingPos)
                .contraption(false);

        if (crh$ownerUUID != null)
            builder.meta("createrecipehooks:owner_uuid", crh$ownerUUID.toString());

        RecipeEventDispatcher.dispatchBlockProcessed(builder.build());
    }
}
