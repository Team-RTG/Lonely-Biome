package teamrtg.lonelybiome;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.BiomeDictionary.Type;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.fml.client.IModGuiFactory;
import net.minecraftforge.fml.client.config.GuiConfig;
import net.minecraftforge.fml.client.config.GuiConfigEntries;
import net.minecraftforge.fml.client.config.GuiConfigEntries.SelectValueEntry;
import net.minecraftforge.fml.client.config.IConfigElement;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistryEntry;


public final class LBConfig
{
    private LBConfig() {}

    private static final Logger LOGGER = LogManager.getLogger(LonelyBiome.MOD_ID+"/Config");

    private static final Predicate<Biome> BIOME_FILTER = checkBiome -> Stream.of(Type.BEACH, Type.RIVER, Type.NETHER, Type.END, Type.VOID)
                                                                             .noneMatch(type -> BiomeDictionary.hasType(checkBiome, type));

    private static File          configFile;
    private static Configuration config;
    private static Property      configBiome;
    private static Property      configEnsureVillages;
    private static Property      configEnsureStrongholds;

    @Nullable
    private static Biome         biome;

    static void init(FMLPreInitializationEvent event)
    {
        if (configFile == null) {
            configFile = event.getSuggestedConfigurationFile();
        }

        if (config == null) {
            config = new Configuration(configFile);

            configBiome = config.get(LonelyBiome.MOD_ID, "biome", "minecraft:plains",
                "The registry name of the biome to generate the world with. [Example: minecraft:plains]" + Configuration.NEW_LINE +
                "If left blank, LonelyBiome will be disabled.")
                .setLanguageKey(LonelyBiome.MOD_ID.concat(".config.biome"))
                .setConfigEntryClass(LBBiomeEntry.class);

            configEnsureStrongholds = config.get(LonelyBiome.MOD_ID, "ensureStrongholds", true,
                "If this is set to true, then stronghold generation will be ensured for the single-biome world.")
                .setLanguageKey(LonelyBiome.MOD_ID.concat(".config.ensureStrongholds"));

            configEnsureVillages = config.get(LonelyBiome.MOD_ID, "ensureVillages", true,
                "If this is set to true, then village generation will be ensured for the single-biome world.")
                .setLanguageKey(LonelyBiome.MOD_ID.concat(".config.ensureVillages"));

            sync();
        }
    }

    private static void sync()
    {
        final String cfgbiome = configBiome.getString();
        if (!cfgbiome.isEmpty()) {
            final ResourceLocation resLoc = new ResourceLocation(cfgbiome);
            biome = ForgeRegistries.BIOMES.getValue(resLoc);
            if (biome == null || BIOME_FILTER.negate().test(biome)) {
                biome = null;
                configBiome.set("");
                final Collection<ResourceLocation> biomes = ForgeRegistries.BIOMES.getValuesCollection().stream().filter(BIOME_FILTER)
                    .map(Biome::getRegistryName).filter(Objects::nonNull).sorted().collect(Collectors.toList());
                LOGGER.error("The config biome ({}) is erroneous; setting to \"\" (disabled).", cfgbiome);
                LOGGER.error("Possible values for biome are: {}", biomes);
            } else {
                LOGGER.debug("Biome set to: {}", biome.getRegistryName());
            }
        } else {
            biome = null;
            LOGGER.warn("LonelyBiome is disabled: Config biome is empty.");
        }

        if (config.hasChanged()) {
            config.save();
        }
    }

    @Nullable
    static Biome getBiome()
    {
        return biome;
    }

    static boolean getEnsureVillages()
    {
        return configEnsureVillages.getBoolean();
    }

    static boolean getEnsureStrongholds()
    {
        return configEnsureStrongholds.getBoolean();
    }

    public static final class LBGuiConfig extends GuiConfig
    {
        LBGuiConfig(GuiScreen parent)
        {
            super(parent, getElements(), LonelyBiome.MOD_ID, false, false, I18n.format(LonelyBiome.MOD_ID + ".config.maintitle"));
        }

        private static List<IConfigElement> getElements()
        {
            return config.getCategory(LonelyBiome.MOD_ID).values().stream().map(ConfigElement::new).collect(Collectors.toList());
        }

        @Override
        public void onGuiClosed()
        {
            if (config.hasChanged()) { sync(); }
        }
    }

    @SuppressWarnings("unused")
    public static final class LBGuiConfigFactory implements IModGuiFactory
    {
        @Override public void initialize(Minecraft mc) { }
        @Override public boolean hasConfigGui() { return true; }
        @Override public GuiScreen createConfigGui(GuiScreen parentScreen) { return new LBGuiConfig(parentScreen); }
        @Override public Set<RuntimeOptionCategoryElement> runtimeGuiCategories() { return new HashSet<>(); }
    }

    public static class LBBiomeEntry extends SelectValueEntry
    {
        public LBBiomeEntry(final GuiConfig owningScreen, final GuiConfigEntries owningEntryList, final IConfigElement prop)
        {
            super(owningScreen, owningEntryList, prop, getSelectableValues());
        }

        private static Map<Object, String> getSelectableValues()
        {
            Map<Object, String> ret = ForgeRegistries.BIOMES.getValuesCollection().stream()
                .filter(BIOME_FILTER).map(IForgeRegistryEntry.Impl::getRegistryName).filter(Objects::nonNull)
                .collect(Collectors.toMap(ResourceLocation::toString, resloc -> TextFormatting.AQUA + resloc.toString() + TextFormatting.RESET, (a, b) -> b, HashMap::new));
            ret.put("", " " + TextFormatting.RED + "- " + I18n.format(LonelyBiome.MOD_ID + ".config.disable") + " -" + TextFormatting.RESET);
            return ret;
        }
    }
}
