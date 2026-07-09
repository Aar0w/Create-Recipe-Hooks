package dev.createrecipehooks.mixin.cei;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.createrecipehooks.api.ICrhOwnable;
import dev.createrecipehooks.api.RecipeFinishedContext;
import dev.createrecipehooks.api.RecipeSource;
import dev.createrecipehooks.core.RecipeEventDispatcher;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import plus.dragons.createenchantmentindustry.content.contraptions.enchanting.printer.PrintEntry;

import java.util.List;
import java.util.UUID;

// Fires the CEI_PRINTER event when the Create Enchantment Industry Printer commits a copy operation, and tracks the Printer's owner.
// Skipped silently when CEI is not installed.
@Pseudo
@Mixin(
    targets = "plus.dragons.createenchantmentindustry.content.contraptions.enchanting.printer.PrinterBlockEntity",
    remap = false
)
public abstract class MixinCeiPrinterBlockEntity implements ICrhOwnable {

    @Unique private @Nullable UUID crh$ownerUUID = null;

    @Override public @Nullable UUID crh$getOwnerUUID() { return crh$ownerUUID; }
    @Override public void crh$setOwnerUUID(@Nullable UUID uuid) { this.crh$ownerUUID = uuid; }

    @Inject(method = "write(Lnet/minecraft/nbt/CompoundTag;Z)V", at = @At("HEAD"))
    private void crh$saveOwner(CompoundTag tag, boolean clientPacket, CallbackInfo ci) {
        if (!clientPacket && crh$ownerUUID != null)
            tag.putUUID("crh:owner", crh$ownerUUID);
    }

    @Inject(method = "read(Lnet/minecraft/nbt/CompoundTag;Z)V", at = @At("HEAD"))
    private void crh$loadOwner(CompoundTag tag, boolean clientPacket, CallbackInfo ci) {
        if (!clientPacket)
            crh$ownerUUID = tag.hasUUID("crh:owner") ? tag.getUUID("crh:owner") : null;
    }

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
                RecipeFinishedContext.Builder builder = RecipeFinishedContext
                        .of(RecipeSource.CEI_PRINTER, level)
                        .blockPos(be.getBlockPos())
                        .itemOutputs(List.of(result.copy()))
                        .itemInputs(List.of(inputStack.copy()));

                if (crh$ownerUUID != null) {
                    builder.meta("createrecipehooks:owner_uuid", crh$ownerUUID.toString());
                }

                RecipeEventDispatcher.dispatch(builder.build());
            }
        }

        return result;
    }
}
