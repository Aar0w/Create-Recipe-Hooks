package dev.createrecipehooks.integration.kubejs;

import dev.createrecipehooks.api.BlockProcessedContext;
import dev.latvian.mods.kubejs.event.EventJS;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;

/**
 * Script-facing wrapper around {@link BlockProcessedContext} for KubeJS.
 *
 * <pre>{@code
 * CRHEvents.blockProcessed('MECHANICAL_DRILL', event => {
 *     if (event.getBlockId() !== 'minecraft:stone') return;
 *     const player = event.getOwner();          // ServerPlayer or null
 *     if (!player) return;
 *     // count in player NBT, award quest progress, etc.
 * })
 * }</pre>
 *
 * <p>Not cancellable — the block is already gone when this fires.
 */
public class BlockProcessedEventJS extends EventJS {

    public static final String OWNER_UUID_KEY = "createrecipehooks:owner_uuid";

    protected final BlockProcessedContext ctx;

    public BlockProcessedEventJS(BlockProcessedContext ctx) {
        this.ctx = ctx;
    }

    /** Source machine name: {@code "MECHANICAL_DRILL"}, {@code "MECHANICAL_HARVESTER"}, {@code "MECHANICAL_SAW"}. */
    public String getSource() {
        return ctx.getSource().name();
    }

    /** Processed block id as string, e.g. {@code "minecraft:stone"}. Never null. */
    public String getBlockId() {
        return ctx.getBlockId().toString();
    }

    /** Full block state of the processed block (crop age etc.). Never null. */
    public BlockState getBlockState() {
        return ctx.getBlockState();
    }

    /** True when the machine was moving as part of a contraption. */
    public boolean isContraption() {
        return ctx.isContraption();
    }

    /**
     * Resolves the attributed player: whoever placed the machine (read from the machine's
     * NBT, or from the contraption's serialized NBT for contraption actors).
     * Null when there is no attribution or the player is offline.
     */
    @Nullable
    public ServerPlayer getOwner() {
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

    /** Raw owner UUID string from metadata, or null. Present even when the player is offline. */
    @Nullable
    public String getOwnerUuid() {
        Object uuidStr = ctx.getMetadata().get(OWNER_UUID_KEY);
        return uuidStr != null ? uuidStr.toString() : null;
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

    /** Escape hatch: the full underlying context for anything not wrapped above. */
    public BlockProcessedContext getContext() {
        return ctx;
    }
}
