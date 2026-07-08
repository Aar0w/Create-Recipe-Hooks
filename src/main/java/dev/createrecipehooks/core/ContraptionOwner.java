package dev.createrecipehooks.core;

import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Reads the owner UUID from a contraption actor's saved block entity NBT, so
 * attribution keeps working while the machine is moving.
 */
public final class ContraptionOwner {

    /** NBT key written by all ICrhOwnable mixins. */
    public static final String NBT_KEY = "crh:owner";

    /** Returns the owner UUID from MovementContext.blockEntityData, or null. */
    @Nullable
    public static UUID fromBlockEntityData(@Nullable CompoundTag blockEntityData) {
        if (blockEntityData == null || !blockEntityData.hasUUID(NBT_KEY))
            return null;
        return blockEntityData.getUUID(NBT_KEY);
    }

    private ContraptionOwner() {}
}
