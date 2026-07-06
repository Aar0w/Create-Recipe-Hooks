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

/**
 * Snapshot of data available at the moment a Create machine processed a world block
 * (broke, harvested, or felled it). This is the block-world counterpart of
 * {@link RecipeFinishedContext}: no recipe is involved, so there is no recipe id,
 * no item outputs, and no fluid outputs.
 *
 * <h3>Which events use this context</h3>
 * <ul>
 *   <li><strong>blockProcessed</strong> — one event per processed block.
 *       Sources: {@link RecipeSource#MECHANICAL_DRILL},
 *       {@link RecipeSource#MECHANICAL_HARVESTER},
 *       {@link RecipeSource#MECHANICAL_SAW} (a sawed block that is not part of a tree).</li>
 *   <li><strong>treeCut</strong> — one event per felled tree.
 *       Source: {@link RecipeSource#MECHANICAL_SAW} only.
 *       {@link #getLogCount()} / {@link #getLeafCount()} carry the tree size.</li>
 * </ul>
 *
 * <h3>Field availability</h3>
 * <ul>
 *   <li>{@link #getSource()}, {@link #getLevel()}, {@link #getBlockState()},
 *       {@link #getTimestamp()} — always present, never {@code null}.</li>
 *   <li>{@link #getBlockPos()} — position of the processed block (for treeCut: the block
 *       the saw physically touched). May be {@code null} only if a hook could not capture it.</li>
 *   <li>{@link #getLogCount()} / {@link #getLeafCount()} — {@code >= 0} for treeCut events
 *       from Create's own TreeCutter; {@code -1} for blockProcessed events and for trees
 *       felled through the Dynamic Trees mod integration (size unknown).</li>
 * </ul>
 *
 * <p>Thread-safety follows the same contract as {@link RecipeFinishedContext}:
 * {@code source}, {@code timestamp}, {@code blockPos}, {@code blockId} are safe on any
 * thread; {@code level} and {@code blockState} only on the server tick thread.
 */
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

    /** The machine that processed the block. Never {@code null}. */
    @NotNull
    public RecipeSource getSource() { return source; }

    /** The server-side {@link Level}. Never {@code null}; events never fire client-side. */
    @NotNull
    public Level getLevel() { return level; }

    /**
     * State of the processed block, captured immediately before destruction.
     * For treeCut events this is the starting block (the log the saw touched).
     * Never {@code null}.
     */
    @NotNull
    public BlockState getBlockState() { return blockState; }

    /**
     * Registry id of the processed block, e.g. {@code minecraft:stone}.
     * Derived from {@link #getBlockState()}; never {@code null}.
     */
    @NotNull
    public ResourceLocation getBlockId() {
        return BuiltInRegistries.BLOCK.getKey(blockState.getBlock());
    }

    /**
     * {@code true} when the machine was operating as a contraption actor (moving on a
     * piston/bearing/gantry/train assembly); {@code false} for stationary machines.
     * Always {@code true} for {@link RecipeSource#MECHANICAL_HARVESTER}.
     */
    public boolean isContraption() { return contraption; }

    /**
     * Number of log blocks in the felled tree. Only meaningful for treeCut events:
     * {@code >= 1} for trees found by Create's TreeCutter (includes vertical plants such
     * as bamboo and cactus, where it is the column height), {@code -1} when the tree was
     * felled through the Dynamic Trees integration (size unknown) and {@code -1} for all
     * blockProcessed events.
     */
    public int getLogCount() { return logCount; }

    /** Number of leaf blocks in the felled tree. Same availability rules as {@link #getLogCount()}. */
    public int getLeafCount() { return leafCount; }

    /** Position of the processed block. See class javadoc for availability. */
    @Nullable
    public BlockPos getBlockPos() { return blockPos; }

    /**
     * Unmodifiable metadata map. Uses the same keys as {@link RecipeFinishedContext},
     * notably {@code createrecipehooks:owner_uuid}.
     */
    @NotNull
    public Map<String, Object> getMetadata() { return metadata; }

    /** {@code System.nanoTime()} captured when the builder was created inside the hook. */
    public long getTimestamp() { return timestamp; }

    // ------------------------------------------------------------------ //
    //  Builder                                                             //
    // ------------------------------------------------------------------ //

    /** Entry point. {@code source}, {@code level} and {@code blockState} are required. */
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

        /** Sets the position of the processed block. */
        public Builder blockPos(@Nullable BlockPos pos) {
            this.blockPos = pos;
            return this;
        }

        /** Marks the event as coming from a contraption actor. */
        public Builder contraption(boolean value) {
            this.contraption = value;
            return this;
        }

        /** Sets tree size for treeCut events. Pass {@code -1, -1} when unknown (Dynamic Trees). */
        public Builder treeSize(int logCount, int leafCount) {
            this.logCount  = logCount;
            this.leafCount = leafCount;
            return this;
        }

        /** Adds a metadata entry. Key should be namespaced: {@code "modid:key"}. */
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
