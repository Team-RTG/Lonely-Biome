package teamrtg.lonelybiome;

import java.util.ArrayList;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerAboutToStartEvent;
import net.minecraftforge.fml.common.event.FMLServerStartedEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppedEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent;
import teamrtg.lonelybiome.config.ConfigManager;
import teamrtg.lonelybiome.event.EventManager;
import teamrtg.lonelybiome.proxy.CommonProxy;
import teamrtg.lonelybiome.reference.ModInfo;

@Mod(modid = ModInfo.MOD_ID, name = ModInfo.MOD_NAME, version = ModInfo.MOD_VERSION, acceptableRemoteVersions = "*")
public class LonelyBiome {
    
    @Instance("lonelybiome")
    public static LonelyBiome instance;
    public static String configPath;
    public static EventManager eventMgr;
    
    @SidedProxy(serverSide = ModInfo.PROXY_COMMON, clientSide = ModInfo.PROXY_CLIENT)
    public static CommonProxy proxy;

    private ConfigManager configManager = new ConfigManager();

    public ConfigManager configManager(int dimension) {
        return configManager;
    }

    @EventHandler
    public void fmlLifeCycleEvent(FMLPreInitializationEvent event) 
    {    
        instance = this;
        
        eventMgr = new EventManager();
        MinecraftForge.TERRAIN_GEN_BUS.register(eventMgr);
        
        configPath = event.getModConfigurationDirectory() + "/";
        ConfigManager.init(configPath);
    }
    
    @EventHandler
    public void fmlLifeCycleEvent(FMLInitializationEvent event) 
    {

    }
    
    @EventHandler
    public void fmlLifeCycle(FMLPostInitializationEvent event)
    {

    }
    
    @EventHandler
    public void fmlLifeCycle(FMLServerAboutToStartEvent event)
    {

    }
    
    @EventHandler
    public void fmlLifeCycle(FMLServerStartingEvent event)
    {

    }
    
    @EventHandler
    public void fmlLifeCycle(FMLServerStartedEvent event)
    {

    }

    @EventHandler
    public void fmlLifeCycle(FMLServerStoppingEvent event)
    {

    }

    public void runOnServerClose(Runnable action) {
        serverCloseActions.add(action);
    }
    
    private ArrayList<Runnable> serverCloseActions = new ArrayList<Runnable>();
    @EventHandler
    public void fmlLifeCycle(FMLServerStoppedEvent event)
    {
        for (Runnable action: serverCloseActions) {
            action.run();
        }

    }
}
