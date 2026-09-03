package dev.createrecipehooks.mixin.sequenced;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
import dev.createrecipehooks.api.RecipeFinishedContext;
import dev.createrecipehooks.api.RecipeSource;
import dev.createrecipehooks.core.RecipeEventDispatcher;
import dev.createrecipehooks.core.SequencedAssemblyLevelCapture;
import dev.createrecipehooks.internal.CrhOwnerContext;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;
import java.util.UUID;

// Fires the SEQUENCED_ASSEMBLY event when the final step of a sequenced assembly produces the finished item. Intermediate steps do not fire events.
@Mixin(value = SequencedAssemblyRecipe.class, remap = false)
public abstract class MixinSequencedAssemblyRecipe {

    @WrapOperation(
        method = "advance(Lnet/minecraft/resources/ResourceLocation;" +
                 "Lnet/minecraft/world/item/ItemStack;" +
                 "Lnet/minecraft/util/RandomSource;)Lnet/minecraft/world/item/ItemStack;",
        at = @At(
            value = "INVOKE",
            target = "Lcom/simibubi/create/content/processing/sequenced/SequencedAssemblyRecipe;" +
                     "rollResult(Lnet/minecraft/util/RandomSource;)Lnet/minecraft/world/item/ItemStack;"
        )
    )
    private ItemStack crh$onSequencedAssemblyFinished(
            SequencedAssemblyRecipe self,
            RandomSource random,
            Operation<ItemStack> original,
            @Local(argsOnly = true) ResourceLocation id,
            @Local(argsOnly = true) ItemStack input
    ) {
        Level level = SequencedAssemblyLevelCapture.current();

        ItemStack result;
        try {
            result = original.call(self, random);
        } finally {
            // Always clear so a stale Level never leaks into the next completion.
            SequencedAssemblyLevelCapture.clear();
        }

        if (level != null && !level.isClientSide()
                && result != null && !result.isEmpty()) {

            RecipeFinishedContext.Builder builder = RecipeFinishedContext.of(RecipeSource.SEQUENCED_ASSEMBLY, level)
                .recipeId(id)
                .recipe(self)
                .itemOutputs(List.of(result.copy()))
                .itemInputs(List.of(input.copy()));

            // The machine processing the final step (deployer, press, spout, saw) puts
            // its owner into the context before rolling results.
            UUID ownerUUID = CrhOwnerContext.get();
            if (ownerUUID != null) builder.meta("createrecipehooks:owner_uuid", ownerUUID.toString());

            RecipeEventDispatcher.dispatch(builder.build());
        }

        return result;
    }
}
