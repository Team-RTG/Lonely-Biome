package teamrtg.lonelybiome;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.init.Biomes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.layer.GenLayer;
import net.minecraft.world.gen.layer.IntCache;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.event.terraingen.WorldTypeEvent;
import net.minecraftforge.fml.client.IModGuiFactory;
import net.minecraftforge.fml.client.config.GuiConfig;
import net.minecraftforge.fml.client.config.IConfigElement;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


@Mod(
    modid        = LonelyBiome.MOD_ID,
    name         = "Lonely Biome",
    version      = "@MOD_VERSION@",
    dependencies = "required-after:forge@[@MCF_MINVER@,);after:biomesoplenty@[@BOP_VERSION@,);",
    guiFactory   = "teamrtg.lonelybiome.LBConfig$LBGuiConfigFactory",
    acceptableRemoteVersions = "*"
)
public final class LonelyBiome
{
    static final String MOD_ID = "lonelybiome";

    @Mod.Instance(MOD_ID) private static LonelyBiome instance;
    @Mod.EventHandler void initPre (FMLPreInitializationEvent  event) { proxy.preInit (event); }
    @Mod.EventHandler void init    (FMLInitializationEvent     event) { proxy.init    (event); }
    @Mod.EventHandler void postInit(FMLPostInitializationEvent event) { proxy.postInit(event); }

    @SidedProxy private static CommonProxy proxy;
    private static abstract class CommonProxy {
        void preInit(FMLPreInitializationEvent event) {
            LBconfig.init(event);
            LOGGER.debug("Registering InitBiomeGens event handler");
            MinecraftForge.TERRAIN_GEN_BUS.register(this);
        }
        void init(FMLInitializationEvent event) { }
        void postInit(FMLPostInitializationEvent event) { LBconfig.sync(); }
        @SubscribeEvent void onInitBiomeGens(WorldTypeEvent.InitBiomeGens event) {
            if (LBconfig.isEnabled()) {
                GenLayer layer = new GenLayerSingle(LBconfig.biome);
                event.setNewBiomeGens(new GenLayer[]{layer,layer});
                LOGGER.info("Setting up new single-biome GenLayer using Biome: {}, with Id: {}", LBconfig.resLoc, LBconfig.biomeId);
            }
        }
    }
    public  static class ClientProxy extends CommonProxy {
        @Override public void preInit (FMLPreInitializationEvent  event) { super.preInit (event); }
        @Override public void init    (FMLInitializationEvent     event) { super.init    (event); }
        @Override public void postInit(FMLPostInitializationEvent event) { super.postInit(event); }
    }
    public  static class ServerProxy extends CommonProxy {
        @Override public void preInit (FMLPreInitializationEvent  event) { super.preInit (event); }
        @Override public void init    (FMLInitializationEvent     event) { super.init    (event); }
        @Override public void postInit(FMLPostInitializationEvent event) { super.postInit(event); }
    }

    private static final class GenLayerSingle extends GenLayer {
        private final int biomeId;
        GenLayerSingle(@Nonnull Biome biome) {
            super(0L);
            this.biomeId = Biome.getIdForBiome(biome);
            LOGGER.debug("Created new GenLayerSingle for Biome: {}, with Id: {}", LBconfig.resLoc, biomeId);
        }
        @Override @Nonnull
        public int[] getInts(int areaX, int areaY, int areaWidth, int areaHeight) {
            int[] ids = IntCache.getIntCache(areaWidth * areaHeight);
            Arrays.fill(ids, biomeId);
            return ids;
        }
    }

    private static final class LBconfig {
        private LBconfig() {}
        private static final Biome      DEFAULT_BIOME = Biomes.PLAINS;
        private static final int        DEFAULT_BIOME_ID = Biome.getIdForBiome(Biomes.PLAINS);
        private static Biome            biome;
        private static int              biomeId;
        private static ResourceLocation resLoc;
        private static File             configFile;
        private static Configuration    config;
        private static Property         configBiome;
        private static Property         enabled;
        private static boolean          isEnabled() { return enabled.getBoolean(); }
        private static void init(FMLPreInitializationEvent event) {
            if (configFile == null) { configFile = event.getSuggestedConfigurationFile(); }
            if (config == null) { config = new Configuration(configFile); }
            configBiome = config.get(LonelyBiome.MOD_ID, "biome", "minecraft:plains", "The biome to generate the world with." + Configuration.NEW_LINE +
                "Enter the Registry name for a biome (eg. minecraft:plains), or a numeric Biome Id." + Configuration.NEW_LINE + "A Registry name is more " +
                "reliable as biome IDs can change with different configurations.").setLanguageKey(LonelyBiome.MOD_ID+".config.biome");
            enabled = config.get(LonelyBiome.MOD_ID, "enabled", true, "Set this to false to disable LonelyBiome.").setLanguageKey(LonelyBiome.MOD_ID + ".config.enabled");
            if (config.hasChanged()) { config.save(); }
        }
        private static void sync() {
            if (configBiome.getString().contains(":")) {
                resLoc = new ResourceLocation(configBiome.getString());
                biome = Biome.REGISTRY.getObject(resLoc);
                if (biome == null) { biome = DEFAULT_BIOME; }
            }
            else {
                biomeId = MathHelper.getInt(configBiome.getString(), DEFAULT_BIOME_ID);
                biome = Biome.getBiome(biomeId, DEFAULT_BIOME);
            }
            biomeId = Biome.getIdForBiome(biome);
            resLoc = biome.getRegistryName();

            LOGGER.info("LonelyBiome configured with Biome: {}, Id: {}", resLoc, biomeId);

            if (config.hasChanged()) { config.save(); }
        }
    }
    @SideOnly(Side.CLIENT)
    public  static final class LBGuiConfig extends GuiConfig {
        LBGuiConfig(GuiScreen parent) { super(parent, getConfigElements(), MOD_ID, false, false, I18n.format(LonelyBiome.MOD_ID+".config.maintitle")); }
        private static List<IConfigElement> getConfigElements() {
            List<IConfigElement> ret = Lists.newArrayList();
            LBconfig.config.getCategory(MOD_ID).values().forEach(e -> {
                e.setComment(I18n.format(e.getLanguageKey()+".comment"));
                ret.add(new ConfigElement(e));
            });
            return ret;
        }
        @Override public void onGuiClosed() {
            super.onGuiClosed();
            if (LBconfig.config.hasChanged()) { LBconfig.config.save(); }
            LBconfig.sync();
        }
    }
    @SideOnly(Side.CLIENT)
    public  static final class LBGuiConfigFactory implements IModGuiFactory {
        @Override public void initialize(Minecraft mc) {}
        @Override public boolean hasConfigGui() { return true; }
        @Override public GuiScreen createConfigGui(GuiScreen parentScreen) { return new LBGuiConfig(parentScreen); }
        @Override public Set<RuntimeOptionCategoryElement> runtimeGuiCategories() { return null; }
    }
}
