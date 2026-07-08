package dev.createrecipehooks.api;

// Listener for block-processing events (Drill, Harvester, Saw). The same interface is
// used for both channels: registerBlockProcessed fires once per processed block,
// registerTreeCut once per felled tree. Called synchronously on the server tick thread,
// same rules as IRecipeFinishedListener.
@FunctionalInterface
public interface IBlockProcessedListener {

    // Called on the server tick thread for every processed block or felled tree.
    void onBlockProcessed(BlockProcessedContext ctx);
}
