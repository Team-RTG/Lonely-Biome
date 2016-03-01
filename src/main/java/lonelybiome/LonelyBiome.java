package lonelybiome;

import java.util.ArrayList;

import lonelybiome.config.ConfigManager;
import lonelybiome.event.EventManager;
import lonelybiome.proxy.CommonProxy;
import lonelybiome.reference.ModInfo;
import net.minecraftforge.common.MinecraftForge;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerAboutToStartEvent;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.event.FMLServerStoppedEvent;
import cpw.mods.fml.common.event.FMLServerStoppingEvent;

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
        MinecraftForge.EVENT_BUS.register(eventMgr);
        
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
