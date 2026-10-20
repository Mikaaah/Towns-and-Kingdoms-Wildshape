package com.pandaismyname1.ultraterraforged.world.worldgen.cave;

import org.jetbrains.annotations.Nullable;
import java.util.BitSet;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.QuartPos;
import net.minecraft.resources.ResourceLocation;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import com.pandaismyname1.ultraterraforged.world.worldgen.GeneratorContext;
import com.pandaismyname1.ultraterraforged.world.worldgen.cell.Cell;
import com.pandaismyname1.ultraterraforged.world.worldgen.heightmap.Levels;
import com.pandaismyname1.ultraterraforged.world.worldgen.tile.Tile;

/**
 * Carving caves into one chunk, once its surface is built: hollowing out ellipsoids, lining them with other rock, and
 * reading the height of the ground inside and around the chunk.
 */
public final class CaveCarving {
	// no water in what's hollowed out
	public static final int DRY = Integer.MIN_VALUE;
	// the hollow may break through the surface
	public static final int OPEN = Integer.MIN_VALUE;
	private static final BlockState AIR = Blocks.AIR.defaultBlockState();
	private static final BlockState WATER = Blocks.WATER.defaultBlockState();

	public final ChunkAccess chunk;
	public final GeneratorContext context;
	public final Levels levels;
	public final int chunkX;
	public final int chunkZ;
	public final int minX;
	public final int minZ;
	public final int bottom;
	private final int[] ground = new int[256];
	private final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
	private final Cell cell = new Cell();
    private final BitSet carvedAir = new BitSet();
    // Cached biome quart samples avoid repeatedly decoding holders during huge caves.
    private final Map<Long, Boolean> protectedBiomeCache = new HashMap<>();

	// the chunk around this one that ground last read from its tile, or null if its tile wasn't there
	@Nullable
	private Tile.Chunk near;
	private int nearX = Integer.MIN_VALUE;
	private int nearZ = Integer.MIN_VALUE;

	CaveCarving(ChunkAccess chunk, GeneratorContext context) {
		this.chunk = chunk;
		this.context = context;
		this.levels = context.levels;
		ChunkPos chunkPos = chunk.getPos();
		this.chunkX = chunkPos.x;
		this.chunkZ = chunkPos.z;
		this.minX = chunkPos.getMinBlockX();
		this.minZ = chunkPos.getMinBlockZ();
		this.bottom = chunk.getMinBuildHeight();
		for (int dz = 0; dz < 16; dz++) {
			for (int dx = 0; dx < 16; dx++) {
				this.ground[dz << 4 | dx] = chunk.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, dx, dz);
			}
		}
	}

	/**
	 * The top block of the ground: from the chunk inside it, and from the terrain around it. For carving only: what's
	 * laid out across chunks must use terrainHeight, which is the same from every chunk.
	 */
	public int ground(float x, float z) {
		int bx = (int) Math.floor(x);
		int bz = (int) Math.floor(z);
		int dx = bx - this.minX;
		int dz = bz - this.minZ;
		if (dx >= 0 && dz >= 0 && dx < 16 && dz < 16) {
			return this.ground[dz << 4 | dx];
		}
		// the chunks next to this one are nearly always in tiles already generated: reading those is far cheaper than
		// working out the terrain afresh, which rock shelters would otherwise do over a thousand times a chunk. A chunk
		// can't finish its features, which lets its tile go, while one next to it is still being carved.
		int cx = bx >> 4;
		int cz = bz >> 4;
		if (this.near == null || cx != this.nearX || cz != this.nearZ) {
			Tile tile = this.context.cache != null ? this.context.cache.provideAtChunkIfPresent(cx, cz) : null;
			this.near = tile != null ? tile.getChunkReader(cx, cz) : null;
			this.nearX = cx;
			this.nearZ = cz;
		}
		return this.levels.scale(this.near != null ? this.near.getCell(bx & 15, bz & 15).height : this.cell(bx, bz).height);
	}

	/**
	 * The terrain at a place, straight from the heightmap, before erosion: the same whichever chunk asks, and without
	 * generating the tiles around it, so caves that reach across chunks are laid out the same in each. The cell is
	 * reused by the next call.
	 */
	public Cell cell(float x, float z) {
		this.context.lookup.compute(this.cell.reset(), (int) Math.floor(x), (int) Math.floor(z));
		return this.cell;
	}

	// the top block of the ground there, as cell gives it
	public int terrainHeight(float x, float z) {
		return this.levels.scale(this.cell(x, z).height);
	}

	// whether a box reaches into this chunk
	public boolean reaches(float minX, float minZ, float maxX, float maxZ) {
		return maxX >= this.minX && minX < this.minX + 16 && maxZ >= this.minZ && minZ < this.minZ + 16;
	}

	/**
	 * Hollows out an ellipsoid: water up to the given level, air above it. Bedrock and fluids are left alone, and so
	 * is the ground within roof blocks of the surface, unless roof is OPEN.
	 */
	public void hollow(float cx, float cy, float cz, float rx, float ry, float rz, int water, int roof) {
		int x0 = Math.max(this.minX, (int) Math.floor(cx - rx));
		int x1 = Math.min(this.minX + 15, (int) Math.ceil(cx + rx));
		int z0 = Math.max(this.minZ, (int) Math.floor(cz - rz));
		int z1 = Math.min(this.minZ + 15, (int) Math.ceil(cz + rz));
		if (x0 > x1 || z0 > z1) {
			return;
		}
		int y0 = Math.max(this.bottom + 1, (int) Math.floor(cy - ry));
		int y1 = (int) Math.ceil(cy + ry);
		for (int x = x0; x <= x1; x++) {
			float nx = (x + 0.5F - cx) / rx;
			for (int z = z0; z <= z1; z++) {
				float nz = (z + 0.5F - cz) / rz;
				float flat = nx * nx + nz * nz;
				if (flat >= 1.0F) {
					continue;
				}
				int top = roof == OPEN ? y1 : Math.min(y1, this.ground[(z - this.minZ) << 4 | (x - this.minX)] - roof);
				for (int y = y0; y <= top; y++) {
					float ny = (y + 0.5F - cy) / ry;
					if (flat + ny * ny >= 1.0F) {
						continue;
					}
					this.clear(x, y, z, water);
				}
			}
		}
	}

	/**
	 * Hollows out a single block, as hollow does.
	 */
	public void clear(int x, int y, int z, int water) {
        if (this.isAlexCavesBiome(x, y, z)) return;
		this.pos.set(x, y, z);
		BlockState state = this.chunk.getBlockState(this.pos);
		if (state.is(Blocks.BEDROCK) || !state.getFluidState().isEmpty()) {
			return;
		}
		BlockState fill = y <= water ? WATER : AIR;
		if (state != fill) {
            if (fill == AIR && !state.isAir() && y >= this.bottom && y < this.chunk.getMaxBuildHeight()) {
                this.carvedAir.set(((y - this.bottom) << 8) | ((z - this.minZ) << 4) | (x - this.minX));
            }
            this.chunk.setBlockState(this.pos, fill, false);
        }
	}

	/**
	 * Replaces the solid rock in a shell around an ellipsoid, out to the given thickness in blocks, with a lining,
	 * leaving the surface itself alone. Hollow the ellipsoid after lining it.
	 */
	public void line(float cx, float cy, float cz, float rx, float ry, float rz, float thickness, Lining lining) {
		float outerX = rx + thickness;
		float outerY = ry + thickness;
		float outerZ = rz + thickness;
		int x0 = Math.max(this.minX, (int) Math.floor(cx - outerX));
		int x1 = Math.min(this.minX + 15, (int) Math.ceil(cx + outerX));
		int z0 = Math.max(this.minZ, (int) Math.floor(cz - outerZ));
		int z1 = Math.min(this.minZ + 15, (int) Math.ceil(cz + outerZ));
		if (x0 > x1 || z0 > z1) {
			return;
		}
		int y0 = Math.max(this.bottom + 1, (int) Math.floor(cy - outerY));
		int y1 = (int) Math.ceil(cy + outerY);
		for (int x = x0; x <= x1; x++) {
			for (int z = z0; z <= z1; z++) {
				int top = Math.min(y1, this.ground[(z - this.minZ) << 4 | (x - this.minX)] - 1);
				for (int y = y0; y <= top; y++) {
					float ox = (x + 0.5F - cx) / outerX;
					float oy = (y + 0.5F - cy) / outerY;
					float oz = (z + 0.5F - cz) / outerZ;
					if (ox * ox + oy * oy + oz * oz >= 1.0F) {
						continue;
					}
					this.pos.set(x, y, z);
					BlockState state = this.chunk.getBlockState(this.pos);
					if (state.isAir() || state.is(Blocks.BEDROCK) || !state.getFluidState().isEmpty()) {
						continue;
					}
					if (!this.isAlexCavesBiome(x, y, z)) this.chunk.setBlockState(this.pos, lining.at(x, y, z), false);
				}
			}
		}
	}

	/**
	 * The cell of a column of this chunk, as the world was generated from it.
	 */
	public Cell tileCell(int dx, int dz) {
		return this.context.cache.provideAtChunk(this.chunkX, this.chunkZ).getChunkReader(this.chunkX, this.chunkZ).getCell(dx, dz);
	}

	public BlockState get(int x, int y, int z) {
		return this.chunk.getBlockState(this.pos.set(x, y, z));
	}

	public void set(int x, int y, int z, BlockState state) {
        if (this.isAlexCavesBiome(x, y, z)) return;
		this.chunk.setBlockState(this.pos.set(x, y, z), state, false);
	}

	// whether a block is in this chunk
	public boolean contains(int x, int z) {
		return x >= this.minX && z >= this.minZ && x < this.minX + 16 && z < this.minZ + 16;
	}


    /**
     * Optional native protection for Alex's Caves biomes. Requires no hard
     * dependency or class loading from Alex's Caves, on either loader.
     */
    private boolean isAlexCavesBiome(int x, int y, int z) {
        // No Alex biomes are present unless the optional mod is installed.
        int qx=QuartPos.fromBlock(x), qy=QuartPos.fromBlock(y), qz=QuartPos.fromBlock(z);
        long key = net.minecraft.core.BlockPos.asLong(qx, qy, qz);
        Boolean cached = this.protectedBiomeCache.get(key);
        if (cached != null) return cached;
        var biomeHolder = this.chunk.getNoiseBiome(qx, qy, qz);
        // Synthetic test chunks and unfinished worldgen may return no holder.
        boolean protectedBiome = biomeHolder != null && biomeHolder.unwrapKey()
            .map(biome -> "alexscaves".equals(biome.location().getNamespace())).orElse(false);
        this.protectedBiomeCache.put(key, protectedBiome);
        return protectedBiome;
    }

    /** Reconcile newly carved air connected to water sources at buildSurface.
     * Disconnected dry cave systems stay dry. Flood cannot rise above the
     * adjacent source water block. Chunk edges are marked for fluid processing.
     */
    public void reconcileWater() {
        if (this.carvedAir.isEmpty()) return;
        // Short offsets are safe for supported Minecraft world heights.
        // Most giant-cave chunks have no exposed fluid connection.
        // Build the seed queue before allocating a per-block water-height table.
        LongArrayFIFOQueue pending = new LongArrayFIFOQueue();
        for (int bit=this.carvedAir.nextSetBit(0); bit>=0; bit=this.carvedAir.nextSetBit(bit+1)) {
            int x=this.minX+(bit&15), z=this.minZ+((bit>>>4)&15), y=this.bottom+(bit>>>8);
            int ceiling = Integer.MIN_VALUE;
            if (y+1 < this.chunk.getMaxBuildHeight() && this.isWater(x,y+1,z)) ceiling=y+1;
            if (x>this.minX && this.isWater(x-1,y,z)) ceiling=Math.max(ceiling,y);
            if (x<this.minX+15 && this.isWater(x+1,y,z)) ceiling=Math.max(ceiling,y);
            if (z>this.minZ && this.isWater(x,y,z-1)) ceiling=Math.max(ceiling,y);
            if (z<this.minZ+15 && this.isWater(x,y,z+1)) ceiling=Math.max(ceiling,y);
            if (ceiling>=y) pending.enqueue(packWater(bit,ceiling));
        }
        if (pending.isEmpty()) return;
        int len = (this.chunk.getMaxBuildHeight() - this.bottom) << 8;
        short[] highestWater = new short[len];
        Arrays.fill(highestWater, (short) -1);
        BitSet filled = new BitSet();
        while(!pending.isEmpty()) {
            long candidate=pending.dequeueLong();
            int bit=(int)candidate, ceiling=(int)(candidate>>>32);
            if(bit<0 || bit>=len || highestWater[bit]>=ceiling-this.bottom) continue;
            int x=this.minX+(bit&15), z=this.minZ+((bit>>>4)&15), y=this.bottom+(bit>>>8);
            if(y>ceiling || this.isAlexCavesBiome(x,y,z)) continue;
            BlockState state=this.chunk.getBlockState(this.pos.set(x,y,z));
            if(!state.isAir() && !filled.get(bit)) continue;
            highestWater[bit]=(short)(ceiling-this.bottom);
            if(state.isAir()) {
                this.chunk.setBlockState(this.pos, WATER, false);
                filled.set(bit);
                if(x==this.minX || x==this.minX+15 || z==this.minZ || z==this.minZ+15) {
                    this.chunk.markPosForPostprocessing(this.pos);
                }
            }
            if((bit&15)>0) pending.enqueue(packWater(bit-1,ceiling));
            if((bit&15)<15) pending.enqueue(packWater(bit+1,ceiling));
            if(((bit>>>4)&15)>0) pending.enqueue(packWater(bit-16,ceiling));
            if(((bit>>>4)&15)<15) pending.enqueue(packWater(bit+16,ceiling));
            if(y>this.bottom) pending.enqueue(packWater(bit-256,ceiling));
            if(y<ceiling && y+1<this.chunk.getMaxBuildHeight()) pending.enqueue(packWater(bit+256,ceiling));
        }
    }

    private static long packWater(int bit, int ceiling) {
        return ((long)ceiling<<32) | (bit & 0xFFFFFFFFL);
    }

    private boolean isWater(int x,int y,int z) {
        return this.chunk.getBlockState(this.pos.set(x,y,z)).getFluidState()
            .is(net.minecraft.tags.FluidTags.WATER);
    }

	/**
	 * What lines a cave, which may vary from block to block.
	 */
	public interface Lining {
		BlockState at(int x, int y, int z);
	}
}
