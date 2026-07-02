package dev.createrecipehooks.neoforge;

import dev.createrecipehooks.api.FluidAmount;
import dev.createrecipehooks.api.RecipeFinishedContext;
import dev.createrecipehooks.api.RecipeSource;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import net.minecraftforge.eventbus.api.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Forge event fired whenever a Create (or addon) recipe completes on the server.
 *
 * <h3>Which event bus</h3>
 * <p>This event is posted on <strong>{@code MinecraftForge.EVENT_BUS}</strong> (the global
 * game event bus), <em>not</em> on the mod-specific event bus.
 * Do NOT subscribe via {@code FMLJavaModLoadingContext.get().getModEventBus()}.
 *
 * <h3>Subscription patterns</h3>
 * <pre>{@code
 * // Option A — static method + @Mod.EventBusSubscriber:
 * @Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
 * public class MyEventHandler {
 *     @SubscribeEvent
 *     public static void onRecipe(CreateRecipeFinishedEvent event) {
 *         RecipeFinishedContext ctx = event.getContext();
 *         if (ctx.getSource() == RecipeSource.BASIN) { ... }
 *     }
 * }
 *
 * // Option B — lambda registration (recommended for simple listeners):
 * MinecraftForge.EVENT_BUS.addListener((CreateRecipeFinishedEvent e) -> {
 *     // e.getItemOutputs(), e.getSource(), etc.
 * });
 *
 * // Option C — direct listener (no Forge dependency in your listener class):
 * CreateRecipeHooks.register(ctx -> { ... });
 * }</pre>
 *
 * <h3>Cancellable / HasResult</h3>
 * <p>This event is <strong>not cancellable</strong> and has <strong>no result</strong> in v1.
 * Listeners are observers only — you cannot prevent the recipe output or modify it here.
 * Output modification support may be added in a future major version.
 *
 * <h3>Threading</h3>
 * <p>Always fired on the <strong>server tick thread</strong>. Never fired client-side.
 * Forge event bus dispatch is synchronous — see
 * {@link dev.createrecipehooks.api.IRecipeFinishedListener} for the async hand-off pattern.
 *
 * <h3>Alternative: direct listener API</h3>
 * <p>If your mod does not need Forge's annotation-based subscription system,
 * {@link dev.createrecipehooks.api.CreateRecipeHooks#register} provides a lighter-weight
 * alternative that avoids the Forge event bus entirely.
 */
public final class CreateRecipeFinishedEvent extends Event {

    private final RecipeFinishedContext context;

    CreateRecipeFinishedEvent(RecipeFinishedContext context) {
        this.context = context;
    }

    @NotNull  public RecipeFinishedContext getContext()          { return context; }
    @NotNull  public RecipeSource getSource()                    { return context.getSource(); }
    @NotNull  public Level getLevel()                            { return context.getLevel(); }
              public long getTimestamp()                         { return context.getTimestamp(); }
    @Nullable public BlockPos getBlockPos()                      { return context.getBlockPos(); }
    @Nullable public ResourceLocation getRecipeId()              { return context.getRecipeId(); }
    @Nullable public Recipe<?> getRecipe()                       { return context.getRecipe(); }
    @Nullable public ServerPlayer getPlayer()                    { return context.getPlayer(); }
    @NotNull  public List<ItemStack> getItemOutputs()            { return context.getItemOutputs(); }
    @NotNull  public List<ItemStack> getItemInputs()             { return context.getItemInputs(); }
    @NotNull  public List<FluidAmount> getFluidOutputs()          { return context.getFluidOutputs(); }
    @NotNull  public Map<String, Object> getMetadata()           { return context.getMetadata(); }
}
