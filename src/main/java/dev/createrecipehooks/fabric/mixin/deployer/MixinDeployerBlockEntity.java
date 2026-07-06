package dev.createrecipehooks.fabric.mixin.deployer;

import com.simibubi.create.content.kinetics.deployer.DeployerBlockEntity;
import dev.createrecipehooks.api.ICrhOwnable;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.UUID;

/**
 * Fabric port: exposes Create's built-in {@code protected UUID owner} through
 * {@link ICrhOwnable}. Verified in Create Fabric 6.0.8.1 (field at line 83, NBT key
 * {@code "Owner"} at lines 389/415) — identical to Forge.
 */
@Mixin(DeployerBlockEntity.class)
public abstract class MixinDeployerBlockEntity implements ICrhOwnable {

    @Shadow protected UUID owner;

    @Override
    public @Nullable UUID crh$getOwnerUUID() { return owner; }

    @Override
    public void crh$setOwnerUUID(@Nullable UUID uuid) { this.owner = uuid; }
}
