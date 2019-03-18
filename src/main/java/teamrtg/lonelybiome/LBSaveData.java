package teamrtg.lonelybiome;

import javax.annotation.Nullable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.registry.ForgeRegistries;


public class LBSaveData extends WorldSavedData
{
    private static final Logger LOGGER    = LogManager.getLogger(LonelyBiome.MOD_ID + "/SaveData");
    private static final String DATA_NAME = LonelyBiome.MOD_ID.concat("_data");
    private static final String BIOME_KEY = "biome";

    @Nullable
    private Biome biome = null;

    @SuppressWarnings("WeakerAccess")
    public LBSaveData(final String name)
    {
        super(name);
    }

    @Nullable
    Biome getBiome()
    {
        return this.biome;
    }

    private LBSaveData setBiome(@Nullable final Biome biome)
    {
        this.biome = biome;
        this.markDirty();
        return this;
    }

    @Nullable
    static LBSaveData getSaveData(final World world)
    {
        return (LBSaveData)world.getPerWorldStorage().getOrLoadData(LBSaveData.class, DATA_NAME);
    }

    static LBSaveData initNewSaveData(final World world)
    {
        LOGGER.debug("Initialising new LonelyBiome save data object");
        final LBSaveData data = new LBSaveData(DATA_NAME).setBiome(LBConfig.getBiome());
        final MapStorage store = world.getPerWorldStorage();
        store.setData(DATA_NAME, data);
        store.saveAllData();
        return data;
    }

    @Override
    public void readFromNBT(final NBTTagCompound nbt)
    {
        LOGGER.debug("Deserialising LonelyBiome data from world save");
        String resloc = nbt.getString(BIOME_KEY);
        if (!resloc.isEmpty()) {
            this.biome = ForgeRegistries.BIOMES.getValue(new ResourceLocation(resloc));
            if (biome == null) {
                LOGGER.error("LonelyBiome world save data is corrupted or erroneous: [{}], Stopping server to preserve the world.", resloc);
                LOGGER.error("If the mod whose biome was used to create this world was removed, please reinstall it, or start a new world.");
                final MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
                server.getPlayerList().removeAllPlayers();
                server.worlds = new WorldServer[0];
                server.initiateShutdown();
                server.stopServer();
            }
        } else {
            LOGGER.warn("LonelyBiome is disabled as per empty world save data");
            this.biome = null;// ensure biome is null
        }
    }

    @Override
    public NBTTagCompound writeToNBT(final NBTTagCompound compound)
    {
        LOGGER.debug("Serialising LonelyBiome data to world save");
        if (this.biome != null) {
            ResourceLocation resloc = this.biome.getRegistryName();
            if (resloc != null) {
                compound.setString(BIOME_KEY, resloc.toString());
            } else {
                // this should be impossible
                throw new IllegalStateException(String.format("Biome is not registered: %s", this.biome));
            }
        } else {
            compound.setString(BIOME_KEY, "");
        }
        return compound;
    }
}
