package dev.createrecipehooks.integration.kubejs;

import dev.createrecipehooks.api.BlockProcessedContext;

/**
 * Script-facing wrapper for tree-felling events (Mechanical Saw). One event per felled
 * tree; the mutually exclusive lone-block case fires {@code blockProcessed} instead.
 *
 * <pre>{@code
 * CRHEvents.treeCut('MECHANICAL_SAW', event => {
 *     const player = event.getOwner();
 *     if (!player) return;
 *     const logs = event.getLogCount(); // -1 for Dynamic Trees mod trees (size unknown)
 *     // sum logs in player NBT for a "cut 500 logs" quest, etc.
 * })
 * }</pre>
 */
public class TreeCutEventJS extends BlockProcessedEventJS {

    public TreeCutEventJS(BlockProcessedContext ctx) {
        super(ctx);
    }

    /**
     * Number of log blocks in the felled tree (column height for bamboo/cactus/sugar
     * cane/kelp/chorus). {@code -1} when the tree was felled through the Dynamic Trees
     * mod integration and the size is unknown.
     */
    public int getLogCount() {
        return ctx.getLogCount();
    }

    /** Number of leaf blocks in the felled tree. Same {@code -1} rule as {@link #getLogCount()}. */
    public int getLeafCount() {
        return ctx.getLeafCount();
    }
}
