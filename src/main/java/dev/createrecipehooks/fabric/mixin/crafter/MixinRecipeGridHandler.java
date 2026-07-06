package dev.createrecipehooks.fabric.mixin.crafter;

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
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;
import java.util.UUID;

/**
 * Fabric port of the Forge {@code MixinRecipeGridHandler}.
 *
 * <p>Key difference from the Forge version: the vanilla-crafting WrapOperation targets
 * {@code RecipeManager.getRecipeFor} by its <em>Mojmap name with remapping enabled</em> —
 * Loom's refmap converts it to intermediary for runtime. The Forge version had to hardcode
 * the SRG name {@code m_44015_} with {@code remap = false}; no such hack is needed here.
 *
 * <p>Verified against Create Fabric 6.0.8.1 {@code tryToApplyRecipe} (line 142): calls
 * {@code getRecipeFor(RecipeType.CRAFTING, inv, world)} then
 * {@code AllRecipeTypes.MECHANICAL_CRAFTING.find(inv, world)} — same two capture points.
 */
@Mixin(RecipeGridHandler.class)
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

    // ── Vanilla crafting path ────────────────────────────────────────────────

    @WrapOperation(
        method = "tryToApplyRecipe(Lnet/minecraft/world/level/Level;" +
                 "Lcom/simibubi/create/content/kinetics/crafter/RecipeGridHandler$GroupedItems;)" +
                 "Lnet/minecraft/world/item/ItemStack;",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/crafting/RecipeManager;" +
                     "getRecipeFor(Lnet/minecraft/world/item/crafting/RecipeType;" +
                     "Lnet/minecraft/world/Container;" +
                     "Lnet/minecraft/world/level/Level;)Ljava/util/Optional;"
        )
    )
    private static Optional<?> crh$captureVanillaCraftingRecipe(
            RecipeManager manager,
            RecipeType<?> type,
            Container container,
            Level world,
            Operation<Optional<?>> original
    ) {
        Optional<?> result = original.call(manager, type, container, world);
        result.ifPresent(r -> CAPTURED_RECIPE.set((Recipe<?>) r));
        return result;
    }

    // ── MechanicalCrafting path ──────────────────────────────────────────────

    @WrapOperation(
        method = "tryToApplyRecipe(Lnet/minecraft/world/level/Level;" +
                 "Lcom/simibubi/create/content/kinetics/crafter/RecipeGridHandler$GroupedItems;)" +
                 "Lnet/minecraft/world/item/ItemStack;",
        at = @At(
            value = "INVOKE",
            target = "Lcom/simibubi/create/AllRecipeTypes;" +
                     "find(Lnet/minecraft/world/Container;" +
                     "Lnet/minecraft/world/level/Level;)Ljava/util/Optional;"
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

    // ── Dispatch at RETURN ───────────────────────────────────────────────────

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
