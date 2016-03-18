package teamrtg.lonelybiome.config;

import java.io.File;

import teamrtg.lonelybiome.config.lonelybiome.ConfigLB;

public class ConfigManager
{
    
    public static File lbConfigFile;

    private ConfigLB configLB = new ConfigLB();
    public ConfigLB rtg() {
        return configLB;
    }
    
    public static void init(String configpath)
    {
    
    	lbConfigFile = new File(configpath + "lonelybiome.cfg");
        
        ConfigLB.init(lbConfigFile);
    }
}
