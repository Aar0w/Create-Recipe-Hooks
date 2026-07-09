package dev.createrecipehooks.fabric.mixin.sequenced;

import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
import dev.createrecipehooks.core.SequencedAssemblyLevelCapture;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

// Captures the Level from every SequencedAssemblyRecipe.getRecipe overload so MixinSequencedAssemblyRecipe can build the event context (the recipe class itself has no Level reference at completion time).
@Mixin(SequencedAssemblyRecipe.class)
public abstract class MixinSequencedAssemblyGetRecipe {

    @Inject(
        method = "getRecipe(Lnet/minecraft/world/level/Level;" +
                 "Lnet/minecraft/world/Container;" +
                 "Lnet/minecraft/world/item/crafting/RecipeType;" +
                 "Ljava/lang/Class;)Ljava/util/Optional;",
        at = @At("HEAD")
    )
    private static <R extends com.simibubi.create.content.processing.recipe.ProcessingRecipe<?>>
    void crh$captureLevel_overload1(
            Level world,
            Container wrapper,
            RecipeType<R> type,
            Class<R> clazz,
            CallbackInfoReturnable<Optional<R>> cir
    ) {
        SequencedAssemblyLevelCapture.set(world);
    }

    @Inject(
        method = "getRecipe(Lnet/minecraft/world/level/Level;" +
                 "Lnet/minecraft/world/Container;" +
                 "Lnet/minecraft/world/item/crafting/RecipeType;" +
                 "Ljava/lang/Class;" +
                 "Ljava/util/function/Predicate;)Ljava/util/Optional;",
        at = @At("HEAD")
    )
    private static <R extends com.simibubi.create.content.processing.recipe.ProcessingRecipe<?>>
    void crh$captureLevel_overload2(
            Level world,
            Container wrapper,
            RecipeType<R> type,
            Class<R> clazz,
            Predicate<? super R> filter,
            CallbackInfoReturnable<Optional<R>> cir
    ) {
        SequencedAssemblyLevelCapture.set(world);
    }

    @Inject(
        method = "getRecipe(Lnet/minecraft/world/level/Level;" +
                 "Lnet/minecraft/world/item/ItemStack;" +
                 "Lnet/minecraft/world/item/crafting/RecipeType;" +
                 "Ljava/lang/Class;)Ljava/util/Optional;",
        at = @At("HEAD")
    )
    private static <R extends com.simibubi.create.content.processing.recipe.ProcessingRecipe<?>>
    void crh$captureLevel_overload3(
            Level level,
            ItemStack item,
            RecipeType<R> type,
            Class<R> clazz,
            CallbackInfoReturnable<Optional<R>> cir
    ) {
        SequencedAssemblyLevelCapture.set(level);
    }

    @Inject(
        method = "getRecipes(Lnet/minecraft/world/level/Level;" +
                 "Lnet/minecraft/world/item/ItemStack;" +
                 "Lnet/minecraft/world/item/crafting/RecipeType;" +
                 "Ljava/lang/Class;)Ljava/util/stream/Stream;",
        at = @At("HEAD")
    )
    private static <R extends com.simibubi.create.content.processing.recipe.ProcessingRecipe<?>>
    void crh$captureLevel_getRecipes(
            Level world,
            ItemStack item,
            RecipeType<R> type,
            Class<R> clazz,
            CallbackInfoReturnable<Stream<R>> cir
    ) {
        SequencedAssemblyLevelCapture.set(world);
    }
}
