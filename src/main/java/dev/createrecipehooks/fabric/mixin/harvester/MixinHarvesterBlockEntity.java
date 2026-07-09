package dev.createrecipehooks.fabric.mixin.harvester;

import com.simibubi.create.content.contraptions.actors.harvester.HarvesterBlockEntity;
import com.simibubi.create.foundation.blockEntity.CachedRenderBBBlockEntity;
import dev.createrecipehooks.api.ICrhOwnable;
import dev.createrecipehooks.core.ContraptionOwner;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.UUID;

// Owner tracking for the Mechanical Harvester, persisted through the vanilla NBT pair so the UUID travels inside assembled contraptions and is read back by MixinHarvesterMovementBehaviour.
@Mixin(HarvesterBlockEntity.class)
public abstract class MixinHarvesterBlockEntity extends CachedRenderBBBlockEntity implements ICrhOwnable {

    @Unique private @Nullable UUID crh$ownerUUID = null;

    // Never invoked, required only so javac accepts the superclass extension.
    private MixinHarvesterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override public @Nullable UUID crh$getOwnerUUID() { return crh$ownerUUID; }
    @Override public void crh$setOwnerUUID(@Nullable UUID uuid) { this.crh$ownerUUID = uuid; }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (crh$ownerUUID != null)
            tag.putUUID(ContraptionOwner.NBT_KEY, crh$ownerUUID);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.hasUUID(ContraptionOwner.NBT_KEY))
            crh$ownerUUID = tag.getUUID(ContraptionOwner.NBT_KEY);
    }
}
