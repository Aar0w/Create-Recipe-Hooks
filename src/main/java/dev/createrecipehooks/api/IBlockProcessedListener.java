package dev.createrecipehooks.api;

/**
 * Listener for block-processing events (Mechanical Drill, Mechanical Harvester,
 * Mechanical Saw tree cutting).
 *
 * <p>The same functional interface is used for both event channels:
 * <ul>
 *   <li>{@link CreateRecipeHooks#registerBlockProcessed}, one call per processed block;</li>
 *   <li>{@link CreateRecipeHooks#registerTreeCut}, one call per felled tree
 *       (context carries {@link BlockProcessedContext#getLogCount()}).</li>
 * </ul>
 *
 * <p>Threading contract is identical to {@link IRecipeFinishedListener}: called
 * synchronously on the server tick thread, must return promptly, exceptions are
 * caught and logged by the dispatcher.
 */
@FunctionalInterface
public interface IBlockProcessedListener {

    /**
     * Called when a Create machine has processed a world block (or felled a tree,
     * depending on which channel this listener was registered to).
     *
     * @param ctx Snapshot of the processed block. Never {@code null}.
     */
    void onBlockProcessed(BlockProcessedContext ctx);
}
