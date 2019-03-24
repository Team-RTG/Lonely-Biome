package teamrtg.lonelybiome;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import com.google.common.collect.Sets;
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
import net.minecraftforge.fml.client.config.GuiConfigEntries.ArrayEntry;
import net.minecraftforge.fml.client.config.GuiConfigEntries.SelectValueEntry;
import net.minecraftforge.fml.client.config.IConfigElement;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.registries.IForgeRegistryEntry;


public final class LBConfig
{
    private LBConfig() {}

    private static final Logger LOGGER = LogManager.getLogger(LonelyBiome.MOD_ID+"/Config");

    private static final Collection<Type> DEFAULT_BIOMETYPES  = Collections.unmodifiableCollection(Arrays.asList(Type.BEACH, Type.RIVER, Type.NETHER, Type.END, Type.VOID));
    private static final Collection<Type> BIOMETYPE_BLACKLIST = Sets.newHashSet(DEFAULT_BIOMETYPES);
    private static final Predicate<Biome> BIOME_FILTER        = checkBiome -> BIOMETYPE_BLACKLIST.stream().noneMatch(type -> BiomeDictionary.hasType(checkBiome, type));

    private static File          configFile;
    private static Configuration config;
    private static Property      configBiome;
    private static Property      configEnsureVillages;
    private static Property      configEnsureStrongholds;
    private static Property      configBiomeTypeBlacklist;

    @Nullable
    private static Biome         biome;

    static void init(FMLPreInitializationEvent event)
    {
        if (configFile == null) {
            configFile = event.getSuggestedConfigurationFile();
        }
    }

    static void postinit()
    {
        if (configFile != null && config == null) {
            config = new Configuration(configFile);

            configBiome = config.get(LonelyBiome.MOD_ID, "biome", "minecraft:plains",
                "The registry name of the biome to generate the world with. [Example: minecraft:plains]" + Configuration.NEW_LINE +
                "If left blank, LonelyBiome will be disabled.")
                .setLanguageKey(LonelyBiome.MOD_ID.concat(".config.biome"));

            configEnsureStrongholds = config.get(LonelyBiome.MOD_ID, "ensureStrongholds", true,
                "If this is set to true, then stronghold generation will be ensured for the single-biome world.")
                .setLanguageKey(LonelyBiome.MOD_ID.concat(".config.ensureStrongholds"));

            configEnsureVillages = config.get(LonelyBiome.MOD_ID, "ensureVillages", true,
                "If this is set to true, then village generation will be ensured for the single-biome world.")
                .setLanguageKey(LonelyBiome.MOD_ID.concat(".config.ensureVillages"));

            configBiomeTypeBlacklist = config.get(LonelyBiome.MOD_ID, "biomeTypeBlacklist", DEFAULT_BIOMETYPES.stream().map(Type::getName).toArray(String[]::new),
                "Biomes of these types will be blacklisted from selection." + Configuration.NEW_LINE +
                "By default, vanilla biomes of the fallowing types are blacklisted as they do not properly generate as" + Configuration.NEW_LINE +
                "normal biomes in the Overworld by themselves, or at all: BEACH, RIVER, NETHER, END, VOID")
                .setLanguageKey(LonelyBiome.MOD_ID.concat(".config.biomeTypeBlacklist"));

            if (FMLCommonHandler.instance().getEffectiveSide() == Side.CLIENT) {
                configBiome.setConfigEntryClass(LBBiomeEntry.class);
                configBiomeTypeBlacklist.setConfigEntryClass(LBBiomeTypeEntry.class);
            }

            sync();
        }
    }

    private static void sync()
    {
        updateBiomeTypeBlacklist(configBiomeTypeBlacklist.getStringList());
        final String cfgbiome = configBiome.getString();
        if (!cfgbiome.isEmpty()) {
            biome = ForgeRegistries.BIOMES.getValue(new ResourceLocation(cfgbiome));
            if (biome == null) {
                invalidateBiome("The config biome ({}) does not exist; setting to \"\" (disabled).", cfgbiome);
            } else if (BIOME_FILTER.negate().test(biome)) {
                invalidateBiome("The config biome ({}) is blacklisted by type; Setting to \"\" (disabled).", cfgbiome);
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

    private static void updateBiomeTypeBlacklist(final Object[] vals)
    {
        BIOMETYPE_BLACKLIST.clear();
        final Collection<String> cfgtypes = Arrays.stream(vals).map(type -> type.toString().toUpperCase()).collect(Collectors.toSet());
        Type.getAll().stream().map(type -> type.getName().toUpperCase()).filter(cfgtypes::contains).map(Type::getType).forEach(BIOMETYPE_BLACKLIST::add);
    }

    private static void invalidateBiome(final String msg, final Object... args)
    {
        biome = null;
        configBiome.set("");
        LOGGER.error(msg, args);
        final Collection<ResourceLocation> biomes = ForgeRegistries.BIOMES.getValuesCollection().stream().filter(BIOME_FILTER)
            .map(Biome::getRegistryName).filter(Objects::nonNull).sorted().collect(Collectors.toList());
        LOGGER.error("Possible values for biome are: {}", biomes);
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
        private static LBBiomeEntry instance;

        public LBBiomeEntry(final GuiConfig owningScreen, final GuiConfigEntries owningEntryList, final IConfigElement prop)
        {
            super(owningScreen, owningEntryList, prop, getSelectableValues());
            instance = this;
        }

        private static Map<Object, String> getSelectableValues()
        {
            Map<Object, String> ret = ForgeRegistries.BIOMES.getValuesCollection().stream()
                .filter(BIOME_FILTER)
                .map(IForgeRegistryEntry.Impl::getRegistryName)
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(ResourceLocation::toString, resloc -> TextFormatting.AQUA + resloc.toString() + TextFormatting.RESET, (a, b) -> b, HashMap::new));
            ret.put("", " " + TextFormatting.RED + "- " + I18n.format(LonelyBiome.MOD_ID + ".config.disable") + " -" + TextFormatting.RESET);
            return ret;
        }

        private static void update()
        {
            if (instance != null) {
                instance.selectableValues = getSelectableValues();
                if (instance.selectableValues.get(instance.currentValue) == null) {
                    instance.currentValue = "";
                    instance.updateValueButtonText();
                }
            }
        }
    }

    public static class LBBiomeTypeEntry extends ArrayEntry
    {
        public LBBiomeTypeEntry(final GuiConfig owningScreen, final GuiConfigEntries owningEntryList, final IConfigElement configElement)
        {
            super(owningScreen, owningEntryList, configElement);
        }

        @Override
        public void setToDefault()
        {
            LBConfig.updateBiomeTypeBlacklist(this.configElement.getDefaults());
            LBBiomeEntry.update();
            super.setToDefault();
        }

        @Override
        public void undoChanges()
        {
            LBConfig.updateBiomeTypeBlacklist(this.beforeValues);
            LBBiomeEntry.update();
            super.undoChanges();
        }

        @Override
        public void setListFromChildScreen(final Object[] newList)
        {
            LBConfig.updateBiomeTypeBlacklist(newList);
            LBBiomeEntry.update();
            super.setListFromChildScreen(newList);
        }

        @Override
        public void updateValueButtonText()
        {
            this.btnValue.displayString = Arrays.stream(currentValues)
                .map(o -> ", " + TextFormatting.DARK_RED + o + TextFormatting.RESET)
                .collect(Collectors.joining())
                .replaceFirst(", ", "");
        }
    }
}
