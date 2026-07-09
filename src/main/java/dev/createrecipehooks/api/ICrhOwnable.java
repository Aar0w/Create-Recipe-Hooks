package dev.createrecipehooks.api;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;

// Implemented (via mixin) on every Create block entity that CRH tracks ownership for.
// Cast the block entity to this interface to read the UUID of the player who placed the machine; null when it was placed by a piston, command or other non-player.
public interface ICrhOwnable {
    @Nullable UUID crh$getOwnerUUID();
    void crh$setOwnerUUID(@Nullable UUID uuid);
}
