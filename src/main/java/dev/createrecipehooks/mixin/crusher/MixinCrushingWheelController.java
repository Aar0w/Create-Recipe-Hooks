package dev.createrecipehooks.mixin.crusher;

import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.kinetics.crusher.CrushingWheelControllerBlockEntity;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import dev.createrecipehooks.api.RecipeFinishedContext;
import dev.createrecipehooks.api.RecipeSource;
import dev.createrecipehooks.core.RecipeEventDispatcher;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Optional;

/**
 * Hook #5 — CrushingWheel.
 * Risk: STABLE
 *
 * <h3>1.20.1 Migration note</h3>
 * {@code RecipeHolder} does NOT exist in MC 1.20.1.
 * {@code CrushingWheelControllerBlockEntity.findRecipe()} returns
 * {@code Optional<ProcessingRecipe<RecipeWrapper>>} directly.
 * Recipe id accessed via {@code recipe.getId()} which exists in 1.20.1 ✅
 */
@Mixin(value = CrushingWheelControllerBlockEntity.class, remap = false)
public abstract class MixinCrushingWheelController {

    @Inject(
        method = "applyRecipe()V",
        at = @At("RETURN")
    )
    private void crh$onCrushingApplied(
            CallbackInfo ci,
            @Local(ordinal = 0) Optional<? extends ProcessingRecipe<?>> recipe,
            @Local(ordinal = 0) List<ItemStack> list
    ) {
        if (recipe == null || recipe.isEmpty()) return;

        CrushingWheelControllerBlockEntity self = (CrushingWheelControllerBlockEntity)(Object)this;
        Level level = self.getLevel();
        if (level == null || level.isClientSide()) return;

        ProcessingRecipe<?> r = recipe.get();
        List<ItemStack> outputs = (list != null) ? List.copyOf(list) : List.of();

        RecipeFinishedContext ctx = RecipeFinishedContext.of(RecipeSource.CRUSHING_WHEEL, level)
            .blockPos(self.getBlockPos())
            .recipe(r)
            .recipeId(r.getId())
            .itemOutputs(outputs)
            .build();

        RecipeEventDispatcher.dispatch(ctx);
    }
}
