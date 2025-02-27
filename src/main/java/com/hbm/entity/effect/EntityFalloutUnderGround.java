package com.hbm.entity.effect;

import com.hbm.blocks.ModBlocks;
import com.hbm.config.BombConfig;
import com.hbm.config.RadiationConfig;
import com.hbm.config.VersatileConfig;
import com.hbm.interfaces.IConstantRenderer;
import com.hbm.render.amlfrom1710.Vec3;
import com.hbm.saveddata.AuxSavedData;
import com.hbm.util.ContaminationUtil;
import com.hbm.util.ContaminationUtil.ContaminationType;
import com.hbm.util.ContaminationUtil.HazardType;

//Chunkloading stuff
import java.util.ArrayList;
import java.util.List;
import com.hbm.entity.logic.IChunkLoader;
import com.hbm.main.MainRegistry;
import com.hbm.blocks.generic.WasteLog;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.common.ForgeChunkManager.Ticket;
import net.minecraftforge.common.ForgeChunkManager.Type;
import net.minecraft.util.math.ChunkPos;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.block.BlockHugeMushroom;
import net.minecraft.block.BlockSand;
import net.minecraft.block.BlockDirt;
import net.minecraft.block.BlockBush;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLog;
import net.minecraft.block.BlockLeaves;
import net.minecraft.block.state.IBlockState;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.world.World;

public class EntityFalloutUnderGround extends Entity implements IConstantRenderer, IChunkLoader {
	private static final DataParameter<Integer> SCALE = EntityDataManager.createKey(EntityFalloutUnderGround.class, DataSerializers.VARINT);
	public boolean done;
	private int maxSamples;
	private int currentSample;
	private int radius;

	private Ticket loaderTicket;

	private double s0;
	private double s1;
	private double s2;
	private double s3;
	private double s4;
	private double s5;
	private double s6;

	private double phi;

	public EntityFalloutUnderGround(World p_i1582_1_) {
		super(p_i1582_1_);
		this.setSize(4, 20);
		this.ignoreFrustumCheck = false;
		this.isImmuneToFire = true;
		this.phi = Math.PI * (3 - Math.sqrt(5));
		this.done = false;
		this.currentSample = 0;

	}

	@Override
	public AxisAlignedBB getRenderBoundingBox() {
		return new AxisAlignedBB(this.posX, this.posY, this.posZ, this.posX, this.posY, this.posZ);
	}

	@Override
	public boolean isInRangeToRender3d(double x, double y, double z) {
		return true;
	}

	@Override
	public boolean isInRangeToRenderDist(double distance) {
		return true;
	}

	public EntityFalloutUnderGround(World p_i1582_1_, int maxage) {
		super(p_i1582_1_);
		this.setSize(4, 20);
		this.isImmuneToFire = true;
		this.phi = (float)(Math.PI * (3 - Math.sqrt(5)));
		this.done = false;
		this.currentSample = 0;
	}

	@Override
	protected void entityInit() {
		init(ForgeChunkManager.requestTicket(MainRegistry.instance, world, Type.ENTITY));
		this.dataManager.register(SCALE, Integer.valueOf(0));
	}

	@Override
	public void init(Ticket ticket) {
		if(!world.isRemote) {
			
            if(ticket != null) {
            	
                if(loaderTicket == null) {
                	
                	loaderTicket = ticket;
                	loaderTicket.bindEntity(this);
                	loaderTicket.getModData();
                }

                ForgeChunkManager.forceChunk(loaderTicket, new ChunkPos(chunkCoordX, chunkCoordZ));
            }
        }
	}

	List<ChunkPos> loadedChunks = new ArrayList<ChunkPos>();
	@Override
	public void loadNeighboringChunks(int newChunkX, int newChunkZ) {
		if(!world.isRemote && loaderTicket != null)
        {
            for(ChunkPos chunk : loadedChunks)
            {
                ForgeChunkManager.unforceChunk(loaderTicket, chunk);
            }

            loadedChunks.clear();
            loadedChunks.add(new ChunkPos(newChunkX, newChunkZ));
            loadedChunks.add(new ChunkPos(newChunkX + 1, newChunkZ + 1));
            loadedChunks.add(new ChunkPos(newChunkX - 1, newChunkZ - 1));
            loadedChunks.add(new ChunkPos(newChunkX + 1, newChunkZ - 1));
            loadedChunks.add(new ChunkPos(newChunkX - 1, newChunkZ + 1));
            loadedChunks.add(new ChunkPos(newChunkX + 1, newChunkZ));
            loadedChunks.add(new ChunkPos(newChunkX, newChunkZ + 1));
            loadedChunks.add(new ChunkPos(newChunkX - 1, newChunkZ));
            loadedChunks.add(new ChunkPos(newChunkX, newChunkZ - 1));

            for(ChunkPos chunk : loadedChunks)
            {
                ForgeChunkManager.forceChunk(loaderTicket, chunk);
            }
        }
	}

	@Override
	public void onUpdate() {

		if(!world.isRemote) {
			ContaminationUtil.radiate(world, (int)posX, (int)posY, (int)posZ, radius, (float)Math.pow(radius, 2), 0);
			MutableBlockPos pos = new BlockPos.MutableBlockPos();
			int rayCounter = 0;
			for(int sample = currentSample; sample < this.maxSamples; sample++){
				this.currentSample = sample;
				if(rayCounter > BombConfig.mk4){
					break;
				}
				double fy = (2D * sample / (maxSamples - 1D)) - 1D;  // y goes from 1 to -1
		        double fr = Math.sqrt(1D - fy * fy);  // radius at y
		        double theta = phi * sample;  // golden angle increment

		        stompRadRay(pos, Math.cos(theta) * fr, fy, Math.sin(theta) * fr);
		        rayCounter++;
		    }

			if(this.currentSample >= this.maxSamples-1) {
				this.done=true;
				this.setDead();
			}
		}
	}


	private void stompRadRay(MutableBlockPos pos, double directionX, double directionY, double directionZ) {
		for(int l = 0; l < radius; l++) {
			pos.setPos(posX+directionX*l, posY+directionY*l, posZ+directionZ*l);
			if(world.isAirBlock(pos))
				continue;

			IBlockState b = world.getBlockState(pos);
			Block bblock = b.getBlock();

			if(bblock == Blocks.STONE) {
				if(l > s1)
					world.setBlockState(pos, ModBlocks.sellafield_slaked.getDefaultState());
				else if(l > s2)
					world.setBlockState(pos, ModBlocks.sellafield_0.getDefaultState());
				else if(l > s3)
					world.setBlockState(pos, ModBlocks.sellafield_1.getDefaultState());
				else if(l > s4)
					world.setBlockState(pos, ModBlocks.sellafield_2.getDefaultState());
				else if(l > s5)
					world.setBlockState(pos, ModBlocks.sellafield_3.getDefaultState());
				else if(l > s6)
					world.setBlockState(pos, ModBlocks.sellafield_4.getDefaultState());
				else if(l <= s6)
					world.setBlockState(pos, ModBlocks.sellafield_core.getDefaultState());
				return;

			} else if(bblock == Blocks.BEDROCK || bblock == ModBlocks.ore_bedrock_oil || bblock == ModBlocks.ore_bedrock_block){
				world.setBlockState(pos.add(0, 1, 0), ModBlocks.toxic_block.getDefaultState());
				return;
			
			} else if(bblock instanceof BlockLeaves) {
				if(l > s1){
					world.setBlockState(pos, ModBlocks.waste_leaves.getDefaultState());
				}
				else{
					world.setBlockToAir(pos);
					world.scheduleBlockUpdate(pos, world.getBlockState(pos).getBlock(), 0, 2);
				}
				continue;

			} else if(bblock instanceof BlockBush && world.getBlockState(pos.add(0, -1, 0)).getBlock() == Blocks.GRASS) {
				world.setBlockState(pos.add(0, -1, 0), ModBlocks.waste_earth.getDefaultState());
				world.setBlockState(pos, ModBlocks.waste_grass_tall.getDefaultState());
				continue;

			} else if(bblock == Blocks.GRASS) {
				world.setBlockState(pos, ModBlocks.waste_earth.getDefaultState());
				return;

			} else if(bblock == Blocks.DIRT) {
				BlockDirt.DirtType meta = b.getValue(BlockDirt.VARIANT);
				if(meta == BlockDirt.DirtType.DIRT)
					world.setBlockState(pos, ModBlocks.waste_dirt.getDefaultState());
				else if(meta == BlockDirt.DirtType.COARSE_DIRT)
					world.setBlockState(pos, ModBlocks.waste_gravel.getDefaultState());
				else if(meta == BlockDirt.DirtType.PODZOL)
					world.setBlockState(pos, ModBlocks.waste_mycelium.getDefaultState());
				return;

			} else if(bblock == Blocks.SNOW_LAYER) {
				world.setBlockState(pos, ModBlocks.fallout.getDefaultState());
				continue;

			} else if(bblock == Blocks.SNOW) {
				world.setBlockState(pos, ModBlocks.block_fallout.getDefaultState());
				continue;

			} else if(bblock == Blocks.MYCELIUM) {
				world.setBlockState(pos, ModBlocks.waste_mycelium.getDefaultState());
				return;

			} else if(bblock == Blocks.GRAVEL) {
				world.setBlockState(pos, ModBlocks.waste_gravel.getDefaultState());
				return;

			} else if(bblock == Blocks.SAND) {
				BlockSand.EnumType meta = b.getValue(BlockSand.VARIANT);
				if(rand.nextInt(60) == 0) {
					world.setBlockState(pos, meta == BlockSand.EnumType.SAND ? ModBlocks.waste_trinitite.getDefaultState() : ModBlocks.waste_trinitite_red.getDefaultState());
				} else {
					world.setBlockState(pos, meta == BlockSand.EnumType.SAND ? ModBlocks.waste_sand.getDefaultState() : ModBlocks.waste_sand_red.getDefaultState());
				}
				return;

			} else if(bblock == Blocks.CLAY) {
				world.setBlockState(pos, Blocks.HARDENED_CLAY.getDefaultState());
				return;

			} else if(bblock == Blocks.MOSSY_COBBLESTONE) {
				world.setBlockState(pos, Blocks.COAL_ORE.getDefaultState());
				return;

			} else if(bblock == Blocks.COAL_ORE) {
				if(l < s6){
					int ra = rand.nextInt(150);
					if(ra < 7) {
						world.setBlockState(pos, Blocks.DIAMOND_ORE.getDefaultState());
					} else if(ra < 10) {
						world.setBlockState(pos, Blocks.EMERALD_ORE.getDefaultState());
					}
				}
				return;

			} else if(bblock == Blocks.BROWN_MUSHROOM_BLOCK || bblock == Blocks.RED_MUSHROOM_BLOCK) {
				if(l < s0){
					BlockHugeMushroom.EnumType meta = b.getValue(BlockHugeMushroom.VARIANT);
					if(meta == BlockHugeMushroom.EnumType.STEM) {
						world.setBlockState(pos, ModBlocks.mush_block_stem.getDefaultState());
					} else {
						world.setBlockState(pos, ModBlocks.mush_block.getDefaultState());
					}
				}
				return;

			} else if(bblock instanceof BlockLog) {
				if(l < s0)
					world.setBlockState(pos, ((WasteLog)ModBlocks.waste_log).getSameRotationState(b));
				return;

			} else if(b.getMaterial() == Material.WOOD && bblock != ModBlocks.waste_log) {
				if(l < s0)
					world.setBlockState(pos, ModBlocks.waste_planks.getDefaultState());
				return;

			} else if(bblock == ModBlocks.ore_uranium) {
				if(l <= s6){
					if (rand.nextInt((int)(1+VersatileConfig.getSchrabOreChance())) == 0)
						world.setBlockState(pos, ModBlocks.ore_schrabidium.getDefaultState());
					else
						world.setBlockState(pos, ModBlocks.ore_uranium_scorched.getDefaultState());
				}
				return;

			} else if(bblock == ModBlocks.ore_nether_uranium) {
				if(l <= s5){
					if(rand.nextInt((int)(1+VersatileConfig.getSchrabOreChance())) == 0)
						world.setBlockState(pos, ModBlocks.ore_nether_schrabidium.getDefaultState());
					else
						world.setBlockState(pos, ModBlocks.ore_nether_uranium_scorched.getDefaultState());
				}
				return;

			} else if(bblock == ModBlocks.ore_gneiss_uranium) {
				if(l <= s4){
					if(rand.nextInt((int)(1+VersatileConfig.getSchrabOreChance()/2)) == 0)
						world.setBlockState(pos, ModBlocks.ore_gneiss_schrabidium.getDefaultState());
					else
						world.setBlockState(pos, ModBlocks.ore_gneiss_uranium_scorched.getDefaultState());
				}
				return;

			} else if(bblock == ModBlocks.brick_concrete) {
				if(rand.nextInt(60) == 0)
					world.setBlockState(pos, ModBlocks.brick_concrete_broken.getDefaultState());
				return;
			} else if(b.getMaterial() == Material.ROCK || b.getMaterial() == Material.IRON){
				return;
			}
		}
	}

	

	@Override
	protected void readEntityFromNBT(NBTTagCompound p_70037_1_) {
		setScale(p_70037_1_.getInteger("scale"));
		currentSample = p_70037_1_.getInteger("currentSample");
	}

	@Override
	protected void writeEntityToNBT(NBTTagCompound p_70014_1_) {
		p_70014_1_.setInteger("scale", getScale());
		p_70014_1_.setInteger("currentSample", currentSample);
	}

	public void setScale(int i) {
		this.dataManager.set(SCALE, Integer.valueOf(i));
		s0 = 0.9 * i;
		s1 = 0.75 * i;
		s2 = 0.6 * i;
		s3 = 0.4 * i;
		s4 = 0.3 * i;
		s5 = 0.2 * i;
		s6 = 0.1 * i;
		radius = i;
		maxSamples = (int)(Math.PI * Math.pow(i, 2));
	}

	public int getScale() {

		int scale = this.dataManager.get(SCALE);

		return scale == 0 ? 1 : scale;
	}
}
