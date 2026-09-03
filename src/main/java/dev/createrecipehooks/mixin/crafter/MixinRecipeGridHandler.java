package dev.createrecipehooks.mixin.crafter;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.kinetics.crafter.RecipeGridHandler;
import dev.createrecipehooks.api.RecipeFinishedContext;
import dev.createrecipehooks.api.RecipeSource;
import dev.createrecipehooks.core.RecipeEventDispatcher;
import dev.createrecipehooks.internal.CrhOwnerContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;
import java.util.UUID;

// Fires the MECHANICAL_CRAFTER event when a crafter chain produces its result, covering both Create's mechanical crafting recipes and vanilla crafting.
@Mixin(value = RecipeGridHandler.class, remap = false)
public abstract class MixinRecipeGridHandler {

    private static final ThreadLocal<RecipeHolder<?>> CAPTURED_RECIPE = new ThreadLocal<>();

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

    // Vanilla crafting recipe path.
    @WrapOperation(
        method = "tryToApplyRecipe(Lnet/minecraft/world/level/Level;" +
                 "Lcom/simibubi/create/content/kinetics/crafter/RecipeGridHandler$GroupedItems;)" +
                 "Lnet/minecraft/world/item/ItemStack;",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/crafting/RecipeManager;" +
                     "getRecipeFor(Lnet/minecraft/world/item/crafting/RecipeType;" +
                     "Lnet/minecraft/world/item/crafting/RecipeInput;" +
                     "Lnet/minecraft/world/level/Level;)Ljava/util/Optional;"
        )
    )
    private static Optional<? extends RecipeHolder<?>> crh$captureVanillaCraftingRecipe(
            RecipeManager manager,
            RecipeType<?> type,
            RecipeInput input,
            Level world,
            Operation<Optional<? extends RecipeHolder<?>>> original
    ) {
        Optional<? extends RecipeHolder<?>> result = original.call(manager, type, input, world);
        result.ifPresent(CAPTURED_RECIPE::set);
        return result;
    }

    // Mechanical crafting recipe path.
    @WrapOperation(
        method = "tryToApplyRecipe(Lnet/minecraft/world/level/Level;" +
                 "Lcom/simibubi/create/content/kinetics/crafter/RecipeGridHandler$GroupedItems;)" +
                 "Lnet/minecraft/world/item/ItemStack;",
        at = @At(
            value = "INVOKE",
            target = "Lcom/simibubi/create/AllRecipeTypes;" +
                     "find(Lnet/minecraft/world/item/crafting/RecipeInput;" +
                     "Lnet/minecraft/world/level/Level;)Ljava/util/Optional;"
        )
    )
    private static Optional<? extends RecipeHolder<?>> crh$captureMechanicalCraftingRecipe(
            AllRecipeTypes self,
            RecipeInput input,
            Level world,
            Operation<Optional<? extends RecipeHolder<?>>> original
    ) {
        Optional<? extends RecipeHolder<?>> result = original.call(self, input, world);
        result.ifPresent(CAPTURED_RECIPE::set);
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

            RecipeHolder<?> recipe = CAPTURED_RECIPE.get();

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
