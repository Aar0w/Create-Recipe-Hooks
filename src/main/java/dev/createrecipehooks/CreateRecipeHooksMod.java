package dev.createrecipehooks;

import dev.createrecipehooks.api.CreateRecipeHooks;
import dev.createrecipehooks.api.IHookProvider;
import dev.createrecipehooks.internal.debug.CrhDebugLogger;
import dev.createrecipehooks.internal.debug.DebugListener;
import dev.createrecipehooks.neoforge.NeoForgeAdapter;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Forge 47 (MC 1.20.1) mod entry point for Create Recipe Hooks.
 *
 * <p>Note: on Forge 47, {@code @Mod} constructors do NOT receive {@code IEventBus modBus}
 * as a parameter — that parameter style was introduced in later Forge/NeoForge versions.
 * Use {@code FMLJavaModLoadingContext.get().getModEventBus()} for the mod event bus.
 *
 * <h3>Addon integration via IMC</h3>
 * <p>Addon mods that wish to register an {@link IHookProvider} without a hard
 * dependency call order requirement can send an IMC message during
 * {@code FMLInterModComms.sendTo} / {@code InterModEnqueueEvent}:
 * <pre>{@code
 * // In your addon's InterModEnqueueEvent handler:
 * InterModComms.sendTo(
 *     "createrecipehooks",
 *     "register_hook_provider",
 *     MyAddonHookProvider::new   // Supplier<IHookProvider>
 * );
 * }</pre>
 * <p>The library processes these messages in {@code InterModProcessEvent} and registers
 * each provider via {@link CreateRecipeHooks#registerProvider(IHookProvider)}.
 */
@Mod(CreateRecipeHooksMod.MOD_ID)
public class CreateRecipeHooksMod {

    public static final String MOD_ID = "createrecipehooks";
    public static final Logger LOGGER  = LogManager.getLogger("CreateRecipeHooks");

    static final String IMC_REGISTER_PROVIDER = "register_hook_provider";

    private static final boolean DEBUG_LOGGING = Boolean.getBoolean("crh.debug");

    public CreateRecipeHooksMod() {
        // Bridge from our dispatcher → Forge EVENT_BUS
        NeoForgeAdapter.register();

        // Subscribe to IMC so optional addons (CEI, S&R, PowerGrid, etc.) can
        // register IHookProvider instances without requiring a hard call-order dependency.
        FMLJavaModLoadingContext.get().getModEventBus()
                .addListener(this::processIMC);

        if (DEBUG_LOGGING) {
            CrhDebugLogger.init();
            CreateRecipeHooks.register(new DebugListener());
            LOGGER.info("[CreateRecipeHooks] Debug logging enabled (-Dcrh.debug=true)");
        }

        LOGGER.info("[CreateRecipeHooks] v{} loaded — {} listener(s) registered",
            CreateRecipeHooks.getVersion(),
            dev.createrecipehooks.core.RecipeEventDispatcher.listenerCount());
    }

    private void processIMC(InterModProcessEvent event) {
        // InterModProcessEvent carries no data — messages are retrieved via
        // InterModComms.getMessages(modId): Stream<IMCMessage>  [verified: fmlcore-47.x]
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
