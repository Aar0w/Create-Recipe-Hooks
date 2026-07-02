package dev.createrecipehooks.api;

/**
 * Listener that receives notifications when a Create recipe completes.
 *
 * <p>Register via {@link CreateRecipeHooks#register(IRecipeFinishedListener)}.
 *
 * <p>This is a {@link FunctionalInterface}, so lambdas are the idiomatic usage:
 * <pre>{@code
 * CreateRecipeHooks.register(ctx -> {
 *     if (ctx.getSource() == RecipeSource.BASIN) {
 *         // handle basin recipe completion
 *     }
 * });
 * }</pre>
 *
 * <h3>Threading contract — READ BEFORE IMPLEMENTING</h3>
 * <p>{@link #onRecipeFinished} is called <strong>synchronously on the server tick
 * thread</strong>. The call happens inline inside the Create machine's processing
 * logic. This means:
 * <ul>
 *   <li>The callback <strong>must return promptly</strong>. Do not perform
 *       blocking I/O (network, database, file writes) inside this method.</li>
 *   <li>The callback <strong>must not throw unchecked exceptions</strong> that
 *       affect game state. Exceptions are caught and logged, but the original
 *       machine operation has already completed — you cannot roll it back.</li>
 * </ul>
 *
 * <h3>Async / off-thread pattern</h3>
 * <p>{@link RecipeFinishedContext} is <strong>structurally immutable</strong> but
 * <em>not fully thread-safe</em>: some fields reference live Minecraft objects.
 *
 * <p><strong>Safe to access from any thread:</strong>
 * {@link RecipeFinishedContext#getSource()},
 * {@link RecipeFinishedContext#getTimestamp()},
 * {@link RecipeFinishedContext#getBlockPos()},
 * {@link RecipeFinishedContext#getRecipeId()},
 * {@link RecipeFinishedContext#getRecipe()},
 * {@link RecipeFinishedContext#getFluidOutputs()}.
 *
 * <p><strong>NOT safe to access from other threads:</strong>
 * {@link RecipeFinishedContext#getLevel()},
 * {@link RecipeFinishedContext#getPlayer()},
 * and any {@code ItemStack} objects from
 * {@link RecipeFinishedContext#getItemOutputs()} /
 * {@link RecipeFinishedContext#getItemInputs()}.
 * These are live Minecraft objects — access only from the server tick thread.
 *
 * <p>For async hand-off, extract the primitive/immutable fields you need
 * <em>on the server tick thread</em>, then pass those to a worker:
 * <pre>{@code
 * private final BlockingQueue<RecipeFinishedContext> queue = new LinkedBlockingQueue<>();
 *
 * // Register once — runs on server tick thread:
 * CreateRecipeHooks.register(ctx -> {
 *     // ctx.getLevel() and ctx.getPlayer() must be read HERE, not in the worker.
 *     queue.offer(ctx);  // safe: structural reference, fast, non-blocking
 * });
 *
 * // Worker thread — read only thread-safe fields:
 * executor.submit(() -> {
 *     for (RecipeFinishedContext ctx : queue) {
 *         // Safe in worker:
 *         RecipeSource src = ctx.getSource();
 *         ResourceLocation id = ctx.getRecipeId();
 *         List<FluidAmount> fluids = ctx.getFluidOutputs();
 *         sendToWebSocket(src, id, fluids);
 *         // Unsafe in worker: ctx.getLevel(), ctx.getPlayer(), ctx.getItemOutputs()
 *     }
 * });
 * }</pre>
 *
 * <h3>Error isolation</h3>
 * <p>Exceptions thrown by a listener are caught by the dispatcher and logged.
 * One broken listener will not prevent others from being called.
 */
@FunctionalInterface
public interface IRecipeFinishedListener {

    /**
     * Called when a Create (or addon) recipe has successfully completed.
     *
     * <p>Called on the server tick thread. Must return promptly.
     * See threading contract in the interface javadoc.
     *
     * @param ctx Snapshot of the completed recipe. Never {@code null}.
     *            Safe to retain structurally; only immutable fields are safe cross-thread.
     *            See threading contract in the interface javadoc.
     */
    void onRecipeFinished(RecipeFinishedContext ctx);
}
