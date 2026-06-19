package dev.createrecipehooks.mixin.cei;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.createrecipehooks.api.RecipeFinishedContext;
import dev.createrecipehooks.api.RecipeSource;
import dev.createrecipehooks.core.RecipeEventDispatcher;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import plus.dragons.createenchantmentindustry.content.contraptions.enchanting.printer.PrintEntry;

import java.util.List;

/**
 * Hook for Create Enchantment Industry — CEI Printer.
 *
 * <p>Soft dependency: {@code @Pseudo} tells the Mixin loader to silently skip this
 * mixin when CEI is not installed. No code from this class runs without CEI.
 *
 * <h3>Injection point</h3>
 * <p>Wraps the {@code Printing.print()} static call inside
 * {@code PrinterBlockEntity.whenItemHeld(TransportedItemStack, TransportedItemStackHandlerBehaviour)}.
 * This call site (bytecode offset 351 in CEI 1.3.3) is the single point where the
 * enchantment-copy operation is committed. It is reached only when:
 * <ol>
 *   <li>The Printer's countdown has expired ({@code processingTicks == 10}).</li>
 *   <li>All preconditions pass: valid entry, correct ink, sufficient quantity.</li>
 * </ol>
 * Early-exit PASS/HOLD paths (no match, too expensive, wrong ink, still counting down)
 * do not reach this call site, so no false-positive events are fired.
 *
 * <h3>Verified against</h3>
 * {@code create_enchantment_industry-1.3.3-for-create-6.0.6.jar}
 */
@Pseudo
@Mixin(
    targets = "plus.dragons.createenchantmentindustry.content.contraptions.enchanting.printer.PrinterBlockEntity",
    remap = false
)
public abstract class MixinCeiPrinterBlockEntity {

    @WrapOperation(
        method = "whenItemHeld(" +
                 "Lcom/simibubi/create/content/kinetics/belt/transport/TransportedItemStack;" +
                 "Lcom/simibubi/create/content/kinetics/belt/behaviour/TransportedItemStackHandlerBehaviour;" +
                 ")Lcom/simibubi/create/content/kinetics/belt/behaviour/BeltProcessingBehaviour$ProcessingResult;",
        at = @At(
            value = "INVOKE",
            target = "Lplus/dragons/createenchantmentindustry/content/contraptions/enchanting/printer/Printing;" +
                     "print(" +
                     "Lplus/dragons/createenchantmentindustry/content/contraptions/enchanting/printer/PrintEntry;" +
                     "Lnet/minecraft/world/item/ItemStack;" +
                     "I" +
                     "Lnet/minecraft/world/item/ItemStack;" +
                     "Lnet/minecraftforge/fluids/FluidStack;" +
                     ")Lnet/minecraft/world/item/ItemStack;",
            remap = false
        )
    )
    private ItemStack crh$onPrinterPrint(
            PrintEntry printEntry,
            ItemStack copyTarget,
            int amount,
            ItemStack inputStack,
            FluidStack fluidStack,
            Operation<ItemStack> original
    ) {
        ItemStack result = original.call(printEntry, copyTarget, amount, inputStack, fluidStack);

        if (result != null && !result.isEmpty()) {
            BlockEntity be = (BlockEntity) (Object) this;
            Level level = be.getLevel();
            if (level != null && !level.isClientSide()) {
                RecipeFinishedContext ctx = RecipeFinishedContext
                        .of(RecipeSource.CEI_PRINTER, level)
                        .blockPos(be.getBlockPos())
                        .itemOutputs(List.of(result.copy()))
                        .itemInputs(List.of(inputStack.copy()))
                        .build();
                RecipeEventDispatcher.dispatch(ctx);
            }
        }

        return result;
    }
}
