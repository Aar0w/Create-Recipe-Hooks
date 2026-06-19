package dev.createrecipehooks.mixin.drain;

import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.fluids.transfer.GenericItemEmptying;
import dev.createrecipehooks.api.FluidAmount;
import dev.createrecipehooks.api.RecipeFinishedContext;
import dev.createrecipehooks.api.RecipeSource;
import dev.createrecipehooks.core.RecipeEventDispatcher;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Optional;

/**
 * Hook #13 — Item Drain (GenericItemEmptying).
 * Risk: MODERATE
 *
 * <h3>1.20.1 Migration</h3>
 * <ul>
 *   <li>Return type is {@code net.createmod.catnip.data.Pair} (from Create 6.0.8 bytecode).
 *       Rather than casting the Pair, we use {@code @Local} to capture {@code resultingFluid}
 *       (slot 3, FluidStack ordinal 0) and {@code resultingItem} (slot 4, ItemStack ordinal 1)
 *       directly from the method body.</li>
 *   <li>FluidStack: {@code net.minecraftforge.fluids.FluidStack} (NeoForge 1.20.1) ✅</li>
 *   <li>Recipe captured as {@code Optional<? extends Recipe<?>>} (slot 5, Optional ordinal 0).</li>
 * </ul>
 */
@Mixin(value = GenericItemEmptying.class, remap = false)
public abstract class MixinGenericItemEmptying {

    @Inject(
        method = "emptyItem(Lnet/minecraft/world/level/Level;" +
                 "Lnet/minecraft/world/item/ItemStack;Z)" +
                 "Lnet/createmod/catnip/data/Pair;",
        at = @At("RETURN")
    )
    @SuppressWarnings("unchecked")
    private static void crh$onItemEmptied(
            Level level,
            ItemStack stack,
            boolean simulate,
            CallbackInfoReturnable<?> cir,
            @Local(ordinal = 0) FluidStack resultingFluid,
            @Local(ordinal = 1) ItemStack resultingItem,
            @Local(ordinal = 0) Optional<?> recipe
    ) {
        if (simulate) return;
        if (level == null || level.isClientSide()) return;
        if (resultingFluid == null || resultingFluid.isEmpty()) return;

        RecipeFinishedContext.Builder builder = RecipeFinishedContext.of(
                RecipeSource.ITEM_DRAIN_EMPTYING, level)
            .itemInputs(List.of(stack.copy()));

        if (recipe != null && recipe.isPresent()) {
            builder.recipe((Recipe<?>) recipe.get());
        }

        if (resultingItem != null && !resultingItem.isEmpty()) {
            builder.itemOutputs(List.of(resultingItem.copy()));
        }

        ResourceLocation fluidKey = BuiltInRegistries.FLUID.getKey(resultingFluid.getFluid());
        if (fluidKey != null) {
            builder.fluidOutputs(List.of(new FluidAmount(fluidKey, resultingFluid.getAmount())));
        }

        RecipeEventDispatcher.dispatch(builder.build());
    }
}
