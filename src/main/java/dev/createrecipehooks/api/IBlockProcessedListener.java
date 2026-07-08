package dev.createrecipehooks.api;

/**
 * Listener for block-processing events (Drill, Harvester, Saw). The same interface is
 * used for both channels: registerBlockProcessed fires once per processed block,
 * registerTreeCut once per felled tree. Called synchronously on the server tick thread,
 * same rules as IRecipeFinishedListener.
 */
@FunctionalInterface
public interface IBlockProcessedListener {

    /**
     * Called when a Create machine has processed a world block (or felled a tree,
     * depending on which channel this listener was registered to).
     *
     * @param ctx Snapshot of the processed block. Never null.
     */
    void onBlockProcessed(BlockProcessedContext ctx);
}
