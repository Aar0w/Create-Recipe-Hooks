package dev.createrecipehooks.mixin.drain;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.fluids.transfer.EmptyingRecipe;
import com.simibubi.create.content.fluids.transfer.GenericItemEmptying;
import dev.createrecipehooks.api.FluidAmount;
import dev.createrecipehooks.api.RecipeFinishedContext;
import dev.createrecipehooks.api.RecipeSource;
import dev.createrecipehooks.core.RecipeEventDispatcher;
import dev.createrecipehooks.internal.CrhOwnerContext;
import net.createmod.catnip.data.Pair;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.UUID;

/**
 * Hook #13 — Item Drain (GenericItemEmptying). Return-value based design,
 * backported from the Fabric port.
 *
 * <h3>Why not @Local captures (the original design)</h3>
 * {@code emptyItem} has an early potion return (bytecode offset 18-21, verified via javap
 * of Create Forge 6.0.8) that fires before the recipe {@code Optional} local is declared.
 * The original {@code @Local Optional} capture at RETURN silently depended on transform-time
 * luck at that return; reading {@code cir.getReturnValue()} is valid at every RETURN by
 * definition and covers all three paths uniformly:
 * <ul>
 *   <li>recipe path (EmptyingRecipe) — recipe attached via the rollResults WrapOperation</li>
 *   <li>capability path (buckets) — no recipe, fluid+item from the Pair</li>
 *   <li>potion path (PotionFluidHandler.emptyPotion) — NOW fires events (the local-based
 *       design skipped potions entirely)</li>
 * </ul>
 */
@Mixin(value = GenericItemEmptying.class, remap = false)
public abstract class MixinGenericItemEmptying {

    private static final ThreadLocal<EmptyingRecipe> CAPTURED_RECIPE = new ThreadLocal<>();

    @Inject(
        method = "emptyItem(Lnet/minecraft/world/level/Level;" +
                 "Lnet/minecraft/world/item/ItemStack;Z)" +
                 "Lnet/createmod/catnip/data/Pair;",
        at = @At("HEAD")
    )
    private static void crh$clearCapture(
            Level level, ItemStack stack, boolean simulate,
            CallbackInfoReturnable<Pair<FluidStack, ItemStack>> cir
    ) {
        CAPTURED_RECIPE.remove();
    }

    @WrapOperation(
        method = "emptyItem(Lnet/minecraft/world/level/Level;" +
                 "Lnet/minecraft/world/item/ItemStack;Z)" +
                 "Lnet/createmod/catnip/data/Pair;",
        at = @At(
            value = "INVOKE",
            target = "Lcom/simibubi/create/content/fluids/transfer/EmptyingRecipe;" +
                     "rollResults()Ljava/util/List;"
        )
    )
    private static List<ItemStack> crh$captureEmptyingRecipe(
            EmptyingRecipe recipe,
            Operation<List<ItemStack>> original
    ) {
        CAPTURED_RECIPE.set(recipe);
        return original.call(recipe);
    }

    @Inject(
        method = "emptyItem(Lnet/minecraft/world/level/Level;" +
                 "Lnet/minecraft/world/item/ItemStack;Z)" +
                 "Lnet/createmod/catnip/data/Pair;",
        at = @At("RETURN")
    )
    private static void crh$onItemEmptied(
            Level level,
            ItemStack stack,
            boolean simulate,
            CallbackInfoReturnable<Pair<FluidStack, ItemStack>> cir
    ) {
        try {
            if (simulate) return;
            if (level == null || level.isClientSide()) return;

            Pair<FluidStack, ItemStack> result = cir.getReturnValue();
            if (result == null) return;

            FluidStack resultingFluid = result.getFirst();
            ItemStack resultingItem = result.getSecond();
            if (resultingFluid == null || resultingFluid.isEmpty()) return;

            RecipeFinishedContext.Builder builder = RecipeFinishedContext.of(
                    RecipeSource.ITEM_DRAIN_EMPTYING, level)
                .itemInputs(List.of(stack.copy()));

            EmptyingRecipe recipe = CAPTURED_RECIPE.get();
            if (recipe != null) {
                builder.recipe(recipe);
            }

            if (resultingItem != null && !resultingItem.isEmpty()) {
                builder.itemOutputs(List.of(resultingItem.copy()));
            }

            ResourceLocation fluidKey = BuiltInRegistries.FLUID.getKey(resultingFluid.getFluid());
            if (fluidKey != null) {
                builder.fluidOutputs(List.of(new FluidAmount(fluidKey, resultingFluid.getAmount())));
            }

            UUID ownerUUID = CrhOwnerContext.get();
            if (ownerUUID != null) builder.meta("createrecipehooks:owner_uuid", ownerUUID.toString());

            RecipeEventDispatcher.dispatch(builder.build());
        } finally {
            CAPTURED_RECIPE.remove();
        }
    }
}
