package dev.createrecipehooks.neoforge;

import com.simibubi.create.content.contraptions.actors.harvester.HarvesterBlock;
import com.simibubi.create.content.fluids.drain.ItemDrainBlock;
import com.simibubi.create.content.fluids.spout.SpoutBlock;
import com.simibubi.create.content.kinetics.crafter.MechanicalCrafterBlock;
import com.simibubi.create.content.kinetics.crusher.CrushingWheelBlock;
import com.simibubi.create.content.kinetics.drill.DrillBlock;
import com.simibubi.create.content.kinetics.fan.EncasedFanBlock;
import com.simibubi.create.content.kinetics.millstone.MillstoneBlock;
import com.simibubi.create.content.kinetics.press.MechanicalPressBlock;
import com.simibubi.create.content.kinetics.saw.SawBlock;
import com.simibubi.create.content.processing.basin.BasinBlock;
import dev.createrecipehooks.api.ICrhOwnable;
import dev.createrecipehooks.api.IRecipeFinishedListener;
import dev.createrecipehooks.api.RecipeFinishedContext;
import dev.createrecipehooks.core.RecipeEventDispatcher;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

// Forge adapter: bridges the dispatcher to the Forge event bus and records machine
// owners on block placement.
public final class NeoForgeAdapter implements IRecipeFinishedListener {

    public static final NeoForgeAdapter INSTANCE = new NeoForgeAdapter();

    private NeoForgeAdapter() {}

    @Override
    public void onRecipeFinished(RecipeFinishedContext ctx) {
        MinecraftForge.EVENT_BUS.post(new CreateRecipeFinishedEvent(ctx));
    }

    // Stores the placer's UUID on every tracked machine.
    @SubscribeEvent
    public static void onOwnableBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        Block block = event.getPlacedBlock().getBlock();
        if (!isOwnerTracked(block)) return;

        BlockEntity be = event.getLevel().getBlockEntity(event.getPos());
        if (be instanceof ICrhOwnable ownable) {
            ownable.crh$setOwnerUUID(player.getUUID());
        }
    }

    private static boolean isOwnerTracked(Block block) {
        if (block instanceof BasinBlock)            return true;
        if (block instanceof MillstoneBlock)        return true;
        if (block instanceof SawBlock)              return true;
        if (block instanceof SpoutBlock)            return true;
        if (block instanceof ItemDrainBlock)        return true;
        if (block instanceof MechanicalCrafterBlock) return true;
        if (block instanceof EncasedFanBlock)       return true;
        if (block instanceof MechanicalPressBlock)  return true;
        if (block instanceof CrushingWheelBlock)    return true;
        if (block instanceof DrillBlock)            return true;
        if (block instanceof HarvesterBlock)        return true;
        // Deployer is skipped: Create sets its own owner field on placement.

        // CEI Printer is matched by registry name to avoid a hard CEI dependency.
        ResourceLocation key = ForgeRegistries.BLOCKS.getKey(block);
        return key != null
            && "create_enchantment_industry".equals(key.getNamespace())
            && "printer".equals(key.getPath());
    }

    // Wires this adapter into the dispatch chain. Called once from the mod constructor.
    public static void register() {
        RecipeEventDispatcher.registerListener(INSTANCE);
        RecipeEventDispatcher.registerBlockProcessedListener(
            ctx -> MinecraftForge.EVENT_BUS.post(new CreateBlockProcessedEvent(ctx)));
        RecipeEventDispatcher.registerTreeCutListener(
            ctx -> MinecraftForge.EVENT_BUS.post(new CreateTreeCutEvent(ctx)));
        MinecraftForge.EVENT_BUS.register(NeoForgeAdapter.class); // onOwnableBlockPlaced
    }
}
