package dev.createrecipehooks.mixin.harvester;

import com.simibubi.create.content.contraptions.actors.harvester.HarvesterBlockEntity;
import com.simibubi.create.foundation.blockEntity.CachedRenderBBBlockEntity;
import dev.createrecipehooks.api.ICrhOwnable;
import dev.createrecipehooks.core.ContraptionOwner;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.UUID;

// Owner tracking for the Mechanical Harvester, persisted through the vanilla NBT pair so the UUID travels inside assembled contraptions and is read back by MixinHarvesterMovementBehaviour.
@Mixin(value = HarvesterBlockEntity.class, remap = false)
public abstract class MixinHarvesterBlockEntity extends CachedRenderBBBlockEntity implements ICrhOwnable {

    @Unique private @Nullable UUID crh$ownerUUID = null;

    // Never invoked, required only so javac accepts the superclass extension.
    private MixinHarvesterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override public @Nullable UUID crh$getOwnerUUID() { return crh$ownerUUID; }
    @Override public void crh$setOwnerUUID(@Nullable UUID uuid) { this.crh$ownerUUID = uuid; }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (crh$ownerUUID != null)
            tag.putUUID(ContraptionOwner.NBT_KEY, crh$ownerUUID);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.hasUUID(ContraptionOwner.NBT_KEY))
            crh$ownerUUID = tag.getUUID(ContraptionOwner.NBT_KEY);
    }
}
