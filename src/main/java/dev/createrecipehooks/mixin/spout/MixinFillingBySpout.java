package dev.createrecipehooks.mixin.spout;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.fluids.spout.FillingBySpout;
import com.simibubi.create.content.fluids.transfer.FillingRecipe;
import dev.createrecipehooks.api.RecipeFinishedContext;
import dev.createrecipehooks.api.RecipeSource;
import dev.createrecipehooks.core.RecipeEventDispatcher;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

/**
 * Hook #12 — Spout filling (FillingBySpout / FillingRecipe).
 * Risk: MODERATE
 *
 * <h3>1.20.1 Migration</h3>
 * <ul>
 *   <li>{@code net.neoforged.neoforge.fluids.FluidStack} →
 *       {@code net.minecraftforge.fluids.FluidStack}</li>
 *   <li>Method descriptor updated: Forge 1.20.1 FluidStack descriptor
 *       = {@code Lnet/minecraftforge/fluids/FluidStack;}</li>
 *   <li>{@code RecipeHolder<FillingRecipe> fillingRecipe} →
 *       local variable is {@code Recipe<?> fillingRecipe} or
 *       {@code FillingRecipe fillingRecipe} in Create 0.5.1.f (no RecipeHolder wrapper).
 *       Captured as {@code @Local Recipe<?>}.</li>
 *   <li>Recipe id accessed via {@code recipe.getId()} (exists in 1.20.1).</li>
 * </ul>
 */
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
            RecipeFinishedContext ctx = RecipeFinishedContext.of(RecipeSource.SPOUT_FILLING, level)
                .recipe(recipe)
                .recipeId(recipe.getId())
                .itemOutputs(results)
                .itemInputs(List.of(stack.copy()))
                .build();

            RecipeEventDispatcher.dispatch(ctx);
        }

        return results;
    }
}
