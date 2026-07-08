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
 * Forge event fired whenever a Create (or addon) recipe completes. Posted on the Forge
 * event bus (the game bus, not the mod bus), server side only, not cancellable.
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
