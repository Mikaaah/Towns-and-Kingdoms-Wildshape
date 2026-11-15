package nl.townsandkingdoms.wildshape.world.worldgen.surface.condition;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.SurfaceRules.LazyXZCondition;
import nl.townsandkingdoms.wildshape.world.worldgen.GeneratorContext;
import nl.townsandkingdoms.wildshape.world.worldgen.WildshapeRandomState;
import nl.townsandkingdoms.wildshape.world.worldgen.cell.Cell;
import nl.townsandkingdoms.wildshape.world.worldgen.tile.Tile;
import nl.townsandkingdoms.wildshape.world.worldgen.util.PosUtil;

abstract class CellCondition extends LazyXZCondition {
	@Nullable
	private Tile.Chunk chunk;
	private long lastXZ;
	private boolean lastResult;
	
	@Nullable
	protected GeneratorContext generatorContext;
		
	public CellCondition(SurfaceRules.Context context) {
		super(context);
		//TODO store this in SurfaceRules$Context instead so we can cache the chunk lookup
		if((Object) context.randomState instanceof WildshapeRandomState randomState && (this.generatorContext = randomState.generatorContext()) != null) {
			ChunkPos chunkPos = context.chunk.getPos();
			this.chunk = this.generatorContext.cache.provideChunk(chunkPos.x, chunkPos.z);
		}
		this.lastXZ = Long.MIN_VALUE;
	}
		
	public abstract boolean test(Cell cell, int x, int z);
	
	@Override
	public boolean compute() {
        int x = this.context.blockX;
        int z = this.context.blockZ;
        long packedPos = PosUtil.pack(x, z);
        if(this.lastXZ != packedPos && this.generatorContext != null) {
        	this.lastXZ = packedPos;
            Cell cell = this.chunk.getCell(x, z);
        	this.lastResult = this.test(cell, x, z);
        }
        return this.lastResult;
	}
}
