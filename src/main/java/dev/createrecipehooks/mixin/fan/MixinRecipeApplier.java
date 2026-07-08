package dev.createrecipehooks.mixin.fan;

import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.foundation.recipe.RecipeApplier;
import dev.createrecipehooks.api.RecipeFinishedContext;
import dev.createrecipehooks.api.RecipeSource;
import dev.createrecipehooks.core.RecipeEventDispatcher;
import dev.createrecipehooks.internal.CrhOwnerContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.UUID;

// Shared hook on RecipeApplier.applyRecipeOn: fires FAN_* events (all four fan
// processing types), MECHANICAL_PRESS and SAND_PAPER via the belt path, and UNKNOWN for
// unrecognized addon recipes. Deployer types are excluded here and handled by
// dev.createrecipehooks.mixin.deployer.MixinBeltDeployerCallbacks.
@Mixin(value = RecipeApplier.class, remap = false)
public abstract class MixinRecipeApplier {

    @Inject(
        method = "applyRecipeOn(Lnet/minecraft/world/level/Level;" +
                 "Lnet/minecraft/world/item/ItemStack;" +
                 "Lnet/minecraft/world/item/crafting/Recipe;Z)Ljava/util/List;",
        at = @At("RETURN"),
        remap = false
    )
    private static void crh$onRecipeApplied(
            Level level,
            ItemStack stackIn,
            Recipe<?> recipe,
            boolean returnProcessingRemainder,
            CallbackInfoReturnable<List<ItemStack>> cir
    ) {
        List<ItemStack> outputs = cir.getReturnValue();
        if (outputs == null || outputs.isEmpty()) return;
        if (level == null || level.isClientSide()) return;

        // Skip Sequenced Assembly transitional items; the final step fires its own event.
        for (ItemStack out : outputs) {
            if (!out.isEmpty()
                    && out.hasTag() && out.getOrCreateTag().contains("SequencedAssembly")) {
                return;
            }
        }

        RecipeSource source = resolveSource(recipe);
        if (source == null) return;

        RecipeFinishedContext.Builder builder = RecipeFinishedContext.of(source, level)
            .recipe(recipe)
            .itemOutputs(outputs)
            .itemInputs(List.of(stackIn.copy()));

        UUID ownerUUID = CrhOwnerContext.get();
        if (ownerUUID != null) builder.meta("createrecipehooks:owner_uuid", ownerUUID.toString());

        RecipeEventDispatcher.dispatch(builder.build());
    }

    @Nullable
    private static RecipeSource resolveSource(Recipe<?> recipe) {
        RecipeType<?> type = recipe.getType();

        if (type == RecipeType.SMELTING || type == RecipeType.BLASTING)
            return RecipeSource.FAN_BLASTING;
        if (type == RecipeType.SMOKING)
            return RecipeSource.FAN_SMOKING;

        if (type == AllRecipeTypes.HAUNTING.getType())  return RecipeSource.FAN_HAUNTING;
        if (type == AllRecipeTypes.SPLASHING.getType()) return RecipeSource.FAN_SPLASHING;

        if (type == AllRecipeTypes.PRESSING.getType())            return RecipeSource.MECHANICAL_PRESS;
        if (type == AllRecipeTypes.SANDPAPER_POLISHING.getType()) return RecipeSource.SAND_PAPER;

        // Deployer types are dispatched by MixinBeltDeployerCallbacks; null avoids a duplicate.
        if (type == AllRecipeTypes.DEPLOYING.getType()
            || type == AllRecipeTypes.ITEM_APPLICATION.getType()) return null;

        return RecipeSource.UNKNOWN;
    }
}
