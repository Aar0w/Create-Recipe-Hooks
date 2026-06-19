package dev.createrecipehooks.mixin.basin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.basin.BasinRecipe;
import dev.createrecipehooks.api.FluidAmount;
import dev.createrecipehooks.api.RecipeFinishedContext;
import dev.createrecipehooks.api.RecipeSource;
import dev.createrecipehooks.core.RecipeEventDispatcher;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraftforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;
import java.util.Objects;

/**
 * Hook #1 — Basin / Mixer / Compactor / Pressing-on-Basin.
 * Risk: STABLE
 *
 * <h3>Target</h3>
 * {@code BasinRecipe.apply(BasinBlockEntity, Recipe<?>, boolean test)} — private static.
 *
 * <h3>Injection point</h3>
 * {@code @WrapOperation} on {@code basin.acceptOutputs(recipeOutputItems, recipeOutputFluids, simulate)}.
 *
 * {@code apply()} loops over {@code Iterate.trueAndFalse}:
 * <ol>
 *   <li>First iteration: {@code simulate=true} — calculate outputs, check capacity.</li>
 *   <li>Second iteration: {@code simulate=false} — consume inputs, commit outputs.</li>
 * </ol>
 * We fire only on {@code simulate=false} AND {@code test=false} (not a match-check call).
 *
 * <h3>Available data</h3>
 * <ul>
 *   <li>{@code recipe} — full Recipe object with id (parameter of apply)</li>
 *   <li>{@code basin} — BasinBlockEntity → Level, BlockPos</li>
 *   <li>{@code itemOutputs} — List&lt;ItemStack&gt; (arguments to acceptOutputs)</li>
 *   <li>{@code fluidOutputs} — List&lt;FluidStack&gt; (arguments to acceptOutputs)</li>
 * </ul>
 *
 * <h3>Duplicate protection</h3>
 * Basin does not go through RecipeApplier. No overlap with MixinRecipeApplier.
 *
 * <h3>Addons covered</h3>
 * <ul>
 *   <li>CEI Infuser (calls BasinRecipe.apply via BasinOperatingBlockEntity.applyBasinRecipe)</li>
 *   <li>Steam 'n' Rails (MixinBasinRecipe patches apply, our hook fires after it)</li>
 *   <li>PowerGrid (BasinRecipeMixin patches apply for NBT transfer, our hook fires after)</li>
 * </ul>
 */
@Mixin(value = BasinRecipe.class, remap = false)
public abstract class MixinBasinRecipe {

    @WrapOperation(
        method = "apply(Lcom/simibubi/create/content/processing/basin/BasinBlockEntity;" +
                 "Lnet/minecraft/world/item/crafting/Recipe;Z)Z",
        at = @At(
            value = "INVOKE",
            target = "Lcom/simibubi/create/content/processing/basin/BasinBlockEntity;" +
                     "acceptOutputs(Ljava/util/List;Ljava/util/List;Z)Z"
        )
    )
    private static boolean crh$onBasinAcceptOutputs(
            BasinBlockEntity basin,
            List<ItemStack> itemOutputs,
            List<FluidStack> fluidOutputs,
            boolean simulate,
            Operation<Boolean> original,
            // Captured from outer apply() parameters via @Local(argsOnly=true)
            @Local(argsOnly = true) Recipe<?> recipe,
            @Local(argsOnly = true) boolean test
    ) {
        boolean accepted = original.call(basin, itemOutputs, fluidOutputs, simulate);

        // Fire only on the real application pass (not simulate, not match-check)
        if (accepted && !simulate && !test
                && basin.getLevel() != null
                && !basin.getLevel().isClientSide()) {

            List<FluidAmount> convertedFluids = fluidOutputs.stream()
                .filter(fs -> !fs.isEmpty())
                .map(fs -> {
                    ResourceLocation key = BuiltInRegistries.FLUID.getKey(fs.getFluid());
                    return key != null ? new FluidAmount(key, fs.getAmount()) : null;
                })
                .filter(Objects::nonNull)
                .toList();

            RecipeFinishedContext ctx = RecipeFinishedContext.of(RecipeSource.BASIN, basin.getLevel())
                .blockPos(basin.getBlockPos())
                .recipe(recipe)
                .itemOutputs(itemOutputs)
                .fluidOutputs(convertedFluids)
                .build();

            RecipeEventDispatcher.dispatch(ctx);
        }

        return accepted;
    }
}
