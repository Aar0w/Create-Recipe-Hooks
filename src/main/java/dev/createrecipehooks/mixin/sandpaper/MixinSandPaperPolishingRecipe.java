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

/**
 * Hook #15b — SandPaper direct player use.
 * Risk: MODERATE
 *
 * <h3>Injection strategy</h3>
 * {@code @WrapOperation} on the single {@code SandPaperPolishingRecipe.applyPolish()} call
 * inside {@code SandPaperItem.m_5922_()} (finishUsingItem; SRG name confirmed from production jar).
 *
 * <h3>Why WrapOperation instead of @Inject @RETURN</h3>
 * {@code finishUsingItem()} has three RETURN instructions:
 * <ol>
 *   <li>Instruction 17  — non-Player entity early return: only {@code stack} (ordinal=0) is
 *       an ItemStack in scope; no {@code toPolish} or {@code polished} yet.</li>
 *   <li>Instruction 91  — client-side early return: both locals exist but at shifted ordinals.</li>
 *   <li>Instruction 151 — final return: merged frame (tag-absent ⊕ server path) loses
 *       {@code toPolish}/{@code polished} from the merged frame.</li>
 * </ol>
 * A {@code @Local(ordinal=1) ItemStack} at {@code @RETURN} fails at transformation time on
 * RETURN 17 and 151 (only one ItemStack in frame), causing {@code SandPaperItem} to fail
 * class-loading and Create to enter ERROR state.
 *
 * <h3>WrapOperation approach</h3>
 * {@code applyPolish(Level, Vec3, ItemStack toPolish, ItemStack sandPaper)} is called
 * exactly once (instruction 54) and only when a Player uses the item. The handler receives
 * {@code toPolish} and {@code polished} (the return value) directly — no {@code @Local}
 * for ItemStacks needed. {@code entityLiving} comes from outer-method args via
 * {@code @Local(argsOnly=true)}, which reads the method descriptor and is always safe.
 *
 * <h3>Available data</h3>
 * <ul>
 *   <li>{@code level}    — from applyPolish arg 0</li>
 *   <li>{@code toPolish} — from applyPolish arg 2 (item being polished)</li>
 *   <li>{@code polished} — return value of applyPolish</li>
 *   <li>{@code entityLiving} — outer method arg 2 (always a Player at this call site)</li>
 * </ul>
 */
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
