package dev.createrecipehooks.api;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

// Snapshot of data for a Create machine processing a world block: the Drill breaking
// a block, the Harvester cutting a plant, the Saw cutting a lone block (blockProcessed
// events) or felling a whole tree (treeCut events). No recipe is involved, so unlike
// RecipeFinishedContext there is no recipe id and no item outputs.
public final class BlockProcessedContext {

    private final RecipeSource        source;
    private final Level               level;
    private final BlockState          blockState;
    private final long                timestamp;
    private final boolean             contraption;
    private final int                 logCount;
    private final int                 leafCount;

    @Nullable private final BlockPos  blockPos;

    private final Map<String, Object> metadata;

    private BlockProcessedContext(Builder b) {
        this.source      = b.source;
        this.level       = b.level;
        this.blockState  = b.blockState;
        this.timestamp   = b.timestamp;
        this.contraption = b.contraption;
        this.logCount    = b.logCount;
        this.leafCount   = b.leafCount;
        this.blockPos    = b.blockPos;
        this.metadata    = Collections.unmodifiableMap(new HashMap<>(b.metadata));
    }

    // The machine that processed the block. Never null.
    @NotNull
    public RecipeSource getSource() { return source; }

    // The server-side Level. Never null; events never fire client-side.
    @NotNull
    public Level getLevel() { return level; }

    // State of the processed block, captured right before it broke. For treeCut events
    // this is the starting log the saw touched. Never null.
    @NotNull
    public BlockState getBlockState() { return blockState; }

    // Registry id of the processed block, e.g. minecraft:stone. Never null.
    @NotNull
    public ResourceLocation getBlockId() {
        return BuiltInRegistries.BLOCK.getKey(blockState.getBlock());
    }

    // True when the machine was moving as part of a contraption.
    public boolean isContraption() { return contraption; }

    // Logs in the felled tree, counting bamboo-like columns too. -1 when unknown
    // (Dynamic Trees) or not a treeCut event.
    public int getLogCount() { return logCount; }

    // Leaves in the felled tree. Same -1 rule as getLogCount().
    public int getLeafCount() { return leafCount; }

    // Position of the processed block.
    @Nullable
    public BlockPos getBlockPos() { return blockPos; }

    // Metadata map, most notably the createrecipehooks:owner_uuid key.
    @NotNull
    public Map<String, Object> getMetadata() { return metadata; }

    // System.nanoTime() at the moment of the event.
    public long getTimestamp() { return timestamp; }

    // Starts a builder. Source, level and blockState are required.
    public static Builder of(@NotNull RecipeSource source, @NotNull Level level, @NotNull BlockState blockState) {
        Objects.requireNonNull(source,     "source must not be null");
        Objects.requireNonNull(level,      "level must not be null");
        Objects.requireNonNull(blockState, "blockState must not be null");
        return new Builder(source, level, blockState);
    }

    public static final class Builder {

        private final RecipeSource source;
        private final Level        level;
        private final BlockState   blockState;
        private final long         timestamp = System.nanoTime();

        private boolean  contraption = false;
        private int      logCount    = -1;
        private int      leafCount   = -1;

        @Nullable private BlockPos blockPos;
        private Map<String, Object> metadata = Map.of();

        private Builder(RecipeSource source, Level level, BlockState blockState) {
            this.source     = source;
            this.level      = level;
            this.blockState = blockState;
        }

        // Sets the position of the processed block.
        public Builder blockPos(@Nullable BlockPos pos) {
            this.blockPos = pos;
            return this;
        }

        // Marks the event as coming from a contraption actor.
        public Builder contraption(boolean value) {
            this.contraption = value;
            return this;
        }

        // Sets tree size for treeCut events, -1 when unknown.
        public Builder treeSize(int logCount, int leafCount) {
            this.logCount  = logCount;
            this.leafCount = leafCount;
            return this;
        }

        // Adds one metadata entry, key should be namespaced like "modid:key".
        public Builder meta(@NotNull String key, @NotNull Object value) {
            if (this.metadata.isEmpty()) {
                this.metadata = new HashMap<>();
            }
            ((HashMap<String, Object>) this.metadata).put(key, value);
            return this;
        }

        @NotNull
        public BlockProcessedContext build() {
            return new BlockProcessedContext(this);
        }
    }
}
