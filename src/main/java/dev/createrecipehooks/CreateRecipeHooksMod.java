package dev.createrecipehooks;

import dev.createrecipehooks.api.CreateRecipeHooks;
import dev.createrecipehooks.api.IHookProvider;
import dev.createrecipehooks.internal.debug.CrhDebugLogger;
import dev.createrecipehooks.internal.debug.DebugListener;
import dev.createrecipehooks.neoforge.NeoForgeAdapter;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkConstants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// Mod entry point (Forge). Wires the dispatcher into the Forge event bus and lets addons
// register IHookProvider instances via IMC message register_hook_provider.
@Mod(CreateRecipeHooksMod.MOD_ID)
public class CreateRecipeHooksMod {

    public static final String MOD_ID = "createrecipehooks";
    public static final Logger LOGGER  = LogManager.getLogger("CreateRecipeHooks");

    static final String IMC_REGISTER_PROVIDER = "register_hook_provider";

    private static final boolean DEBUG_LOGGING = Boolean.getBoolean("crh.debug");

    public CreateRecipeHooksMod() {
        // Server-side only: clients without this mod can join servers that have it.
        ModLoadingContext.get().registerExtensionPoint(
            IExtensionPoint.DisplayTest.class,
            () -> new IExtensionPoint.DisplayTest(
                () -> NetworkConstants.IGNORESERVERONLY,
                (remoteVersion, isFromServer) -> true
            )
        );

        NeoForgeAdapter.register();

        FMLJavaModLoadingContext.get().getModEventBus()
                .addListener(this::processIMC);

        if (DEBUG_LOGGING) {
            CrhDebugLogger.init();
            CreateRecipeHooks.register(new DebugListener());
            LOGGER.info("[CreateRecipeHooks] Debug logging enabled (-Dcrh.debug=true)");
        }

        LOGGER.info("[CreateRecipeHooks] v{} loaded, {} listener(s) registered",
            CreateRecipeHooks.getVersion(),
            dev.createrecipehooks.core.RecipeEventDispatcher.listenerCount());
    }

    private void processIMC(InterModProcessEvent event) {
        InterModComms.getMessages(MOD_ID)
            .filter(msg -> IMC_REGISTER_PROVIDER.equals(msg.method()))
            .forEach(msg -> {
                Object value = msg.messageSupplier().get();
                if (value instanceof IHookProvider provider) {
                    try {
                        CreateRecipeHooks.registerProvider(provider);
                        LOGGER.info("[CreateRecipeHooks] Registered IHookProvider '{}' via IMC from '{}'",
                            provider.getId(), msg.senderModId());
                    } catch (Exception e) {
                        LOGGER.error("[CreateRecipeHooks] Failed to register IHookProvider from '{}': {}",
                            msg.senderModId(), e.getMessage(), e);
                    }
                } else {
                    LOGGER.warn("[CreateRecipeHooks] IMC '{}' from '{}' carried unexpected type: {}",
                        IMC_REGISTER_PROVIDER, msg.senderModId(),
                        value == null ? "null" : value.getClass().getName());
                }
            });
    }
}
