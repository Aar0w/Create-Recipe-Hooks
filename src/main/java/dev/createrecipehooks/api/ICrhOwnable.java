package dev.createrecipehooks.api;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Implemented (via Mixin) on any Create BlockEntity that CRH tracks ownership for.
 *
 * <p>Addon mods can cast a {@code BasinBlockEntity} to this interface to read the UUID
 * of the player who placed the machine:
 * <pre>{@code
 * if (basin instanceof ICrhOwnable ownable) {
 *     UUID owner = ownable.crh$getOwnerUUID(); // null if placed by piston/command
 * }
 * }</pre>
 */
public interface ICrhOwnable {
    @Nullable UUID crh$getOwnerUUID();
    void crh$setOwnerUUID(@Nullable UUID uuid);
}
