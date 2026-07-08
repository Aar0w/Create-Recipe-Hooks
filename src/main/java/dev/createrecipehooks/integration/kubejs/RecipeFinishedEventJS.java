package dev.createrecipehooks.integration.kubejs;

import dev.createrecipehooks.api.FluidAmount;
import dev.createrecipehooks.api.RecipeFinishedContext;
import dev.latvian.mods.kubejs.event.EventJS;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

// Script-facing wrapper for CRHEvents.recipeFinished. Not cancellable, the recipe has
// already been applied when this fires.
public class RecipeFinishedEventJS extends EventJS {

    public static final String OWNER_UUID_KEY = "createrecipehooks:owner_uuid";

    private final RecipeFinishedContext ctx;

    public RecipeFinishedEventJS(RecipeFinishedContext ctx) {
        this.ctx = ctx;
    }

    // Source machine name, e.g. "MILLSTONE", "FAN_BLASTING". Never null.
    public String getSource() {
        return ctx.getSource().name();
    }

    // Recipe id as string, e.g. "create:milling/wheat". Null for capability fills/empties.
    @Nullable
    public String getRecipeId() {
        return ctx.getRecipeId() != null ? ctx.getRecipeId().toString() : null;
    }

    // Resolves the attributed player: the direct player for SAND_PAPER, otherwise the
    // machine owner / item thrower from the createrecipehooks:owner_uuid metadata.
    // Null when there is no attribution or the player is offline.
    @Nullable
    public ServerPlayer getOwner() {
        if (ctx.getPlayer() != null)
            return ctx.getPlayer();

        Object uuidStr = ctx.getMetadata().get(OWNER_UUID_KEY);
        if (uuidStr == null)
            return null;

        MinecraftServer server = ctx.getLevel().getServer();
        if (server == null)
            return null;

        try {
            return server.getPlayerList().getPlayer(UUID.fromString(uuidStr.toString()));
        } catch (IllegalArgumentException malformedUuid) {
            return null;
        }
    }

    // Raw owner UUID string from metadata, or null. Present even when the player is offline.
    @Nullable
    public String getOwnerUuid() {
        Object uuidStr = ctx.getMetadata().get(OWNER_UUID_KEY);
        return uuidStr != null ? uuidStr.toString() : null;
    }

    public List<ItemStack> getItemOutputs() {
        return ctx.getItemOutputs();
    }

    public List<ItemStack> getItemInputs() {
        return ctx.getItemInputs();
    }

    public List<FluidAmount> getFluidOutputs() {
        return ctx.getFluidOutputs();
    }

    @Nullable
    public BlockPos getBlockPos() {
        return ctx.getBlockPos();
    }

    public Level getLevel() {
        return ctx.getLevel();
    }

    public Map<String, Object> getMetadata() {
        return ctx.getMetadata();
    }

    public long getTimestamp() {
        return ctx.getTimestamp();
    }

    // Escape hatch: the full underlying context for anything not wrapped above.
    public RecipeFinishedContext getContext() {
        return ctx;
    }
}
