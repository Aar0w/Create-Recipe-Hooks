package dev.createrecipehooks.mixin.deployer;

import com.simibubi.create.content.kinetics.deployer.DeployerBlockEntity;
import dev.createrecipehooks.api.ICrhOwnable;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.UUID;

/**
 * Exposes Create's built-in {@code protected UUID owner} (set by
 * {@code DeployerBlock.setPlacedBy}) through {@link ICrhOwnable}.
 *
 * <p>No NBT injection needed — Create already persists {@code owner} under key
 * {@code "Owner"}. No CrhOwnerContext injection needed — the UUID is read directly
 * from {@code deployer instanceof ICrhOwnable} inside
 * {@link MixinBeltDeployerCallbacks#crh$onDeployerActivated}.
 */
@Mixin(value = DeployerBlockEntity.class, remap = false)
public abstract class MixinDeployerBlockEntity implements ICrhOwnable {

    @Shadow protected UUID owner;

    @Override
    public @Nullable UUID crh$getOwnerUUID() { return owner; }

    @Override
    public void crh$setOwnerUUID(@Nullable UUID uuid) { this.owner = uuid; }
}
