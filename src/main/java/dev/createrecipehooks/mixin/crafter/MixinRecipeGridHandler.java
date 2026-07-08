package dev.createrecipehooks.mixin.crafter;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.kinetics.crafter.RecipeGridHandler;
import dev.createrecipehooks.api.RecipeFinishedContext;
import dev.createrecipehooks.api.RecipeSource;
import dev.createrecipehooks.core.RecipeEventDispatcher;
import dev.createrecipehooks.internal.CrhOwnerContext;
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
import java.util.UUID;

// Fires the MECHANICAL_CRAFTER event when a crafter chain produces its result,
// covering both Create's mechanical crafting recipes and vanilla crafting.
// The SRG method name in the vanilla-path target is intentional: production Forge
// keeps SRG method names at mixin time, and the wrap simply no-ops in dev.
@Mixin(value = RecipeGridHandler.class, remap = false)
public abstract class MixinRecipeGridHandler {

    private static final ThreadLocal<Recipe<?>> CAPTURED_RECIPE = new ThreadLocal<>();

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

    // Mechanical crafting recipe path.
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

    // Vanilla crafting recipe path.
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

            UUID ownerUUID = CrhOwnerContext.get();
            if (ownerUUID != null) builder.meta("createrecipehooks:owner_uuid", ownerUUID.toString());

            RecipeEventDispatcher.dispatch(builder.build());
        } finally {
            CAPTURED_RECIPE.remove();
        }
    }
}
