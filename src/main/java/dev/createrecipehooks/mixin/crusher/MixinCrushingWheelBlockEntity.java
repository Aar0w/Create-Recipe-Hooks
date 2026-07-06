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

/**
 * Adds {@link ICrhOwnable} UUID tracking to the crushing wheel itself (the player-placed
 * block), so that {@link MixinCrushingWheelController} can fall back to the wheel owner
 * when the crushed item carries no thrower UUID (belt/hopper-fed automation).
 *
 * <h3>Why overrides instead of the usual write/read injections</h3>
 * {@code CrushingWheelBlockEntity} does not declare {@code write}/{@code read} (verified
 * via javap of Create Forge 6.0.8) — they are inherited from {@link KineticBlockEntity}.
 * A {@code @Inject} can only target declared methods, so this mixin extends the superclass
 * and merges real overrides into the target instead.
 */
@Mixin(value = CrushingWheelBlockEntity.class, remap = false)
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
