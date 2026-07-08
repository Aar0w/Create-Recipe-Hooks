package dev.createrecipehooks.api;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Snapshot of data available at the moment a Create recipe completed. Source, level and
 * timestamp are always present; everything else is optional, check for null or empty
 * list before use. Read live objects (level, player, item stacks) only on the server
 * tick thread and treat the stacks as read-only.
 */
public final class RecipeFinishedContext {

    // ------------------------------------------------------------------ //
    //  Fields                                                              //
    // ------------------------------------------------------------------ //

    private final RecipeSource           source;
    private final Level                  level;
    private final long                   timestamp;

    @Nullable private final BlockPos           blockPos;
    @Nullable private final ResourceLocation   recipeId;
    @Nullable private final Recipe<?>          recipe;
    @Nullable private final ServerPlayer       player;

    private final List<ItemStack>        itemOutputs;
    private final List<ItemStack>        itemInputs;
    private final List<FluidAmount>      fluidOutputs;
    private final Map<String, Object>    metadata;

    // ------------------------------------------------------------------ //
    //  Constructor, private, use Builder                                  //
    // ------------------------------------------------------------------ //

    private RecipeFinishedContext(Builder b) {
        this.source       = b.source;
        this.level        = b.level;
        this.timestamp    = b.timestamp;
        this.blockPos     = b.blockPos;
        this.recipeId     = b.recipeId;
        this.recipe       = b.recipe;
        this.player       = b.player;
        // ArrayList is used instead of List.copyOf() because Rhino (KubeJS JS engine) cannot
        // reflectively access ImmutableCollections$List12 (non-exported java.base internal class).
        // ArrayList is in the exported java.util package and is accessible from script engines.
        this.itemOutputs  = new ArrayList<>(b.itemOutputs);
        this.itemInputs   = new ArrayList<>(b.itemInputs);
        this.fluidOutputs = new ArrayList<>(b.fluidOutputs);
        this.metadata     = Collections.unmodifiableMap(new HashMap<>(b.metadata));
    }

    // ------------------------------------------------------------------ //
    //  Guaranteed fields                                                   //
    // ------------------------------------------------------------------ //

    /**
     * The machine or mechanic that produced this recipe completion.
     * Never null.
     */
    @NotNull
    public RecipeSource getSource() { return source; }

    /**
     * The server-side Level where the recipe completed.
     * Never null. Events are never dispatched on the client.
     */
    @NotNull
    public Level getLevel() { return level; }

    /**
     * System.nanoTime() at the moment of the event. Not a wall-clock time,
     * subtract two timestamps to get nanosecond durations.
     */
    public long getTimestamp() { return timestamp; }

    // ------------------------------------------------------------------ //
    //  Optional fields                                                     //
    // ------------------------------------------------------------------ //

    /**
     * Block position of the machine. Null for Fan processing (item in the world),
     * belt Deployer and Sand Paper.
     */
    @Nullable
    public BlockPos getBlockPos() { return blockPos; }

    /**
     * Registry ID of the completed recipe, e.g. create:mixing/iron_nugget.
     * Null when no recipe object was involved (capability emptying on the Item Drain).
     */
    @Nullable
    public ResourceLocation getRecipeId() { return recipeId; }

    /**
     * The recipe object. May be null even when getRecipeId() is present, for example
     * vanilla crafting in the Mechanical Crafter stores only the id.
     */
    @Nullable
    public Recipe<?> getRecipe() { return recipe; }

    /**
     * The player involved in this completion. Non-null only for SAND_PAPER hand use;
     * for every other source use the owner UUID from the metadata instead.
     */
    @Nullable
    public ServerPlayer getPlayer() { return player; }

    /**
     * Item stacks produced by this recipe. Never null, may be empty.
     * Treat the stacks as read-only, call stack.copy() before storing or changing one.
     */
    @NotNull
    public List<ItemStack> getItemOutputs() { return itemOutputs; }

    /**
     * Item stacks consumed as inputs (available for Fan, Sequenced Assembly, Spout,
     * Item Drain). Never null, may be empty. Treat the stacks as read-only.
     */
    @NotNull
    public List<ItemStack> getItemInputs() { return itemInputs; }

    /**
     * Fluid outputs in milli-buckets (Basin, Item Drain). Never null, empty for
     * most machines.
     */
    @NotNull
    public List<FluidAmount> getFluidOutputs() { return fluidOutputs; }

    /**
     * Unmodifiable map of addon-defined metadata.
     * Keys are namespaced strings (e.g. "create_enchantment_industry:experience_amount").
     * Empty for all built-in sources.
     */
    @NotNull
    public Map<String, Object> getMetadata() { return metadata; }

    // ------------------------------------------------------------------ //
    //  Builder                                                             //
    // ------------------------------------------------------------------ //

    /**
     * Entry point for constructing a RecipeFinishedContext.
     *
     * Only source and level are required; all other fields are optional.
     */
    public static Builder of(@NotNull RecipeSource source, @NotNull Level level) {
        Objects.requireNonNull(source, "source must not be null");
        Objects.requireNonNull(level,  "level must not be null");
        return new Builder(source, level);
    }

    /**
     * Builder for RecipeFinishedContext.
     *
     * Not thread-safe during construction; call build() on the same thread
     * that holds all the data, then pass the immutable context to other threads.
     */
    public static final class Builder {

        // Required
        private final RecipeSource source;
        private final Level        level;
        private final long         timestamp = System.nanoTime();

        // Optional
        @Nullable private BlockPos         blockPos;
        @Nullable private ResourceLocation recipeId;
        @Nullable private Recipe<?>        recipe;
        @Nullable private ServerPlayer     player;
        private List<ItemStack>    itemOutputs  = List.of();
        private List<ItemStack>    itemInputs   = List.of();
        private List<FluidAmount>  fluidOutputs = List.of();
        private Map<String, Object> metadata    = Map.of();

        private Builder(RecipeSource source, Level level) {
            this.source = source;
            this.level  = level;
        }

        /** Sets the block position of the completing machine. */
        public Builder blockPos(@Nullable BlockPos pos) {
            this.blockPos = pos;
            return this;
        }

        /**
         * Sets both recipeId and recipe from a Recipe object.
         * Extracts the id via recipe.getId().
         */
        public Builder recipe(@NotNull Recipe<?> r) {
            this.recipe   = r;
            this.recipeId = r.getId();
            return this;
        }

        /**
         * Sets only the recipe id (for cases where only the id is available,
         * e.g. vanilla crafting in MechanicalCrafter).
         */
        public Builder recipeId(@Nullable ResourceLocation id) {
            this.recipeId = id;
            return this;
        }

        /** Sets the player (real or FakePlayer) involved in this completion. */
        public Builder player(@Nullable ServerPlayer p) {
            this.player = p;
            return this;
        }

        /** Sets item outputs. The list is defensively copied. */
        public Builder itemOutputs(@NotNull List<ItemStack> outputs) {
            this.itemOutputs = outputs;
            return this;
        }

        /** Sets item inputs (pre-consumption snapshot). The list is defensively copied. */
        public Builder itemInputs(@NotNull List<ItemStack> inputs) {
            this.itemInputs = inputs;
            return this;
        }

        /**
         * Sets fluid outputs. Convert platform FluidStack objects to
         * FluidAmount in the mixin/adapter layer before calling this.
         *
         * @param fluids list of fluid amounts; defensively copied
         */
        public Builder fluidOutputs(@NotNull List<FluidAmount> fluids) {
            this.fluidOutputs = List.copyOf(fluids);
            return this;
        }

        /**
         * Adds a single addon-defined metadata entry.
         * Key should be namespaced: "modid:key".
         */
        public Builder meta(@NotNull String key, @NotNull Object value) {
            if (this.metadata.isEmpty()) {
                this.metadata = new HashMap<>();
            }
            ((HashMap<String, Object>) this.metadata).put(key, value);
            return this;
        }

        /** Builds the immutable RecipeFinishedContext. */
        @NotNull
        public RecipeFinishedContext build() {
            return new RecipeFinishedContext(this);
        }
    }
}
