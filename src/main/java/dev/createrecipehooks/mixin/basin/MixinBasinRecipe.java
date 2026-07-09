package dev.createrecipehooks.mixin.basin;

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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraftforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;
import java.util.Objects;

// Fires the BASIN event when a basin recipe commits its outputs (Mixer, Compactor, pressing on a Basin, and addons that go through BasinRecipe.apply).
@Mixin(value = BasinRecipe.class, remap = false)
public abstract class MixinBasinRecipe {

    @WrapOperation(
        method = "apply(Lcom/simibubi/create/content/processing/basin/BasinBlockEntity;" +
                 "Lnet/minecraft/world/item/crafting/Recipe;Z)Z",
        at = @At(
            value = "INVOKE",
            target = "Lcom/simibubi/create/content/processing/basin/BasinBlockEntity;" +
                     "acceptOutputs(Ljava/util/List;Ljava/util/List;Z)Z"
        )
    )
    private static boolean crh$onBasinAcceptOutputs(
            BasinBlockEntity basin,
            List<ItemStack> itemOutputs,
            List<FluidStack> fluidOutputs,
            boolean simulate,
            Operation<Boolean> original,
            @Local(argsOnly = true) Recipe<?> recipe,
            @Local(argsOnly = true) boolean test
    ) {
        boolean accepted = original.call(basin, itemOutputs, fluidOutputs, simulate);

        // Fire only on the real application pass (not simulate, not match-check)
        if (accepted && !simulate && !test
                && basin.getLevel() != null
                && !basin.getLevel().isClientSide()) {

            List<FluidAmount> convertedFluids = fluidOutputs.stream()
                .filter(fs -> !fs.isEmpty())
                .map(fs -> {
                    ResourceLocation key = BuiltInRegistries.FLUID.getKey(fs.getFluid());
                    return key != null ? new FluidAmount(key, fs.getAmount()) : null;
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
