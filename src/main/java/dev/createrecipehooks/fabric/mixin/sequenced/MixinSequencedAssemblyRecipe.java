package dev.createrecipehooks.fabric.mixin.sequenced;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
import dev.createrecipehooks.api.RecipeFinishedContext;
import dev.createrecipehooks.api.RecipeSource;
import dev.createrecipehooks.core.RecipeEventDispatcher;
import dev.createrecipehooks.core.SequencedAssemblyLevelCapture;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

// Fires the SEQUENCED_ASSEMBLY event when the final step of a sequenced assembly produces the finished item. Intermediate steps do not fire events.
@Mixin(SequencedAssemblyRecipe.class)
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
        Level level = SequencedAssemblyLevelCapture.current();

        ItemStack result;
        try {
            result = original.call(self);
        } finally {
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
