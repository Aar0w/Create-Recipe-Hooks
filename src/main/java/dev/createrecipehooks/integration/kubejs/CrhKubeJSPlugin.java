package dev.createrecipehooks.integration.kubejs;

import dev.createrecipehooks.api.BlockProcessedContext;
import dev.createrecipehooks.api.CreateRecipeHooks;
import dev.createrecipehooks.api.RecipeFinishedContext;
import dev.latvian.mods.kubejs.KubeJSPlugin;
import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventHandler;
import dev.latvian.mods.kubejs.event.Extra;
import dev.latvian.mods.kubejs.script.ScriptType;

// KubeJS integration, registers the CRHEvents group (recipeFinished, blockProcessed,
// treeCut). Loaded only by KubeJS via kubejs.plugins.txt, so the dependency stays
// optional; never reference this class from anywhere else in the mod.
public class CrhKubeJSPlugin extends KubeJSPlugin {

    public static final EventGroup GROUP = EventGroup.of("CRHEvents");

    // Extra.STRING (not REQUIRES_STRING), the source filter is optional:
    // scripts may subscribe with or without it.
    public static final EventHandler RECIPE_FINISHED = GROUP
        .server("recipeFinished", () -> RecipeFinishedEventJS.class)
        .extra(Extra.STRING);

    // One event per block broken by a Drill, crop cut by a Harvester, lone block cut by a Saw.
    public static final EventHandler BLOCK_PROCESSED = GROUP
        .server("blockProcessed", () -> BlockProcessedEventJS.class)
        .extra(Extra.STRING);

    // One event per tree felled by a Saw. Mutually exclusive with blockProcessed.
    public static final EventHandler TREE_CUT = GROUP
        .server("treeCut", () -> TreeCutEventJS.class)
        .extra(Extra.STRING);

    @Override
    public void registerEvents() {
        GROUP.register();
    }

    @Override
    public void init() {
        // Bridge: CRH dispatcher → KubeJS. Registered only when KubeJS loads this plugin.
        CreateRecipeHooks.register(CrhKubeJSPlugin::postRecipeFinished);
        CreateRecipeHooks.registerBlockProcessed(CrhKubeJSPlugin::postBlockProcessed);
        CreateRecipeHooks.registerTreeCut(CrhKubeJSPlugin::postTreeCut);
    }

    private static void postRecipeFinished(RecipeFinishedContext ctx) {
        // hasListeners() guard: skip wrapper allocation on servers with no CRH scripts.
        if (!RECIPE_FINISHED.hasListeners())
            return;

        RECIPE_FINISHED.post(ScriptType.SERVER, ctx.getSource().name(), new RecipeFinishedEventJS(ctx));
    }

    private static void postBlockProcessed(BlockProcessedContext ctx) {
        if (!BLOCK_PROCESSED.hasListeners())
            return;

        BLOCK_PROCESSED.post(ScriptType.SERVER, ctx.getSource().name(), new BlockProcessedEventJS(ctx));
    }

    private static void postTreeCut(BlockProcessedContext ctx) {
        if (!TREE_CUT.hasListeners())
            return;

        TREE_CUT.post(ScriptType.SERVER, ctx.getSource().name(), new TreeCutEventJS(ctx));
    }
}
