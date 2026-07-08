package dev.createrecipehooks.mixin.sandpaper;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.equipment.sandPaper.SandPaperItem;
import dev.createrecipehooks.api.RecipeFinishedContext;
import dev.createrecipehooks.api.RecipeSource;
import dev.createrecipehooks.core.RecipeEventDispatcher;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

// Fires the SAND_PAPER event when a player finishes polishing an item by hand.
// This is the only source where getPlayer() is non-null. The SRG method name
// in the target is intentional: production Forge keeps SRG method names at mixin time.
@Mixin(value = SandPaperItem.class, remap = false)
public abstract class MixinSandPaperPolishingRecipe {

    @WrapOperation(
        method = "m_5922_(Lnet/minecraft/world/item/ItemStack;" +
                 "Lnet/minecraft/world/level/Level;" +
                 "Lnet/minecraft/world/entity/LivingEntity;)" +
                 "Lnet/minecraft/world/item/ItemStack;",
        at = @At(
            value = "INVOKE",
            target = "Lcom/simibubi/create/content/equipment/sandPaper/SandPaperPolishingRecipe;" +
                     "applyPolish(Lnet/minecraft/world/level/Level;" +
                     "Lnet/minecraft/world/phys/Vec3;" +
                     "Lnet/minecraft/world/item/ItemStack;" +
                     "Lnet/minecraft/world/item/ItemStack;)" +
                     "Lnet/minecraft/world/item/ItemStack;"
        )
    )
    private ItemStack crh$onSandPaperApply(
            Level level,
            Vec3 pos,
            ItemStack toPolish,
            ItemStack sandPaper,
            Operation<ItemStack> original,
            @Local(argsOnly = true) LivingEntity entityLiving
    ) {
        ItemStack polished = original.call(level, pos, toPolish, sandPaper);

        if (polished != null && !polished.isEmpty() && !level.isClientSide()) {
            ServerPlayer player = entityLiving instanceof ServerPlayer sp ? sp : null;

            RecipeFinishedContext ctx = RecipeFinishedContext.of(RecipeSource.SAND_PAPER, level)
                .player(player)
                .itemOutputs(List.of(polished.copy()))
                .itemInputs(!toPolish.isEmpty() ? List.of(toPolish.copy()) : List.of())
                .build();

            RecipeEventDispatcher.dispatch(ctx);
        }

        return polished;
    }
}
