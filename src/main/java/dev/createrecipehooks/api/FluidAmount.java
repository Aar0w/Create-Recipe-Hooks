package dev.createrecipehooks.api;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * One fluid output: the fluid's registry key (e.g. minecraft:water) and the amount in
 * milli-buckets. Keeps the common API free of a platform FluidStack dependency.
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
