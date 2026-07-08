package dev.createrecipehooks.mixin.spout;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.fluids.spout.FillingBySpout;
import com.simibubi.create.content.fluids.transfer.FillingRecipe;
import dev.createrecipehooks.api.RecipeFinishedContext;
import dev.createrecipehooks.api.RecipeSource;
import dev.createrecipehooks.core.RecipeEventDispatcher;
import dev.createrecipehooks.internal.CrhOwnerContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;
import java.util.UUID;

// Fires the SPOUT_FILLING event, covering both filling paths: FillingRecipe results
// (with recipe id) and fluid capability containers such as buckets (recipe id null).
@Mixin(value = FillingBySpout.class, remap = false)
public abstract class MixinFillingBySpout {

    @WrapOperation(
        method = "fillItem(Lnet/minecraft/world/level/Level;" +
                 "ILnet/minecraft/world/item/ItemStack;" +
                 "Lnet/minecraftforge/fluids/FluidStack;)Lnet/minecraft/world/item/ItemStack;",
        at = @At(
            value = "INVOKE",
            target = "Lcom/simibubi/create/content/fluids/transfer/FillingRecipe;" +
                     "rollResults()Ljava/util/List;"
        )
    )
    private static List<ItemStack> crh$onFillingBySpout(
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

    // Capability path: no FillingRecipe involved, dispatched with recipeId null.
    @WrapOperation(
        method = "fillItem(Lnet/minecraft/world/level/Level;" +
                 "ILnet/minecraft/world/item/ItemStack;" +
                 "Lnet/minecraftforge/fluids/FluidStack;)Lnet/minecraft/world/item/ItemStack;",
        at = @At(
            value = "INVOKE",
            target = "Lcom/simibubi/create/content/fluids/transfer/GenericItemFilling;" +
                     "fillItem(Lnet/minecraft/world/level/Level;" +
                     "ILnet/minecraft/world/item/ItemStack;" +
                     "Lnet/minecraftforge/fluids/FluidStack;)Lnet/minecraft/world/item/ItemStack;"
        )
    )
    private static ItemStack crh$onCapabilityFilling(
            Level level,
            int requiredAmount,
            ItemStack stack,
            net.minecraftforge.fluids.FluidStack fluid,
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
