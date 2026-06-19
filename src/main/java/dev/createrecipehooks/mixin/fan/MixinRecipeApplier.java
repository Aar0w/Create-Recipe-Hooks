package dev.createrecipehooks.mixin.fan;

import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.foundation.recipe.RecipeApplier;
import dev.createrecipehooks.api.RecipeFinishedContext;
import dev.createrecipehooks.api.RecipeSource;
import dev.createrecipehooks.core.RecipeEventDispatcher;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Hook #2/#3 — RecipeApplier general hook.
 * Risk: STABLE
 *
 * <h3>1.20.1 Migration</h3>
 * <ul>
 *   <li>Removed {@code import com.simibubi.create.AllDataComponents} —
 *       DataComponents API does not exist in MC 1.20.1.</li>
 *   <li>Sequenced Assembly deduplication: In 1.20.1, transitional items are identified
 *       by their NBT tag. Create 0.5.1.f stores the sequenced assembly state as
 *       {@code CompoundTag} under key {@code "SequencedAssembly"} in the ItemStack's tag.
 *       Check: {@code out.hasTag() && out.getOrCreateTag().contains("SequencedAssembly")}.</li>
 * </ul>
 */
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

        // Deduplication: skip Sequenced Assembly transitional items.
        // In NeoForge 1.20.1, SA state is stored as NBT under key "SequencedAssembly".
        for (ItemStack out : outputs) {
            if (!out.isEmpty()
                    && out.hasTag() && out.getOrCreateTag().contains("SequencedAssembly")) {
                return; // intermediate SA step — skip
            }
        }

        RecipeFinishedContext ctx = RecipeFinishedContext.of(resolveSource(recipe), level)
            .recipe(recipe)
            .itemOutputs(outputs)
            .itemInputs(List.of(stackIn.copy()))
            .build();

        RecipeEventDispatcher.dispatch(ctx);
    }

    private static RecipeSource resolveSource(Recipe<?> recipe) {
        RecipeType<?> type = recipe.getType();

        if (type == RecipeType.SMELTING || type == RecipeType.BLASTING)
            return RecipeSource.FAN_BLASTING;
        if (type == RecipeType.SMOKING)
            return RecipeSource.FAN_SMOKING;

        if (type == AllRecipeTypes.HAUNTING.getType())  return RecipeSource.FAN_HAUNTING;
        if (type == AllRecipeTypes.SPLASHING.getType()) return RecipeSource.FAN_SPLASHING;

        // MechanicalPress belt/world (fix #1)
        if (type == AllRecipeTypes.PRESSING.getType())           return RecipeSource.MECHANICAL_PRESS;
        if (type == AllRecipeTypes.SANDPAPER_POLISHING.getType())return RecipeSource.SAND_PAPER;
        if (type == AllRecipeTypes.DEPLOYING.getType()
            || type == AllRecipeTypes.ITEM_APPLICATION.getType()) return RecipeSource.DEPLOYER_BELT;

        return RecipeSource.UNKNOWN;
    }
}
