package dev.createrecipehooks.api;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Loader-independent snapshot of one fluid output, so the common API has no dependency
 * on a platform FluidStack class.
 *
 * @param fluid  Registry key of the fluid, e.g. minecraft:water
 * @param amount Amount in milli-buckets (1 bucket = 1000 mB); always >= 0
 */
public record FluidAmount(@NotNull ResourceLocation fluid, int amount) {

    public FluidAmount {
        Objects.requireNonNull(fluid, "fluid must not be null");
        if (amount < 0) throw new IllegalArgumentException("amount must be >= 0, got " + amount);
    }

    @Override
    public String toString() {
        return amount + "mB " + fluid;
    }
}
