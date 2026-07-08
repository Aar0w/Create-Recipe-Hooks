package dev.createrecipehooks.core;

import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Reads the CRH owner UUID out of a contraption actor's serialized block-entity NBT.
 *
 * When a contraption assembles, Create serializes every block entity via
 * saveWithFullMetadata() into MovementContext.blockEntityData. Machines
 * whose block entities implement ICrhOwnable write the placing player's UUID
 * under the crh:owner key, so the attribution travels with the contraption
 * without any world lookups.
 */
public final class ContraptionOwner {

    /** NBT key written by all ICrhOwnable mixins. */
    public static final String NBT_KEY = "crh:owner";

    /**
     * @param blockEntityData MovementContext.blockEntityData; may be null
     *                        for blocks without a block entity
     * @return the owner UUID, or null when absent
     */
    @Nullable
    public static UUID fromBlockEntityData(@Nullable CompoundTag blockEntityData) {
        if (blockEntityData == null || !blockEntityData.hasUUID(NBT_KEY))
            return null;
        return blockEntityData.getUUID(NBT_KEY);
    }

    private ContraptionOwner() {}
}
