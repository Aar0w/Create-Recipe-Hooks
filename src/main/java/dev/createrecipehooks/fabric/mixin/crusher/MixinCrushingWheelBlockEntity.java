package dev.createrecipehooks.fabric.mixin.crusher;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.crusher.CrushingWheelBlockEntity;
import dev.createrecipehooks.api.ICrhOwnable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.UUID;

/**
 * Fabric port of the Forge {@code MixinCrushingWheelBlockEntity}: {@link ICrhOwnable}
 * UUID tracking on the player-placed wheel, used as the attribution fallback for
 * belt/hopper-fed crushing.
 *
 * <p>{@code CrushingWheelBlockEntity} does not declare {@code write}/{@code read}
 * (verified in Create Fabric 6.0.8.1 sources), so this mixin extends
 * {@link KineticBlockEntity} and merges real overrides into the target.
 *
 * <p>Owner capture on placement happens automatically via the {@code MixinBlockItem}
 * hook (instanceof {@link ICrhOwnable}); no per-block registration is needed on Fabric.
 */
@Mixin(CrushingWheelBlockEntity.class)
public abstract class MixinCrushingWheelBlockEntity extends KineticBlockEntity implements ICrhOwnable {

    @Unique private @Nullable UUID crh$ownerUUID = null;

    // Never invoked — required only so javac accepts the superclass extension.
    private MixinCrushingWheelBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override public @Nullable UUID crh$getOwnerUUID() { return crh$ownerUUID; }
    @Override public void crh$setOwnerUUID(@Nullable UUID uuid) { this.crh$ownerUUID = uuid; }

    @Override
    protected void write(CompoundTag tag, boolean clientPacket) {
        super.write(tag, clientPacket);
        if (!clientPacket && crh$ownerUUID != null)
            tag.putUUID("crh:owner", crh$ownerUUID);
    }

    @Override
    protected void read(CompoundTag tag, boolean clientPacket) {
        super.read(tag, clientPacket);
        if (!clientPacket && tag.hasUUID("crh:owner"))
            crh$ownerUUID = tag.getUUID("crh:owner");
    }
}
