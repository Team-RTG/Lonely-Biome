
package teamrtg.lonelybiome.world.gen.genlayer;

import net.minecraft.world.gen.layer.GenLayer;
import net.minecraft.world.gen.layer.IntCache;

/**
 * @author Zeno410
 */
public class GenLayerConstant extends GenLayer
{
	private final int value;
	
	public GenLayerConstant(int value)
	{
		super(0L);
		this.value = value;
	}

	@Override
	public int[] getInts(int par1, int par2, int par3, int par4){

	    int[] aint2 = IntCache.getIntCache(par3 * par4);
	    
	    for (int i = 0; i < aint2.length; i++) {
	    	
	        aint2[i] = value;
	    }
	    
	    return aint2;
	}
}
