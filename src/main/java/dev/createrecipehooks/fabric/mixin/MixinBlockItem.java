package dev.createrecipehooks.fabric.mixin;

import dev.createrecipehooks.api.ICrhOwnable;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fabric replacement for Forge's {@code BlockEvent.EntityPlaceEvent} owner capture
 * (see the Forge project's {@code NeoForgeAdapter.onOwnableBlockPlaced}).
 *
 * <p>Fabric API provides no generic block-place event, so we inject at the tail of
 * {@link BlockItem#place(BlockPlaceContext)}: if the placement succeeded, the placer is a
 * server player, and the new block entity implements {@link ICrhOwnable} (added by the
 * CRH Create mixins), the player's UUID is recorded.
 *
 * <p>No per-block-type filter is needed: the {@code instanceof ICrhOwnable} check is
 * exactly the set of machines CRH tracks.
 */
@Mixin(BlockItem.class)
public abstract class MixinBlockItem {

    @Inject(method = "place(Lnet/minecraft/world/item/context/BlockPlaceContext;)Lnet/minecraft/world/InteractionResult;",
            at = @At("RETURN"))
    private void crh$captureOwner(BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (!cir.getReturnValue().consumesAction())
            return;
        if (!(context.getPlayer() instanceof ServerPlayer player))
            return;

        Level level = context.getLevel();
        if (level.isClientSide())
            return;

        BlockEntity be = level.getBlockEntity(context.getClickedPos());
        if (be instanceof ICrhOwnable ownable)
            ownable.crh$setOwnerUUID(player.getUUID());
    }
}
