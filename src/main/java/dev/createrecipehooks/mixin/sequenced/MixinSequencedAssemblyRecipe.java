package dev.createrecipehooks.mixin.sequenced;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
import dev.createrecipehooks.api.RecipeFinishedContext;
import dev.createrecipehooks.core.SequencedAssemblyLevelCapture;
import dev.createrecipehooks.api.RecipeSource;
import dev.createrecipehooks.core.RecipeEventDispatcher;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

/**
 * Hook #10 — Sequenced Assembly final step.
 * Risk: MODERATE (ThreadLocal dependency)
 *
 * <h3>1.20.1 Migration</h3>
 * No changes to this mixin's code — the {@code advance(ResourceLocation, ItemStack, RandomSource)}
 * and {@code rollResult(RandomSource)} signatures exist in Create 0.5.1.f for 1.20.1.
 * The DataComponents reference is in {@link MixinRecipeApplier} (deduplication),
 * not in this class.
 *
 * <p>The ThreadLocal Level capture pattern remains identical.
 */
@Mixin(value = SequencedAssemblyRecipe.class, remap = false)
public abstract class MixinSequencedAssemblyRecipe {

    @WrapOperation(
        method = "advance(Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/ItemStack;",
        at = @At(
            value = "INVOKE",
            target = "Lcom/simibubi/create/content/processing/sequenced/SequencedAssemblyRecipe;" +
                     "rollResult()Lnet/minecraft/world/item/ItemStack;"
        )
    )
    private ItemStack crh$onSequencedAssemblyFinished(
            SequencedAssemblyRecipe self,
            Operation<ItemStack> original,
            @Local(argsOnly = true) ItemStack input
    ) {
        // Capture before the call so clear() in finally always uses the right Level
        Level level = SequencedAssemblyLevelCapture.current();

        ItemStack result;
        try {
            result = original.call(self);
        } finally {
            // Always clear regardless of rollResult() throwing — prevents stale Level
            // on the next SA completion on this thread.
            SequencedAssemblyLevelCapture.clear();
        }

        if (level != null && !level.isClientSide()
                && result != null && !result.isEmpty()) {

            RecipeFinishedContext ctx = RecipeFinishedContext.of(RecipeSource.SEQUENCED_ASSEMBLY, level)
                .recipeId(self.getId())
                .recipe(self)
                .itemOutputs(List.of(result.copy()))
                .itemInputs(List.of(input.copy()))
                .build();

            RecipeEventDispatcher.dispatch(ctx);
        }

        return result;
    }
}
