package dev.createrecipehooks.mixin.sequenced;

import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
import dev.createrecipehooks.core.SequencedAssemblyLevelCapture;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

// Captures the Level from the SequencedAssemblyRecipe lookups so MixinSequencedAssemblyRecipe can build the event context (advance() has no Level reference).
// The RecipeInput overloads both funnel into getRecipes, so hooking getRecipes and the ItemStack overload covers every path.
@Mixin(value = SequencedAssemblyRecipe.class, remap = false)
public abstract class MixinSequencedAssemblyGetRecipe {

    @Inject(
        method = "getRecipe(Lnet/minecraft/world/level/Level;" +
                 "Lnet/minecraft/world/item/ItemStack;" +
                 "Lnet/minecraft/world/item/crafting/RecipeType;" +
                 "Ljava/lang/Class;)Ljava/util/Optional;",
        at = @At("HEAD")
    )
    private static <R extends ProcessingRecipe<?, ?>> void crh$captureLevel_getRecipe(
            Level level,
            ItemStack item,
            RecipeType<R> type,
            Class<R> clazz,
            CallbackInfoReturnable<Optional<RecipeHolder<R>>> cir
    ) {
        SequencedAssemblyLevelCapture.set(level);
    }

    @Inject(
        method = "getRecipes(Lnet/minecraft/world/level/Level;" +
                 "Lnet/minecraft/world/item/ItemStack;" +
                 "Lnet/minecraft/world/item/crafting/RecipeType;" +
                 "Ljava/lang/Class;" +
                 "Ljava/util/function/Predicate;)Ljava/util/List;",
        at = @At("HEAD")
    )
    private static <R extends ProcessingRecipe<?, ?>> void crh$captureLevel_getRecipes(
            Level level,
            ItemStack item,
            RecipeType<R> type,
            Class<R> clazz,
            Predicate<? super RecipeHolder<R>> filter,
            CallbackInfoReturnable<List<RecipeHolder<R>>> cir
    ) {
        SequencedAssemblyLevelCapture.set(level);
    }
}
