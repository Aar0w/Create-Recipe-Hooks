package dev.createrecipehooks.mixin.millstone;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.kinetics.millstone.MillingRecipe;
import com.simibubi.create.content.kinetics.millstone.MillstoneBlockEntity;
import dev.createrecipehooks.api.RecipeFinishedContext;
import dev.createrecipehooks.api.RecipeSource;
import dev.createrecipehooks.core.RecipeEventDispatcher;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

/**
 * Hook #4 — Millstone.
 * Risk: STABLE
 *
 * <h3>Target</h3>
 * {@code MillstoneBlockEntity.process()} — private void.
 *
 * <h3>Injection point</h3>
 * {@code @WrapOperation} on {@code lastRecipe.rollResults(level.random)}.
 * This call appears exactly once in {@code process()}, so ordinal is not needed.
 *
 * <h3>Why this approach</h3>
 * Field {@code lastRecipe} is of type {@code MillingRecipe} (not RecipeHolder), so the
 * recipe object appears as the receiver of {@code rollResults()}. No @Local or @Accessor needed.
 *
 * <h3>Available data</h3>
 * <ul>
 *   <li>{@code recipe} — MillingRecipe (extends ProcessingRecipe) with id via Recipe.getId()</li>
 *   <li>{@code this} cast to MillstoneBlockEntity → Level, BlockPos</li>
 *   <li>{@code results} — actual rolled output stacks</li>
 * </ul>
 *
 * <h3>Duplicate protection</h3>
 * Millstone does not call RecipeApplier. No overlap.
 */
@Mixin(value = MillstoneBlockEntity.class, remap = false)
public abstract class MixinMillstoneBlockEntity {

    @WrapOperation(
        method = "process()V",
        at = @At(
            value = "INVOKE",
            target = "Lcom/simibubi/create/content/kinetics/millstone/MillingRecipe;" +
                     "rollResults()Ljava/util/List;"
        )
    )
    private List<ItemStack> crh$onMillstoneProcess(
            MillingRecipe recipe,
            Operation<List<ItemStack>> original
    ) {
        List<ItemStack> results = original.call(recipe);

        MillstoneBlockEntity self = (MillstoneBlockEntity)(Object)this;
        Level level = self.getLevel();

        if (level != null && !level.isClientSide() && !results.isEmpty()) {
            RecipeFinishedContext ctx = RecipeFinishedContext.of(RecipeSource.MILLSTONE, level)
                .blockPos(self.getBlockPos())
                .recipe(recipe)
                .itemOutputs(results)
                .build();

            RecipeEventDispatcher.dispatch(ctx);
        }

        return results;
    }
}
