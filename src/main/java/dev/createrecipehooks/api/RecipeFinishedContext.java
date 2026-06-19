package dev.createrecipehooks.api;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Snapshot of data available at the moment a Create recipe completed.
 *
 * <h3>Field availability</h3>
 * <ul>
 *   <li>{@link #getSource()}, {@link #getLevel()}, {@link #getTimestamp()} —
 *       <strong>always present</strong>, never {@code null}.</li>
 *   <li>All other fields are optional; check for {@code null} / empty list before use.</li>
 * </ul>
 *
 * <h3>Immutability — structural, not deep</h3>
 * <p>The <em>structure</em> of this object is immutable: field references cannot change
 * after construction, and all collection fields are unmodifiable views.
 * However, some referenced Minecraft objects are <strong>not deeply immutable</strong>:
 * <ul>
 *   <li>{@link #getLevel()} — a live, mutable {@code Level}; see threading note below.</li>
 *   <li>{@link #getPlayer()} — a live, mutable {@code ServerPlayer}.</li>
 *   <li>{@link #getItemOutputs()} / {@link #getItemInputs()} — lists are unmodifiable,
 *       but the {@code ItemStack} elements inside are mutable objects.
 *       <strong>Do not mutate them.</strong> Treat them as read-only snapshots.</li>
 *   <li>{@link #getFluidOutputs()} — fully deep-immutable ({@link FluidAmount} records).</li>
 * </ul>
 *
 * <h3>Thread safety — READ THIS FOR ASYNC USE</h3>
 * <p>Only the following fields are safe to read from a thread other than the server tick thread:
 * <ul>
 *   <li>{@link #getSource()} — enum constant ✅</li>
 *   <li>{@link #getTimestamp()} — primitive long ✅</li>
 *   <li>{@link #getBlockPos()} — immutable value object ✅</li>
 *   <li>{@link #getRecipeId()} — immutable {@code ResourceLocation} ✅</li>
 *   <li>{@link #getRecipe()} — read-only registered singleton ✅</li>
 *   <li>{@link #getFluidOutputs()} — deep-immutable records ✅</li>
 *   <li>{@link #getMetadata()} — safe if addon-supplied values are thread-safe ⚠️</li>
 * </ul>
 * <p><strong>DO NOT access from other threads:</strong>
 * {@link #getLevel()}, {@link #getPlayer()}, or any {@code ItemStack} from
 * {@link #getItemOutputs()} / {@link #getItemInputs()}. These are live Minecraft objects
 * that are not safe for concurrent access.
 *
 * <p>For async hand-off (WebSocket, analytics, databases), copy the primitive/immutable
 * fields you need before dispatching to a worker thread:
 * <pre>{@code
 * CreateRecipeHooks.register(ctx -> {
 *     // Extract safe fields on server tick thread:
 *     RecipeSource src   = ctx.getSource();
 *     ResourceLocation id = ctx.getRecipeId();
 *     List<FluidAmount> fluids = ctx.getFluidOutputs();
 *     // Hand safe copies to worker:
 *     executor.submit(() -> sendToDatabase(src, id, fluids));
 * });
 * }</pre>
 *
 * <h3>No Create or NeoForge imports</h3>
 * This class depends only on Minecraft common API (shared between NeoForge and Fabric).
 *
 * @see RecipeSource
 * @see IRecipeFinishedListener
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
    //  Constructor — private, use Builder                                  //
    // ------------------------------------------------------------------ //

    private RecipeFinishedContext(Builder b) {
        this.source       = b.source;
        this.level        = b.level;
        this.timestamp    = b.timestamp;
        this.blockPos     = b.blockPos;
        this.recipeId     = b.recipeId;
        this.recipe       = b.recipe;
        this.player       = b.player;
        this.itemOutputs  = List.copyOf(b.itemOutputs);
        this.itemInputs   = List.copyOf(b.itemInputs);
        this.fluidOutputs = List.copyOf(b.fluidOutputs);
        this.metadata     = Collections.unmodifiableMap(new HashMap<>(b.metadata));
    }

    // ------------------------------------------------------------------ //
    //  Guaranteed fields                                                   //
    // ------------------------------------------------------------------ //

    /**
     * The machine or mechanic that produced this recipe completion.
     * Never {@code null}.
     */
    @NotNull
    public RecipeSource getSource() { return source; }

    /**
     * The server-side {@link Level} where the recipe completed.
     * Never {@code null}. Events are never dispatched on the client.
     */
    @NotNull
    public Level getLevel() { return level; }

    /**
     * {@code System.nanoTime()} captured at the moment the {@link Builder} was created
     * inside the completing mixin, immediately before
     * {@link dev.createrecipehooks.core.RecipeEventDispatcher#dispatch} is called.
     * Useful for performance profiling or ordering events.
     * Not a wall-clock time — subtract two timestamps to get nanosecond durations.
     */
    public long getTimestamp() { return timestamp; }

    // ------------------------------------------------------------------ //
    //  Optional fields                                                     //
    // ------------------------------------------------------------------ //

    /**
     * Block position of the machine that completed the recipe.
     *
     * <p>{@code null} for:
     * <ul>
     *   <li>Fan processing (ItemEntity in the world, not a stationary block)</li>
     *   <li>Belt Deployer (position comes from the transported item, not captured)</li>
     *   <li>Sand Paper (held item — no block position)</li>
     * </ul>
     */
    @Nullable
    public BlockPos getBlockPos() { return blockPos; }

    /**
     * Registry ID of the completed recipe, e.g. {@code create:mixing/iron_nugget}.
     *
     * <p>{@code null} for:
     * <ul>
     *   <li>Item Drain when emptying via fluid capability with no matching
     *       {@code EmptyingRecipe} — the fluid is extracted directly from the container's
     *       fluid handler, so no recipe object or ID exists.</li>
     * </ul>
     * <p>All other v1 sources have a recipe object and therefore a non-null recipe ID.
     *
     * <p>See also: {@link #getRecipe()} for the one-way relationship between
     * recipe id and recipe object.
     */
    @Nullable
    public ResourceLocation getRecipeId() { return recipeId; }

    /**
     * The recipe object. Usually the same type as the machine processes.
     *
     * <h4>Relationship with {@link #getRecipeId()}</h4>
     * <p>These two fields are <em>not</em> symmetric:
     * <ul>
     *   <li>If {@code getRecipe() != null} then {@code getRecipeId() != null} (always).</li>
     *   <li>If {@code getRecipeId() != null} then {@code getRecipe()} may still be
     *       {@code null} — for example, Mechanical Crafter vanilla crafting stores only
     *       the recipe id (captured from {@code RecipeManager}), not the recipe object.</li>
     * </ul>
     * <p>When both are needed, check {@code getRecipe()} first; fall back to
     * {@code getRecipeId()} for id-only lookup.
     */
    @Nullable
    public Recipe<?> getRecipe() { return recipe; }

    /**
     * The player involved in this recipe completion.
     *
     * <h4>v1 availability</h4>
     * <ul>
     *   <li>{@link RecipeSource#SAND_PAPER} (hand use by player only) — the real
     *       {@code ServerPlayer}. Non-null.</li>
     *   <li>All other sources, including {@link RecipeSource#DEPLOYER_BELT} — {@code null}.
     *       The Deployer's {@code DeployerFakePlayer} is <em>not</em> captured in v1.</li>
     * </ul>
     *
     * <p>Always check for {@code null} before using this field, even for sources that
     * may add player support in future versions.
     */
    @Nullable
    public ServerPlayer getPlayer() { return player; }

    /**
     * Unmodifiable list of item stacks produced by this recipe.
     * Never {@code null}; empty when no item outputs exist or they could not be captured.
     * Fluid-only recipes (e.g. some Basin recipes) may return an empty list here.
     *
     * <p><strong>Do not mutate the {@code ItemStack} objects in this list.</strong>
     * They are shallow snapshots — the list structure is immutable, but the stacks
     * themselves are mutable Minecraft objects. Treat them as read-only.
     * Call {@code stack.copy()} if you need to store or modify a stack.
     */
    @NotNull
    public List<ItemStack> getItemOutputs() { return itemOutputs; }

    /**
     * Unmodifiable list of item stacks consumed as inputs.
     * Never {@code null}; empty when inputs could not be captured before consumption (most machines).
     * Available for: Fan processing (input ItemEntity), Sequenced Assembly (input stack).
     *
     * <p><strong>Do not mutate the {@code ItemStack} objects in this list.</strong>
     * Same shallow-immutability caveat as {@link #getItemOutputs()}.
     */
    @NotNull
    public List<ItemStack> getItemInputs() { return itemInputs; }

    /**
     * Unmodifiable list of fluid outputs produced by this recipe.
     *
     * <p>Empty for most machines. Non-empty for:
     * <ul>
     *   <li>Basin (mixing recipes with fluid outputs)</li>
     *   <li>Item Drain (emptying fluid containers)</li>
     * </ul>
     *
     * <p>Each entry is a {@link FluidAmount} — a platform-independent snapshot
     * of fluid type and quantity in milli-buckets. No dependency on Forge's FluidStack.
     */
    @NotNull
    public List<FluidAmount> getFluidOutputs() { return fluidOutputs; }

    /**
     * Unmodifiable map of addon-defined metadata.
     * Keys are namespaced strings (e.g. {@code "create_enchantment_industry:experience_amount"}).
     * Empty for all built-in sources.
     */
    @NotNull
    public Map<String, Object> getMetadata() { return metadata; }

    // ------------------------------------------------------------------ //
    //  Builder                                                             //
    // ------------------------------------------------------------------ //

    /**
     * Entry point for constructing a {@link RecipeFinishedContext}.
     *
     * <p>Only {@code source} and {@code level} are required; all other fields are optional.
     */
    public static Builder of(@NotNull RecipeSource source, @NotNull Level level) {
        Objects.requireNonNull(source, "source must not be null");
        Objects.requireNonNull(level,  "level must not be null");
        return new Builder(source, level);
    }

    /**
     * Builder for {@link RecipeFinishedContext}.
     *
     * <p>Not thread-safe during construction; call {@link #build()} on the same thread
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
         * Sets both {@code recipeId} and {@code recipe} from a {@link Recipe} object.
         * Extracts the id via {@code recipe.getId()}.
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
         * Sets fluid outputs. Convert platform {@code FluidStack} objects to
         * {@link FluidAmount} in the mixin/adapter layer before calling this.
         *
         * @param fluids list of fluid amounts; defensively copied
         */
        public Builder fluidOutputs(@NotNull List<FluidAmount> fluids) {
            this.fluidOutputs = List.copyOf(fluids);
            return this;
        }

        /**
         * Adds a single addon-defined metadata entry.
         * Key should be namespaced: {@code "modid:key"}.
         */
        public Builder meta(@NotNull String key, @NotNull Object value) {
            if (this.metadata.isEmpty()) {
                this.metadata = new HashMap<>();
            }
            ((HashMap<String, Object>) this.metadata).put(key, value);
            return this;
        }

        /** Builds the immutable {@link RecipeFinishedContext}. */
        @NotNull
        public RecipeFinishedContext build() {
            return new RecipeFinishedContext(this);
        }
    }
}
