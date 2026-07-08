package dev.createrecipehooks.mixin.crusher;

import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.kinetics.crusher.CrushingWheelBlockEntity;
import com.simibubi.create.content.kinetics.crusher.CrushingWheelControllerBlockEntity;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import dev.createrecipehooks.api.ICrhOwnable;
import dev.createrecipehooks.api.RecipeFinishedContext;
import dev.createrecipehooks.api.RecipeSource;
import dev.createrecipehooks.core.RecipeEventDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Fires the CRUSHING_WHEEL event when the wheels finish a recipe. Attribution:
 * whoever threw the item in, with a fallback to the owner of an adjacent wheel
 * for belt and hopper fed input.
 */
@Mixin(value = CrushingWheelControllerBlockEntity.class, remap = false)
public abstract class MixinCrushingWheelController {

    @Unique private @Nullable UUID crh$throwerUUID = null;

    @Inject(
        method = "intakeItem(Lnet/minecraft/world/entity/item/ItemEntity;)V",
        at = @At("HEAD")
    )
    private void crh$captureItemThrower(ItemEntity entity, CallbackInfo ci) {
        Entity owner = entity.getOwner();
        crh$throwerUUID = owner != null ? owner.getUUID() : null;
    }

    @Inject(
        method = "applyRecipe()V",
        at = @At("RETURN")
    )
    private void crh$onCrushingApplied(
            CallbackInfo ci,
            @Local(ordinal = 0) Optional<? extends ProcessingRecipe<?>> recipe,
            @Local(ordinal = 0) List<ItemStack> list
    ) {
        if (recipe == null || recipe.isEmpty()) return;

        CrushingWheelControllerBlockEntity self = (CrushingWheelControllerBlockEntity)(Object)this;
        Level level = self.getLevel();
        if (level == null || level.isClientSide()) return;

        ProcessingRecipe<?> r = recipe.get();
        List<ItemStack> outputs = (list != null) ? List.copyOf(list) : List.of();

        RecipeFinishedContext.Builder builder = RecipeFinishedContext.of(RecipeSource.CRUSHING_WHEEL, level)
            .blockPos(self.getBlockPos())
            .recipe(r)
            .recipeId(r.getId())
            .itemOutputs(outputs);

        // Attribution priority: item thrower first, then the owner of an adjacent wheel.
        UUID attributed = crh$throwerUUID != null
                ? crh$throwerUUID
                : crh$findWheelOwner(level, self.getBlockPos());
        if (attributed != null)
            builder.meta("createrecipehooks:owner_uuid", attributed.toString());

        RecipeEventDispatcher.dispatch(builder.build());
        crh$throwerUUID = null;
    }

    @Unique
    private static @Nullable UUID crh$findWheelOwner(Level level, BlockPos controllerPos) {
        for (Direction dir : Direction.values()) {
            BlockEntity be = level.getBlockEntity(controllerPos.relative(dir));
            if (be instanceof CrushingWheelBlockEntity && be instanceof ICrhOwnable ownable) {
                UUID owner = ownable.crh$getOwnerUUID();
                if (owner != null)
                    return owner;
            }
        }
        return null;
    }
}
