package dev.createrecipehooks.mixin.deployer;

import com.simibubi.create.content.kinetics.deployer.DeployerBlockEntity;
import dev.createrecipehooks.api.ICrhOwnable;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.UUID;

/**
 * Exposes Create's built-in Deployer owner field through ICrhOwnable.
 * Create persists it itself, so no NBT handling is needed here.
 */
@Mixin(value = DeployerBlockEntity.class, remap = false)
public abstract class MixinDeployerBlockEntity implements ICrhOwnable {

    @Shadow protected UUID owner;

    @Override
    public @Nullable UUID crh$getOwnerUUID() { return owner; }

    @Override
    public void crh$setOwnerUUID(@Nullable UUID uuid) { this.owner = uuid; }
}
