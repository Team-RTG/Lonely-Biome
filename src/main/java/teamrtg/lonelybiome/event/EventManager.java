package teamrtg.lonelybiome.event;

import net.minecraft.world.gen.layer.GenLayer;
import net.minecraftforge.event.terraingen.WorldTypeEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import teamrtg.lonelybiome.config.lonelybiome.ConfigLB;
import teamrtg.lonelybiome.world.gen.genlayer.GenLayerConstant;

public class EventManager
{

    public EventManager()
    {

    }
    
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onBiomeGenInit(WorldTypeEvent.InitBiomeGens event) {
        if (ConfigLB.singleBiomeId == -1) return;
        GenLayer[] replacement = new GenLayer[2];
        replacement[0] = new GenLayerConstant(ConfigLB.singleBiomeId);
        replacement[1] = replacement[0];
        event.newBiomeGens = replacement;
    }
}