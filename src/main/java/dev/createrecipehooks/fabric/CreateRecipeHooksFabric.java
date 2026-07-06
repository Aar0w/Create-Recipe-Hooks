package dev.createrecipehooks.fabric;

import dev.createrecipehooks.core.RecipeEventDispatcher;
import net.fabricmc.api.ModInitializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Fabric entrypoint for Create Recipe Hooks (declared in {@code fabric.mod.json}).
 *
 * <p>Owner tracking (block-place capture) and the Create mixin hooks are added in later
 * phases; this entrypoint only wires the adapter so that the public callback event works.
 */
public class CreateRecipeHooksFabric implements ModInitializer {

    public static final String MOD_ID = "createrecipehooks";
    public static final Logger LOGGER = LogManager.getLogger("CreateRecipeHooks");

    @Override
    public void onInitialize() {
        FabricAdapter.register();

        LOGGER.info("[CreateRecipeHooks] Fabric adapter initialized — {} listener(s) registered",
            RecipeEventDispatcher.listenerCount());
    }
}
