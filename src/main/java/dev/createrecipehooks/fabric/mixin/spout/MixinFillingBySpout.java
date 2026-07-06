package dev.createrecipehooks.fabric.mixin.spout;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.fluids.spout.FillingBySpout;
import com.simibubi.create.content.fluids.transfer.FillingRecipe;
import dev.createrecipehooks.api.RecipeFinishedContext;
import dev.createrecipehooks.api.RecipeSource;
import dev.createrecipehooks.core.RecipeEventDispatcher;
import dev.createrecipehooks.internal.CrhOwnerContext;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;
import java.util.UUID;

/**
 * Fabric port of the Forge {@code MixinFillingBySpout} — both filling paths.
 *
 * <h3>Differences from Forge (verified against Create Fabric 6.0.8.1 sources)</h3>
 * <ul>
 *   <li>{@code fillItem(Level, long, ItemStack, FluidStack)} — the amount is a
 *       {@code long} in droplets, and FluidStack is Porting Lib's.</li>
 *   <li>Both paths confirmed present: {@code FillingRecipe.rollResults()} (recipe path)
 *       and the {@code GenericItemFilling.fillItem} fallback (capability path, buckets).</li>
 * </ul>
 */
@Mixin(FillingBySpout.class)
public abstract class MixinFillingBySpout {

    // ── Recipe path ───────────────────────────────────────────────────────────

    @WrapOperation(
        method = "fillItem(Lnet/minecraft/world/level/Level;" +
                 "JLnet/minecraft/world/item/ItemStack;" +
                 "Lio/github/fabricators_of_create/porting_lib/fluids/FluidStack;)" +
                 "Lnet/minecraft/world/item/ItemStack;",
        at = @At(
            value = "INVOKE",
            target = "Lcom/simibubi/create/content/fluids/transfer/FillingRecipe;" +
                     "rollResults()Ljava/util/List;"
        )
    )
    private static List<ItemStack> crh$onRecipeFilling(
            FillingRecipe recipe,
            Operation<List<ItemStack>> original,
            @Local(argsOnly = true) Level level,
            @Local(argsOnly = true) ItemStack stack
    ) {
        List<ItemStack> results = original.call(recipe);

        if (level != null && !level.isClientSide() && !results.isEmpty()) {
            RecipeFinishedContext.Builder builder = RecipeFinishedContext.of(RecipeSource.SPOUT_FILLING, level)
                .recipe(recipe)
                .recipeId(recipe.getId())
                .itemOutputs(results)
                .itemInputs(List.of(stack.copy()));

            UUID ownerUUID = CrhOwnerContext.get();
            if (ownerUUID != null) builder.meta("createrecipehooks:owner_uuid", ownerUUID.toString());

            RecipeEventDispatcher.dispatch(builder.build());
        }

        return results;
    }

    // ── Capability path (buckets and other fluid containers; recipeId = null) ─

    @WrapOperation(
        method = "fillItem(Lnet/minecraft/world/level/Level;" +
                 "JLnet/minecraft/world/item/ItemStack;" +
                 "Lio/github/fabricators_of_create/porting_lib/fluids/FluidStack;)" +
                 "Lnet/minecraft/world/item/ItemStack;",
        at = @At(
            value = "INVOKE",
            target = "Lcom/simibubi/create/content/fluids/transfer/GenericItemFilling;" +
                     "fillItem(Lnet/minecraft/world/level/Level;" +
                     "JLnet/minecraft/world/item/ItemStack;" +
                     "Lio/github/fabricators_of_create/porting_lib/fluids/FluidStack;)" +
                     "Lnet/minecraft/world/item/ItemStack;"
        )
    )
    private static ItemStack crh$onCapabilityFilling(
            Level level,
            long requiredAmount,
            ItemStack stack,
            FluidStack fluid,
            Operation<ItemStack> original
    ) {
        ItemStack input = stack.copy();
        ItemStack result = original.call(level, requiredAmount, stack, fluid);

        if (level != null && !level.isClientSide() && result != null && !result.isEmpty()) {
            RecipeFinishedContext.Builder builder = RecipeFinishedContext.of(RecipeSource.SPOUT_FILLING, level)
                .itemOutputs(List.of(result.copy()))
                .itemInputs(List.of(input));

            UUID ownerUUID = CrhOwnerContext.get();
            if (ownerUUID != null) builder.meta("createrecipehooks:owner_uuid", ownerUUID.toString());

            RecipeEventDispatcher.dispatch(builder.build());
        }

        return result;
    }
}
