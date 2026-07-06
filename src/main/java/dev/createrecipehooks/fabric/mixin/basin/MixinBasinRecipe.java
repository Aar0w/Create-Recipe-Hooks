package dev.createrecipehooks.fabric.mixin.basin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.basin.BasinRecipe;
import dev.createrecipehooks.api.FluidAmount;
import dev.createrecipehooks.api.ICrhOwnable;
import dev.createrecipehooks.api.RecipeFinishedContext;
import dev.createrecipehooks.api.RecipeSource;
import dev.createrecipehooks.core.RecipeEventDispatcher;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;
import java.util.Objects;

/**
 * Fabric port of the Forge {@code MixinBasinRecipe}.
 *
 * <h3>Differences from Forge (verified against Create Fabric 6.0.8.1 sources)</h3>
 * <ul>
 *   <li>{@code acceptOutputs} takes a Fabric Transfer API {@link TransactionContext}
 *       instead of {@code boolean simulate}, and Porting Lib {@link FluidStack}s.</li>
 *   <li>{@code apply(basin, recipe, test)} calls {@code acceptOutputs} exactly once for
 *       both the match-check ({@code test=true}) and the real pass; the real pass commits
 *       the transaction afterwards. We fire only when {@code accepted && !test}.</li>
 *   <li>Fluid amounts are in droplets (81000 = 1 bucket); converted to millibuckets
 *       for {@link FluidAmount} by dividing by 81.</li>
 * </ul>
 */
@Mixin(BasinRecipe.class)
public abstract class MixinBasinRecipe {

    @WrapOperation(
        method = "apply(Lcom/simibubi/create/content/processing/basin/BasinBlockEntity;" +
                 "Lnet/minecraft/world/item/crafting/Recipe;Z)Z",
        at = @At(
            value = "INVOKE",
            target = "Lcom/simibubi/create/content/processing/basin/BasinBlockEntity;" +
                     "acceptOutputs(Ljava/util/List;Ljava/util/List;" +
                     "Lnet/fabricmc/fabric/api/transfer/v1/transaction/TransactionContext;)Z"
        )
    )
    private static boolean crh$onBasinAcceptOutputs(
            BasinBlockEntity basin,
            List<ItemStack> itemOutputs,
            List<FluidStack> fluidOutputs,
            TransactionContext transaction,
            Operation<Boolean> original,
            @Local(argsOnly = true) Recipe<?> recipe,
            @Local(argsOnly = true) boolean test
    ) {
        boolean accepted = original.call(basin, itemOutputs, fluidOutputs, transaction);

        if (accepted && !test
                && basin.getLevel() != null
                && !basin.getLevel().isClientSide()) {

            List<FluidAmount> convertedFluids = fluidOutputs.stream()
                .filter(fs -> !fs.isEmpty())
                .map(fs -> {
                    ResourceLocation key = BuiltInRegistries.FLUID.getKey(fs.getFluid());
                    // droplets → millibuckets (81 droplets = 1 mB)
                    return key != null ? new FluidAmount(key, (int) (fs.getAmount() / 81)) : null;
                })
                .filter(Objects::nonNull)
                .toList();

            RecipeFinishedContext.Builder builder = RecipeFinishedContext.of(RecipeSource.BASIN, basin.getLevel())
                .blockPos(basin.getBlockPos())
                .recipe(recipe)
                .itemOutputs(itemOutputs)
                .fluidOutputs(convertedFluids);

            if (basin instanceof ICrhOwnable ownable && ownable.crh$getOwnerUUID() != null) {
                builder.meta("createrecipehooks:owner_uuid", ownable.crh$getOwnerUUID().toString());
            }

            RecipeEventDispatcher.dispatch(builder.build());
        }

        return accepted;
    }
}
