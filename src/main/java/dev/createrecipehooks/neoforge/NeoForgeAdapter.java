package dev.createrecipehooks.neoforge;

import dev.createrecipehooks.api.IRecipeFinishedListener;
import dev.createrecipehooks.api.RecipeFinishedContext;
import dev.createrecipehooks.core.RecipeEventDispatcher;
import net.minecraftforge.common.MinecraftForge;

/**
 * Forge adapter — bridges {@link RecipeEventDispatcher} to the Forge global EVENT_BUS.
 *
 * <p>This is the <strong>only</strong> file that imports {@code net.minecraftforge.*}.
 * All Mixin classes are platform-agnostic.
 *
 * <p>For the 1.20.1 / Forge 47 target, uses {@code MinecraftForge.EVENT_BUS}
 * (not {@code NeoForge.EVENT_BUS} which belongs to NeoForge 20.4+ / 1.21).
 */
public final class NeoForgeAdapter implements IRecipeFinishedListener {

    public static final NeoForgeAdapter INSTANCE = new NeoForgeAdapter();

    private NeoForgeAdapter() {}

    @Override
    public void onRecipeFinished(RecipeFinishedContext ctx) {
        MinecraftForge.EVENT_BUS.post(new CreateRecipeFinishedEvent(ctx));
    }

    /**
     * Wires this adapter into the dispatch chain.
     *
     * <p>Calls {@link RecipeEventDispatcher#registerListener} directly rather than
     * going through {@link dev.createrecipehooks.api.CreateRecipeHooks#register} because
     * this adapter IS part of the library infrastructure, not an addon. Using the public
     * API here would be circular — the adapter and the public API are at the same layer.
     */
    public static void register() {
        RecipeEventDispatcher.registerListener(INSTANCE);
    }
}
