package dev.createrecipehooks.core;

import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Reads the CRH owner UUID out of a contraption actor's serialized block-entity NBT.
 *
 * <p>When a contraption assembles, Create serializes every block entity via
 * {@code saveWithFullMetadata()} into {@code MovementContext.blockEntityData}. Machines
 * whose block entities implement {@code ICrhOwnable} write the placing player's UUID
 * under the {@code crh:owner} key, so the attribution travels with the contraption
 * without any world lookups.
 */
public final class ContraptionOwner {

    /** NBT key written by all ICrhOwnable mixins. */
    public static final String NBT_KEY = "crh:owner";

    /**
     * @param blockEntityData {@code MovementContext.blockEntityData}; may be {@code null}
     *                        for blocks without a block entity
     * @return the owner UUID, or {@code null} when absent
     */
    @Nullable
    public static UUID fromBlockEntityData(@Nullable CompoundTag blockEntityData) {
        if (blockEntityData == null || !blockEntityData.hasUUID(NBT_KEY))
            return null;
        return blockEntityData.getUUID(NBT_KEY);
    }

    private ContraptionOwner() {}
}
