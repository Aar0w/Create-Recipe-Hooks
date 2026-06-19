package dev.createrecipehooks.mixin.sequenced;

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

/**
 * Captures {@link Level} from all SequencedAssemblyRecipe.getRecipe/getRecipes overloads.
 *
 * <h3>1.20.1 Create 6.0.8 actual signatures (from javap)</h3>
 * <pre>
 * getRecipe(Level, C extends Container, RecipeType, Class)           → Optional  [overload 1]
 * getRecipe(Level, C extends Container, RecipeType, Class, Predicate) → Optional [overload 2]
 * getRecipe(Level, ItemStack, RecipeType, Class)                      → Optional  [overload 3]
 * getRecipes(Level, ItemStack, RecipeType, Class)                     → Stream    [no Predicate!]
 * </pre>
 * Overloads 1 and 2 use Container (erased from generic C extends Container) in bytecode.
 * getRecipes has no Predicate parameter and returns Stream, not List.
 */
@Mixin(value = SequencedAssemblyRecipe.class, remap = false)
public abstract class MixinSequencedAssemblyGetRecipe {

    // ── Overload #1: getRecipe(Level, Container, RecipeType, Class) ────────

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

    // ── Overload #2: getRecipe(Level, Container, RecipeType, Class, Predicate) ─

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

    // ── Overload #3: getRecipe(Level, ItemStack, RecipeType, Class) ───────────

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

    // ── getRecipes(Level, ItemStack, RecipeType, Class) → Stream ─────────────
    // No Predicate parameter; returns Stream (not List) in Create 6.0.8.

    @Inject(
        method = "getRecipes(Lnet/minecraft/world/level/Level;" +
                 "Lnet/minecraft/world/item/ItemStack;" +
                 "Lnet/minecraft/world/item/crafting/RecipeType;" +
                 "Ljava/lang/Class;)Ljava/util/stream/Stream;",
        at = @At("HEAD")
    )
    private static <R extends com.simibubi.create.content.processing.recipe.ProcessingRecipe<?>>
    void crh$captureLevel_getRecipes(
            Level level,
            ItemStack item,
            RecipeType<R> type,
            Class<R> clazz,
            CallbackInfoReturnable<Stream<R>> cir
    ) {
        SequencedAssemblyLevelCapture.set(level);
    }
}
