package dev.createrecipehooks.mixin.deployer;

import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.function.Supplier;

// Read access to the forced-result marker that Sequenced Assembly sets on its wrapped step recipes. Non-null means the recipe is an assembly step, not a standalone recipe.
@Mixin(value = ProcessingRecipe.class, remap = false)
public interface CrhProcessingRecipeAccessor {

    @Accessor(value = "forcedResult", remap = false)
    Supplier<ItemStack> crh$getForcedResult();
}
