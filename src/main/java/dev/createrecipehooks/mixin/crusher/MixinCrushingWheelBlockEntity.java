package dev.createrecipehooks.mixin.crusher;

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

// Owner tracking for the Crushing Wheel block, used by MixinCrushingWheelController as the attribution fallback for belt and hopper fed input.
// The wheel block entity does not declare its own NBT methods, so overrides are merged in via the superclass.
@Mixin(value = CrushingWheelBlockEntity.class, remap = false)
public abstract class MixinCrushingWheelBlockEntity extends KineticBlockEntity implements ICrhOwnable {

    @Unique private @Nullable UUID crh$ownerUUID = null;

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
