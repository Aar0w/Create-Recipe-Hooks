package dev.createrecipehooks.integration.kubejs;

import dev.createrecipehooks.api.BlockProcessedContext;

// Script-facing wrapper for CRHEvents.treeCut: one event per tree felled by a Saw, with log and leaf counts. Lone blocks fire blockProcessed instead.
public class TreeCutEventJS extends BlockProcessedEventJS {

    public TreeCutEventJS(BlockProcessedContext ctx) {
        super(ctx);
    }

    // Number of log blocks in the felled tree (column height for bamboo/cactus/sugarcane/kelp/chorus). -1 when the tree was felled through the Dynamic Trees(or other) mod integration and the size is unknown.
    public int getLogCount() {
        return ctx.getLogCount();
    }

    // Number of leaf blocks in the felled tree. Same -1 rule as getLogCount().
    public int getLeafCount() {
        return ctx.getLeafCount();
    }
}
