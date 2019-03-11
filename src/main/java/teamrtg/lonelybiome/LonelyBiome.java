package teamrtg.lonelybiome;

import java.util.Collections;
import java.util.List;
import com.google.common.collect.Lists;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeProvider;
import net.minecraft.world.biome.BiomeProviderSingle;
import net.minecraft.world.gen.structure.MapGenVillage;
import net.minecraftforge.common.BiomeManager;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;


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

    private static final String BOP_MOD_ID = "biomesoplenty";

    @Mod.EventHandler
    void initPre(final FMLPreInitializationEvent event)
    {
        // early initialisation to create the config.
        LBConfig.init(event);
    }

    @Mod.EventHandler
    void initPost(final FMLPostInitializationEvent event)
    {
        // late initialisation in case mods improperly adds biome types late in the lifecycle.
        LBConfig.postinit();
    }

    @Mod.EventBusSubscriber
    public static final class EventHandler
    {
        private static final Logger          LOGGER         = LogManager.getLogger(MOD_ID + "/EventHandler");
        private static final String          PROVIDER_FIELD = "field_76578_c";
        private static final List<WorldType> BOP_WORLDTYPES = Lists.newArrayList();

        private static boolean resetStronghold = false;
        private static boolean resetVillage    = false;

        private EventHandler() {}

        @SubscribeEvent
        public static void onCreateSpawn(final WorldEvent.CreateSpawnPosition event)
        {
            // Do this on Overworld creation no matter what. If not done during spawn point creation
            // then an erroneous chunk will be created by the world at the spawn location.
            final World world = event.getWorld();
            if (verifyWorld(world)) { handleWorldLoad(world); }
        }

        @SubscribeEvent
        public static void onWorldLoad(final WorldEvent.Load event)
        {
            // Only do this if the world is already created (world.getTotalWorldTime() > 0).
            // For new worlds, this needs to be called from WorldEvent.CreateSpawnPosition
            final World world = event.getWorld();
            if (verifyWorld(world) && world.getTotalWorldTime() > 0) { handleWorldLoad(world); }
        }

        @SubscribeEvent
        public static void onWorldUnload(final WorldEvent.Unload event)
        {
            final World world = event.getWorld();
            if (verifyWorld(world)) {
                LBSaveData data = LBSaveData.getSaveData(world);
                final Biome biome;
                if (data != null && (biome = data.getBiome()) != null) {
                    // If village/stronghold generation is not default for this biome, then reset it.
                    resetBiome(biome);
                }

                // Reset the Biomes O' Plenty decoration blacklist back to default when the world is unloaded.
                if (Loader.isModLoaded(BOP_MOD_ID)) {
                    biomesoplenty.api.biome.BOPBiomes.excludedDecoratedWorldTypes.clear();
                    biomesoplenty.api.biome.BOPBiomes.excludedDecoratedWorldTypes.addAll(BOP_WORLDTYPES);
                    LOGGER.debug("Reset the Biomes O' Plenty world type decoration blacklist.");
                }
            }
        }

        private static void handleWorldLoad(final World world)
        {
            Biome biome = null;
            LBSaveData data = LBSaveData.getSaveData(world);
            if (data != null) {
                LOGGER.debug("Found LonelyBiome save data.");
                biome = data.getBiome();
            } else {
                if (world.getTotalWorldTime() == 0) {
                    LOGGER.debug("LonelyBiome save data not found and world is new. Creating new save data.");
                    biome = LBSaveData.initNewSaveData(world).getBiome();
                } else {
                    LOGGER.warn("LonelyBiome save data not found and world is old. No changes made.");
                }
            }

            if (biome != null) {
                final ResourceLocation resloc = biome.getRegistryName();

                // special case for BoP, so its biomes will decorate, because it is such a unique snowflake.
                if (Loader.isModLoaded(BOP_MOD_ID)) {
                    if (resloc != null && resloc.getNamespace().equals(BOP_MOD_ID)) {
                        final WorldType worldType = world.getWorldType();
                        final List<WorldType> bopWorldTypes = biomesoplenty.api.biome.BOPBiomes.excludedDecoratedWorldTypes;
                        if (bopWorldTypes.contains(worldType)) {
                            BOP_WORLDTYPES.clear();
                            BOP_WORLDTYPES.addAll(bopWorldTypes);
                            bopWorldTypes.remove(worldType);
                            LOGGER.debug("Removed current world type from Biomes O' Plenty decoration blacklist: {}", worldType.getName());
                        }
                    }
                }

                // Check and maybe set if villages or strongholds should generate.
                configBiome(biome);

                // Replace the Overworld BiomeProvider with an instance of BiomeProviderSingle using our biome
                LOGGER.info("Replacing the BiomeProvider of the Overworld with BiomeProviderSingle using: {}", resloc);
                ObfuscationReflectionHelper.setPrivateValue(WorldProvider.class, world.provider, createBiomeProvider(biome), PROVIDER_FIELD);
            }
        }

        private static boolean verifyWorld(final World world)
        {
            return !world.isRemote && world.provider.getDimension() == 0;
        }

        private static BiomeProvider createBiomeProvider(final Biome biome)
        {
            final List<Biome> original = BiomeProvider.allowedBiomes;
            BiomeProvider.allowedBiomes = Collections.singletonList(biome);
            final BiomeProvider ret = new BiomeProviderSingle(biome);
            BiomeProvider.allowedBiomes = original;
            return ret;
        }

        private static void configBiome(final Biome biome)
        {
            if (LBConfig.getEnsureStrongholds() && !BiomeManager.strongHoldBiomes.contains(biome)) {
                resetStronghold = true;
                BiomeManager.addStrongholdBiome(biome);
                LOGGER.debug("Added {} to stronghold generation", biome.getRegistryName());
            }

            if (LBConfig.getEnsureVillages() && !MapGenVillage.VILLAGE_SPAWN_BIOMES.contains(biome)) {
                resetVillage = true;
                BiomeManager.addVillageBiome(biome, true);
                LOGGER.debug("Added {} to village generation", biome.getRegistryName());
            }
        }

        private static void resetBiome(final Biome biome)
        {
            if (resetStronghold) {
                resetStronghold = false;
                BiomeManager.removeStrongholdBiome(biome);
                LOGGER.debug("Removed {} from stronghold generation", biome.getRegistryName());
            }

            if (resetVillage) {
                resetVillage = false;
                BiomeManager.removeVillageBiome(biome);
                LOGGER.debug("Removed {} from village generation", biome.getRegistryName());
            }
        }
    }
}
