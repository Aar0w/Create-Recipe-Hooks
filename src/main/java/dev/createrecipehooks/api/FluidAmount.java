package dev.createrecipehooks.api;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Platform-independent snapshot of one fluid output produced by a Create recipe.
 *
 * <p>Avoids a compile-time dependency on Forge/NeoForge's {@code FluidStack} in this
 * common API module. The amount is in milli-buckets (1 bucket = 1000 mB).
 *
 * <h3>Conversion from FluidStack (Forge 1.20.1)</h3>
 * <pre>{@code
 * import net.minecraft.core.registries.BuiltInRegistries;
 * FluidStack fs = ...;
 * FluidAmount fa = new FluidAmount(
 *     BuiltInRegistries.FLUID.getKey(fs.getFluid()),
 *     fs.getAmount()
 * );
 * }</pre>
 *
 * @param fluid  Registry key of the fluid, e.g. {@code minecraft:water}
 * @param amount Amount in milli-buckets; always &gt;= 0
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
