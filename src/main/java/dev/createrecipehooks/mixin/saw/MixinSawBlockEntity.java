package dev.createrecipehooks.mixin.saw;

import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.kinetics.saw.SawBlockEntity;
import dev.createrecipehooks.api.RecipeFinishedContext;
import dev.createrecipehooks.api.RecipeSource;
import dev.createrecipehooks.core.RecipeEventDispatcher;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Hook #6 — MechanicalSaw.
 * Risk: MODERATE
 *
 * <h3>1.20.1 Migration</h3>
 * In Create 6.0.8 / NeoForge 1.20.1, {@code SawBlockEntity.getRecipes()} returns
 * {@code List<? extends Recipe<?>>} (no RecipeHolder wrapper).
 * Recipe id accessed via {@code recipe.getId()} ✅
 */
@Mixin(value = SawBlockEntity.class, remap = false)
public abstract class MixinSawBlockEntity {

    @Shadow
    private int recipeIndex;

    @Shadow
    public com.simibubi.create.content.processing.recipe.ProcessingInventory inventory;

    @Inject(
        method = "applyRecipe()V",
        at = @At(value = "RETURN", ordinal = 2)
    )
    private void crh$onSawApplied(
            CallbackInfo ci,
            @Local(ordinal = 1) List<? extends Recipe<?>> recipes
    ) {
        SawBlockEntity self = (SawBlockEntity)(Object)this;
        Level level = self.getLevel();
        if (level == null || level.isClientSide()) return;

        if (recipes == null || recipes.isEmpty()) return;

        int idx = recipeIndex;
        if (idx < 0 || idx >= recipes.size()) return;

        Recipe<?> recipe = recipes.get(idx);

        List<ItemStack> outputs = new java.util.ArrayList<>();
        for (int s = 1; s < inventory.getSlots(); s++) {
            ItemStack out = inventory.getStackInSlot(s);
            if (!out.isEmpty()) outputs.add(out.copy());
        }

        RecipeFinishedContext ctx = RecipeFinishedContext.of(RecipeSource.MECHANICAL_SAW, level)
            .blockPos(self.getBlockPos())
            .recipe(recipe)
            .recipeId(recipe.getId())
            .itemOutputs(outputs)
            .build();

        RecipeEventDispatcher.dispatch(ctx);
    }
}
