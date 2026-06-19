package dev.createrecipehooks.mixin.crafter;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.kinetics.crafter.RecipeGridHandler;
import dev.createrecipehooks.api.RecipeFinishedContext;
import dev.createrecipehooks.api.RecipeSource;
import dev.createrecipehooks.core.RecipeEventDispatcher;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/**
 * Hook #7 — MechanicalCrafter.
 *
 * <h3>1.20.1 Migration notes</h3>
 * <ul>
 *   <li>{@code CraftingInput} → does not exist; erased generic type is {@code Container}</li>
 *   <li>{@code RecipeHolder} → does not exist; results are raw {@code Recipe<?>}</li>
 *   <li>{@code AllRecipeTypes.find(Container, Level)} → {@code Optional<T extends Recipe<C>>}</li>
 *   <li>{@code RecipeManager.getRecipeFor(RecipeType, Container, Level)} → {@code Optional<T>}</li>
 *   <li>{@code Recipe.getId()} exists in 1.20.1 ✅</li>
 * </ul>
 *
 * <h3>SRG name verification — {@code m_44015_}</h3>
 * <p>WrapOperation #B targets {@code RecipeManager.getRecipeFor(RecipeType, Container, Level)}
 * with {@code remap = false} using the SRG name {@code m_44015_}.
 * This name has been verified against MCP config {@code 1.20.1-20230612.114412}:
 * <pre>
 * Mojang mappings (client_mappings.txt):
 *   net.minecraft.world.item.crafting.RecipeManager  →  cjd
 *   getRecipeFor(RecipeType, Container, Level)         →  a  (obfuscated)
 *
 * Forge joined.tsrg:
 *   cjd  a (Lcjf;Lbdq;Lcmm;)Ljava/util/Optional;  →  m_44015_
 *   cjf = RecipeType, bdq = Container, cmm = Level  (verified)
 * </pre>
 * In production Forge 47.x (MC 1.20.1), class names are remapped to Mojang canonical form
 * while method names remain at SRG level at the time Mixin processes the bytecode.
 * Therefore {@code m_44015_} is correct for production; {@code remap = false} prevents
 * the annotation processor from attempting to remap in DEV (where the method is
 * named {@code getRecipeFor}). The WrapOperation silently no-ops in DEV — expected.
 */
@Mixin(value = RecipeGridHandler.class, remap = false)
public abstract class MixinRecipeGridHandler {

    private static final ThreadLocal<Recipe<?>> CAPTURED_RECIPE = new ThreadLocal<>();

    // ── FIX #4: clear at HEAD ───────────────────────────────────────────────

    @Inject(
        method = "tryToApplyRecipe(Lnet/minecraft/world/level/Level;" +
                 "Lcom/simibubi/create/content/kinetics/crafter/RecipeGridHandler$GroupedItems;)" +
                 "Lnet/minecraft/world/item/ItemStack;",
        at = @At("HEAD")
    )
    private static void crh$clearCapture(
            Level world,
            RecipeGridHandler.GroupedItems items,
            CallbackInfoReturnable<ItemStack> cir
    ) {
        CAPTURED_RECIPE.remove();
    }

    // ── WrapOperation #A: MechanicalCrafting path ───────────────────────────
    // AllRecipeTypes.find(C extends Container, Level) → Optional<T extends Recipe<C>>
    // Erased descriptor uses Container as second parameter type.

    @WrapOperation(
        method = "tryToApplyRecipe(Lnet/minecraft/world/level/Level;" +
                 "Lcom/simibubi/create/content/kinetics/crafter/RecipeGridHandler$GroupedItems;)" +
                 "Lnet/minecraft/world/item/ItemStack;",
        at = @At(
            value = "INVOKE",
            target = "Lcom/simibubi/create/AllRecipeTypes;" +
                     "find(Lnet/minecraft/world/Container;" +
                     "Lnet/minecraft/world/level/Level;)Ljava/util/Optional;",
            remap = false
        )
    )
    private static Optional<?> crh$captureMechanicalCraftingRecipe(
            AllRecipeTypes self,
            Container container,
            Level world,
            Operation<Optional<?>> original
    ) {
        Optional<?> result = original.call(self, container, world);
        result.ifPresent(r -> CAPTURED_RECIPE.set((Recipe<?>) r));
        return result;
    }

    // ── WrapOperation #B: Vanilla crafting path ─────────────────────────────
    // RecipeManager.getRecipeFor(RecipeType, C extends Container, Level) → Optional<T>
    // Erased descriptor uses Container as second parameter type.

    @WrapOperation(
        method = "tryToApplyRecipe(Lnet/minecraft/world/level/Level;" +
                 "Lcom/simibubi/create/content/kinetics/crafter/RecipeGridHandler$GroupedItems;)" +
                 "Lnet/minecraft/world/item/ItemStack;",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/crafting/RecipeManager;" +
                     "m_44015_(Lnet/minecraft/world/item/crafting/RecipeType;" +
                     "Lnet/minecraft/world/Container;" +
                     "Lnet/minecraft/world/level/Level;)Ljava/util/Optional;",
            remap = false
        )
    )
    private static Optional<?> crh$captureVanillaCraftingRecipe(
            net.minecraft.world.item.crafting.RecipeManager manager,
            RecipeType<?> type,
            Container container,
            Level world,
            Operation<Optional<?>> original
    ) {
        Optional<?> result = original.call(manager, type, container, world);
        result.ifPresent(r -> CAPTURED_RECIPE.set((Recipe<?>) r));
        return result;
    }

    // ── Inject RETURN ────────────────────────────────────────────────────────

    @Inject(
        method = "tryToApplyRecipe(Lnet/minecraft/world/level/Level;" +
                 "Lcom/simibubi/create/content/kinetics/crafter/RecipeGridHandler$GroupedItems;)" +
                 "Lnet/minecraft/world/item/ItemStack;",
        at = @At("RETURN")
    )
    private static void crh$onCrafterResult(
            Level world,
            RecipeGridHandler.GroupedItems items,
            CallbackInfoReturnable<ItemStack> cir
    ) {
        try {
            ItemStack result = cir.getReturnValue();
            if (result == null || result.isEmpty()) return;
            if (world == null || world.isClientSide()) return;

            Recipe<?> recipe = CAPTURED_RECIPE.get();

            RecipeFinishedContext.Builder builder = RecipeFinishedContext.of(RecipeSource.MECHANICAL_CRAFTER, world)
                .itemOutputs(java.util.List.of(result.copy()));
            if (recipe != null) builder.recipe(recipe);
            RecipeFinishedContext ctx = builder.build();

            RecipeEventDispatcher.dispatch(ctx);
        } finally {
            CAPTURED_RECIPE.remove();
        }
    }
}
