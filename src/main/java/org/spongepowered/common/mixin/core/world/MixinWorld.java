/*
 * This file is part of Sponge, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.spongepowered.common.mixin.core.world;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.flowpowered.math.vector.Vector2i;
import com.flowpowered.math.vector.Vector3d;
import com.flowpowered.math.vector.Vector3i;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityHanging;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.boss.EntityDragonPart;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.item.EntityEnderPearl;
import net.minecraft.entity.item.EntityFallingBlock;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityPainting;
import net.minecraft.entity.item.EntityPainting.EnumArt;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.projectile.EntityPotion;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.Packet;
import net.minecraft.profiler.Profiler;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.gui.IUpdatePlayerListBox;
import net.minecraft.server.management.ServerConfigurationManager;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.biome.WorldChunkManager;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraft.world.storage.WorldInfo;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.spongepowered.api.Platform;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.tileentity.TileEntity;
import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.DataHolder;
import org.spongepowered.api.data.DataTransactionResult;
import org.spongepowered.api.data.DataView;
import org.spongepowered.api.data.Property;
import org.spongepowered.api.data.key.Key;
import org.spongepowered.api.data.manipulator.DataManipulator;
import org.spongepowered.api.data.manipulator.ImmutableDataManipulator;
import org.spongepowered.api.data.merge.MergeFunction;
import org.spongepowered.api.data.property.PropertyStore;
import org.spongepowered.api.data.value.BaseValue;
import org.spongepowered.api.data.value.immutable.ImmutableValue;
import org.spongepowered.api.data.value.mutable.Value;
import org.spongepowered.api.effect.particle.ParticleEffect;
import org.spongepowered.api.effect.sound.SoundType;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntitySnapshot;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.projectile.EnderPearl;
import org.spongepowered.api.entity.projectile.source.ProjectileSource;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.event.block.NotifyNeighborBlockEvent;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.NamedCause;
import org.spongepowered.api.event.entity.DestructEntityEvent;
import org.spongepowered.api.service.permission.context.Context;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.text.chat.ChatType;
import org.spongepowered.api.text.sink.MessageSink;
import org.spongepowered.api.text.sink.MessageSinks;
import org.spongepowered.api.text.title.Title;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.util.DiscreteTransform3;
import org.spongepowered.api.util.Functional;
import org.spongepowered.api.util.PositionOutOfBoundsException;
import org.spongepowered.api.util.annotation.NonnullByDefault;
import org.spongepowered.api.util.persistence.InvalidDataException;
import org.spongepowered.api.world.Chunk;
import org.spongepowered.api.world.Dimension;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.WorldBorder;
import org.spongepowered.api.world.WorldCreationSettings;
import org.spongepowered.api.world.biome.BiomeType;
import org.spongepowered.api.world.difficulty.Difficulty;
import org.spongepowered.api.world.explosion.Explosion;
import org.spongepowered.api.world.extent.Extent;
import org.spongepowered.api.world.gen.BiomeGenerator;
import org.spongepowered.api.world.gen.GeneratorPopulator;
import org.spongepowered.api.world.gen.Populator;
import org.spongepowered.api.world.gen.WorldGenerator;
import org.spongepowered.api.world.gen.WorldGeneratorModifier;
import org.spongepowered.api.world.storage.WorldProperties;
import org.spongepowered.api.world.weather.Weather;
import org.spongepowered.api.world.weather.Weathers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.common.SpongeImpl;
import org.spongepowered.common.SpongeImplFactory;
import org.spongepowered.common.block.SpongeBlockSnapshotBuilder;
import org.spongepowered.common.config.SpongeConfig;
import org.spongepowered.common.config.SpongeConfig.WorldConfig;
import org.spongepowered.common.data.property.SpongePropertyRegistry;
import org.spongepowered.common.effect.particle.SpongeParticleEffect;
import org.spongepowered.common.effect.particle.SpongeParticleHelper;
import org.spongepowered.common.event.SpongeCommonEventFactory;
import org.spongepowered.common.interfaces.IMixinEntityPlayer;
import org.spongepowered.common.interfaces.IMixinWorld;
import org.spongepowered.common.interfaces.IMixinWorldInfo;
import org.spongepowered.common.interfaces.IMixinWorldSettings;
import org.spongepowered.common.interfaces.IMixinWorldType;
import org.spongepowered.common.interfaces.block.IMixinBlock;
import org.spongepowered.common.registry.provider.DirectionFacingProvider;
import org.spongepowered.common.registry.type.world.DimensionRegistryModule;
import org.spongepowered.common.scoreboard.SpongeScoreboard;
import org.spongepowered.common.util.SpongeHooks;
import org.spongepowered.common.util.StaticMixinHelper;
import org.spongepowered.common.util.VecHelper;
import org.spongepowered.common.world.CauseTracker;
import org.spongepowered.common.world.DimensionManager;
import org.spongepowered.common.world.border.PlayerBorderListener;
import org.spongepowered.common.world.extent.ExtentViewDownsize;
import org.spongepowered.common.world.extent.ExtentViewTransform;
import org.spongepowered.common.world.gen.CustomChunkProviderGenerate;
import org.spongepowered.common.world.gen.CustomWorldChunkManager;
import org.spongepowered.common.world.gen.SpongeBiomeGenerator;
import org.spongepowered.common.world.gen.SpongeGeneratorPopulator;
import org.spongepowered.common.world.gen.SpongeWorldGenerator;
import org.spongepowered.common.world.storage.SpongeChunkLayout;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

@NonnullByDefault
@Mixin(net.minecraft.world.World.class)
public abstract class MixinWorld implements World, IMixinWorld {

    private static final Vector3i BLOCK_MIN = new Vector3i(-30000000, 0, -30000000);
    private static final Vector3i BLOCK_MAX = new Vector3i(30000000, 256, 30000000).sub(1, 1, 1);
    private static final Vector3i BLOCK_SIZE = BLOCK_MAX.sub(BLOCK_MIN).add(1, 1, 1);
    private static final Vector2i BIOME_MIN = BLOCK_MIN.toVector2(true);
    private static final Vector2i BIOME_MAX = BLOCK_MAX.toVector2(true);
    private static final Vector2i BIOME_SIZE = BIOME_MAX.sub(BIOME_MIN).add(1, 1);
    private long weatherStartTime;
    private boolean keepSpawnLoaded;
    private boolean worldSpawnerRunning;
    private boolean chunkSpawnerRunning;
    public SpongeConfig<WorldConfig> worldConfig;
    @Nullable private volatile Context worldContext;
    private ImmutableList<Populator> populators;
    private ImmutableList<GeneratorPopulator> generatorPopulators;

    protected SpongeScoreboard spongeScoreboard = new SpongeScoreboard();
    private final CauseTracker causeTracker = new CauseTracker((World) this);

    // @formatter:off
    @Shadow public Profiler theProfiler;
    @Shadow public boolean isRemote;
    @Shadow protected boolean scheduledUpdatesAreImmediate;
    @Shadow public WorldProvider provider;
    @Shadow protected WorldInfo worldInfo;
    @Shadow public Random rand;
    @Shadow public List<net.minecraft.entity.Entity> loadedEntityList;
    @Shadow public Scoreboard worldScoreboard;
    @Shadow public List<net.minecraft.tileentity.TileEntity> loadedTileEntityList;
    @Shadow private net.minecraft.world.border.WorldBorder worldBorder;
    @Shadow public List playerEntities;

    @Shadow(prefix = "shadow$") public abstract net.minecraft.world.border.WorldBorder shadow$getWorldBorder();
    @Shadow(prefix = "shadow$") public abstract EnumDifficulty shadow$getDifficulty();
    @Shadow public abstract void onEntityAdded(net.minecraft.entity.Entity entityIn);
    @Shadow public abstract boolean isAreaLoaded(int xStart, int yStart, int zStart, int xEnd, int yEnd, int zEnd, boolean allowEmpty);
    @Shadow public abstract void updateEntity(net.minecraft.entity.Entity ent);
    @Shadow public abstract boolean isValid(BlockPos pos);
    @Shadow public abstract net.minecraft.world.chunk.Chunk getChunkFromBlockCoords(BlockPos pos);
    @Shadow public abstract boolean checkLight(BlockPos pos);
    @Shadow public abstract void markBlockForUpdate(BlockPos pos);
    //@Shadow public abstract void updateComparatorOutputLevel(BlockPos pos, Block blockIn);
    @Shadow public abstract void notifyNeighborsRespectDebug(BlockPos pos, Block blockType);
    @Shadow public abstract boolean isBlockLoaded(BlockPos pos);
    @Shadow public abstract boolean addWeatherEffect(net.minecraft.entity.Entity entityIn);
    @Shadow public abstract void playSoundEffect(double x, double y, double z, String soundName, float volume, float pitch);
    @Shadow public abstract BiomeGenBase getBiomeGenForCoords(BlockPos pos);
    @Shadow public abstract IChunkProvider getChunkProvider();
    @Shadow public abstract WorldChunkManager getWorldChunkManager();
    @Shadow public abstract net.minecraft.tileentity.TileEntity getTileEntity(BlockPos pos);
    @Shadow public abstract boolean isBlockPowered(BlockPos pos);
    @Shadow public abstract IBlockState getBlockState(BlockPos pos);
    @Shadow public abstract net.minecraft.world.chunk.Chunk getChunkFromChunkCoords(int chunkX, int chunkZ);
    @Shadow public abstract boolean isChunkLoaded(int x, int z, boolean allowEmpty);
    @Shadow public abstract net.minecraft.world.Explosion newExplosion(net.minecraft.entity.Entity entityIn, double x, double y, double z, float strength,
            boolean isFlaming, boolean isSmoking);
    @Shadow public abstract List<net.minecraft.entity.Entity> getEntities(Class<net.minecraft.entity.Entity> entityType,
            com.google.common.base.Predicate<net.minecraft.entity.Entity> filter);

    // @formatter:on

    @Inject(method = "<init>", at = @At("RETURN") )
    public void onConstructed(ISaveHandler saveHandlerIn, WorldInfo info, WorldProvider providerIn, Profiler profilerIn, boolean client,
            CallbackInfo ci) {
        if (!client) {
            String providerName = providerIn.getDimensionName().toLowerCase().replace(" ", "_").replace("[^A-Za-z0-9_]", "");
            this.worldConfig =
                    new SpongeConfig<>(SpongeConfig.Type.WORLD,
                            SpongeImpl.getSpongeConfigDir()
                                    .resolve("worlds")
                                    .resolve(providerName)
                                    .resolve((providerIn.getDimensionId() == 0 ? "DIM0"
                                            : DimensionRegistryModule.getInstance().getWorldFolder(providerIn.getDimensionId())))
                                    .resolve("world.conf"),
                            SpongeImpl.ECOSYSTEM_ID);
            ((IMixinWorldInfo) info).setWorldConfig(this.worldConfig.getConfig());
            this.keepSpawnLoaded = ((WorldProperties) info).doesKeepSpawnLoaded();
        }

        if (SpongeImpl.getGame().getPlatform().getType() == Platform.Type.SERVER) {
            this.worldBorder.addListener(new PlayerBorderListener());
        }
    }

    @Override
    public CauseTracker getCauseTracker() {
        return this.causeTracker;
    }

    @Overwrite
    public boolean setBlockState(BlockPos pos, IBlockState newState, int flags) {
        if (!this.isValid(pos)) {
            return false;
        } else if (!this.isRemote && this.worldInfo.getTerrainType() == WorldType.DEBUG_WORLD) {
            return false;
        } else {
            return this.causeTracker.setBlockState(pos, newState, flags);
        }
    }

    /**
     * @author bloodmc - November 15th, 2015
     *
     *         Purpose: Rewritten to pass the source block position.
     */
    @Overwrite
    public void notifyNeighborsOfStateChange(BlockPos pos, Block blockType) {
        if (this.isRemote) {
            for (EnumFacing facing : EnumFacing.values()) {
                this.causeTracker.notifyBlockOfStateChange(pos.offset(facing), blockType, pos);
            }
            return;
        }

        NotifyNeighborBlockEvent event =
                SpongeCommonEventFactory.callNotifyNeighborEvent(this, pos, java.util.EnumSet.allOf(EnumFacing.class));
        if (event.isCancelled()) {
            return;
        }

        for (EnumFacing facing : EnumFacing.values()) {
            if (event.getNeighbors().keySet().contains(DirectionFacingProvider.getInstance().getKey(facing).get())) {
                this.causeTracker.notifyBlockOfStateChange(pos.offset(facing), blockType, pos);
            }
        }
    }

    /**
     * @author bloodmc - November 15th, 2015
     *
     *         Purpose: Rewritten to pass the source block position.
     */
    @SuppressWarnings("rawtypes")
    @Overwrite
    public void notifyNeighborsOfStateExcept(BlockPos pos, Block blockType, EnumFacing skipSide) {
        EnumSet directions = EnumSet.allOf(EnumFacing.class);
        directions.remove(skipSide);

        if (this.isRemote) {
            for (Object obj : directions) {
                EnumFacing facing = (EnumFacing) obj;
                this.causeTracker.notifyBlockOfStateChange(pos.offset(facing), blockType, pos);
            }
            return;
        }

        NotifyNeighborBlockEvent event = SpongeCommonEventFactory.callNotifyNeighborEvent(this, pos, directions);
        if (event.isCancelled()) {
            return;
        }

        for (EnumFacing facing : EnumFacing.values()) {
            if (event.getNeighbors().keySet().contains(DirectionFacingProvider.getInstance().getKey(facing).get())) {
                this.causeTracker.notifyBlockOfStateChange(pos.offset(facing), blockType, pos);
            }
        }
    }

    /**
     * @author bloodmc - November 15th, 2015
     *
     *         Purpose: Redirect's vanilla method to ours that includes source
     *         block position.
     */
    @Overwrite
    public void notifyBlockOfStateChange(BlockPos notifyPos, final Block blockIn) {
        this.causeTracker.notifyBlockOfStateChange(notifyPos, blockIn, null);
    }

    @Override
    public long getRunningDuration() {
        return this.worldInfo.getWorldTotalTime() - this.weatherStartTime;
    }

    @Shadow
    public abstract int getSkylightSubtracted();

    @Shadow
    public abstract int getLightFor(EnumSkyBlock type, BlockPos pos);

    @SuppressWarnings("rawtypes")
    @Inject(method = "getCollidingBoundingBoxes(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/AxisAlignedBB;)Ljava/util/List;", at = @At("HEAD") , cancellable = true)
    public
            void onGetCollidingBoundingBoxes(net.minecraft.entity.Entity entity, AxisAlignedBB axis,
                    CallbackInfoReturnable<List> cir) {
        if (!entity.worldObj.isRemote && SpongeHooks.checkBoundingBoxSize(entity, axis)) {
            // Removing misbehaved living entities
            cir.setReturnValue(new ArrayList());
        }
    }

    @Inject(method = "onEntityRemoved", at = @At(value = "HEAD") )
    public void onEntityRemoval(net.minecraft.entity.Entity entityIn, CallbackInfo ci) {
        MessageSink sink = MessageSinks.toNone();
        MessageSink originalSink = MessageSinks.toNone();
        Text originalMessage = Texts.of();
        Text message = Texts.of();

        if (entityIn.isDead && entityIn.getEntityId() != StaticMixinHelper.lastDestroyedEntityId && !(entityIn instanceof EntityLivingBase)) {

            DestructEntityEvent event = SpongeEventFactory.createDestructEntityEvent(SpongeImpl.getGame(), Cause.of(this), originalMessage, message,
                    originalSink, sink, (Entity) entityIn);
            SpongeImpl.getGame().getEventManager().post(event);
            Text returned = Texts.format(event.getMessage());
            if (returned != Texts.of()) {
                event.getSink().sendMessage(returned);
            }
        }
    }

    @Redirect(method = "forceBlockUpdateTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/Block;updateTick(Lnet/minecraft/world/World;Lnet/minecraft/util/BlockPos;Lnet/minecraft/block/state/IBlockState;Ljava/util/Random;)V") )
    public void onForceBlockUpdateTick(Block block, net.minecraft.world.World worldIn, BlockPos pos, IBlockState state, Random rand) {
        if (this.isRemote || this.causeTracker.isCapturingBlock() || ((IMixinWorld) worldIn).getCauseTracker().capturingTerrainGen()) {
            block.updateTick(worldIn, pos, state, rand);
            return;
        }

        this.causeTracker.captureTickingBlock(this.causeTracker.createSpongeBlockSnapshot(state, state.getBlock().getActualState(state, (IBlockAccess) this, pos), pos, 0));
        block.updateTick(worldIn, pos, state, rand);
        this.causeTracker.handlePostTickCaptures(Cause.of(NamedCause.source(this.causeTracker.currentTickBlock)));
        this.causeTracker.finishTickingBlockCapture();
    }

    @Redirect(method = "updateEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;onUpdate()V") )
    public void onUpdateEntities(net.minecraft.entity.Entity entityIn) {
        if (this.isRemote || this.causeTracker.isCapturingEntity()) {
            entityIn.onUpdate();
            return;
        }

        this.causeTracker.captureTickingEntity((Entity) entityIn);
        entityIn.onUpdate();
        this.causeTracker.handlePostTickCaptures(Cause.of(NamedCause.source(entityIn)));
        this.causeTracker.finishTickingEntityCapture();
    }

    @Redirect(method = "updateEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/gui/IUpdatePlayerListBox;update()V") )
    public void onUpdateTileEntities(IUpdatePlayerListBox tile) {
        if (this.isRemote || this.causeTracker.isCapturingTile()) {
            tile.update();
            return;
        }

        this.causeTracker.captureTickingTile((TileEntity) tile);
        tile.update();
        this.causeTracker.handlePostTickCaptures(Cause.of(NamedCause.source(tile)));
        this.causeTracker.finishTickingTileCapture();
    }

    @Redirect(method = "updateEntityWithOptionalForce", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;onUpdate()V") )
    public void onCallEntityUpdate(net.minecraft.entity.Entity entity) {
        if (this.isRemote || this.causeTracker.isCapturingEntity() || StaticMixinHelper.packetPlayer != null) {
            entity.onUpdate();
            return;
        }

        this.causeTracker.captureTickingEntity((Entity) entity);
        entity.onUpdate();
        this.causeTracker.handlePostTickCaptures(Cause.of(NamedCause.source(entity)));
        this.causeTracker.finishTickingEntityCapture();
    }

    /**
     * @author bloodmc
     *
     *         Purpose: Redirects vanilla method to our method which includes a
     *         cause.
     */
    @Overwrite
    public boolean spawnEntityInWorld(net.minecraft.entity.Entity entity) {
        return spawnEntity((Entity) entity, Cause.of(NamedCause.source(this)));
    }
    @Override
    public boolean spawnEntity(Entity entity, Cause cause) {
        checkNotNull(entity, "Entity cannot be null!");
        checkNotNull(cause, "Cause cannot be null!");
        return this.causeTracker.spawnEntity(entity, cause);
    }

    /**
     * @author bloodmc - November 15th, 2015
     *
     *         Purpose: Used to track comparators when they update levels.
     */
    @Overwrite
    public void updateComparatorOutputLevel(BlockPos pos, Block blockIn) {
        SpongeImplFactory.updateComparatorOutputLevel((net.minecraft.world.World) (Object) this, pos, blockIn);
    }

    @Override
    public UUID getUniqueId() {
        return ((WorldProperties) this.worldInfo).getUniqueId();
    }

    @Override
    public String getName() {
        return this.worldInfo.getWorldName();
    }

    @Override
    public Optional<Chunk> getChunk(Vector3i position) {
        return getChunk(position.getX(), position.getY(), position.getZ());
    }

    @Override
    public Optional<Chunk> getChunk(int x, int y, int z) {
        if (!SpongeChunkLayout.instance.isValidChunk(x, y, z)) {
            return Optional.empty();
        }
        WorldServer worldserver = (WorldServer) (Object) this;
        net.minecraft.world.chunk.Chunk chunk = null;
        if (worldserver.theChunkProviderServer.chunkExists(x, z)) {
            chunk = worldserver.theChunkProviderServer.provideChunk(x, z);
        }
        return Optional.ofNullable((Chunk) chunk);
    }

    @Override
    public Optional<Chunk> loadChunk(Vector3i position, boolean shouldGenerate) {
        return loadChunk(position.getX(), position.getY(), position.getZ(), shouldGenerate);
    }

    @Override
    public Optional<Chunk> loadChunk(int x, int y, int z, boolean shouldGenerate) {
        if (!SpongeChunkLayout.instance.isValidChunk(x, y, z)) {
            return Optional.empty();
        }
        WorldServer worldserver = (WorldServer) (Object) this;
        net.minecraft.world.chunk.Chunk chunk = null;
        if (worldserver.theChunkProviderServer.chunkExists(x, z) || shouldGenerate) {
            chunk = worldserver.theChunkProviderServer.loadChunk(x, z);
        }
        return Optional.ofNullable((Chunk) chunk);
    }

    @Override
    public BlockState getBlock(int x, int y, int z) {
        checkBlockBounds(x, y, z);
        IBlockState state = getBlockState(new BlockPos(x, y, z));
        if (((IMixinBlock) state.getBlock()).forceUpdateBlockState()) {
            BlockState updatedState = (BlockState) state.getBlock().getActualState(state, (IBlockAccess) this, new BlockPos(x, y, z));
            return updatedState;
        } else {
            return (BlockState) state;
        }
    }

    @Override
    public BlockType getBlockType(int x, int y, int z) {
        checkBlockBounds(x, y, z);
        // avoid intermediate object creation from using BlockState
        return (BlockType) getChunkFromChunkCoords(x >> 4, z >> 4).getBlock(x, y, z);
    }

    @Override
    public void setBlock(int x, int y, int z, BlockState block) {
        checkBlockBounds(x, y, z);
        SpongeHooks.setBlockState(((net.minecraft.world.World) (Object) this), x, y, z, block);
    }

    @Override
    public BiomeType getBiome(int x, int z) {
        checkBiomeBounds(x, z);
        return (BiomeType) this.getBiomeGenForCoords(new BlockPos(x, 0, z));
    }

    @Override
    public void setBiome(int x, int z, BiomeType biome) {
        checkBiomeBounds(x, z);
        ((Chunk) getChunkFromChunkCoords(x >> 4, z >> 4)).setBiome(x, z, biome);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Collection<Entity> getEntities() {
        return Lists.newArrayList((Collection<Entity>) (Object) this.loadedEntityList);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Collection<Entity> getEntities(Predicate<Entity> filter) {
        // This already returns a new copy
        return (Collection<Entity>) (Object) this.getEntities(net.minecraft.entity.Entity.class,
                Functional.java8ToGuava((Predicate<net.minecraft.entity.Entity>) (Object) filter));
    }

    @Override
    public Optional<Entity> createEntity(EntityType type, Vector3d position) {
        checkNotNull(type, "The entity type cannot be null!");
        checkNotNull(position, "The position cannot be null!");

        Entity entity = null;

        Class<? extends Entity> entityClass = type.getEntityClass();
        double x = position.getX();
        double y = position.getY();
        double z = position.getZ();

        if (entityClass.isAssignableFrom(EntityPlayerMP.class) || entityClass.isAssignableFrom(EntityDragonPart.class)) {
            // Unable to construct these
            return Optional.empty();
        }

        net.minecraft.world.World world = (net.minecraft.world.World) (Object) this;

        // Not all entities have a single World parameter as their constructor
        if (entityClass.isAssignableFrom(EntityLightningBolt.class)) {
            entity = (Entity) new EntityLightningBolt(world, x, y, z);
        } else if (entityClass.isAssignableFrom(EntityEnderPearl.class)) {
            EntityArmorStand tempEntity = new EntityArmorStand(world, x, y, z);
            tempEntity.posY -= tempEntity.getEyeHeight();
            entity = (Entity) new EntityEnderPearl(world, tempEntity);
            ((EnderPearl) entity).setShooter(ProjectileSource.UNKNOWN);
        }

        // Some entities need to have non-null fields (and the easiest way to
        // set them is to use the more specialised constructor).
        if (entityClass.isAssignableFrom(EntityFallingBlock.class)) {
            entity = (Entity) new EntityFallingBlock(world, x, y, z, Blocks.sand.getDefaultState());
        } else if (entityClass.isAssignableFrom(EntityItem.class)) {
            entity = (Entity) new EntityItem(world, x, y, z, new ItemStack(Blocks.stone));
        }

        if (entity == null) {
            try {
                entity = ConstructorUtils.invokeConstructor(entityClass, this);
                ((net.minecraft.entity.Entity) entity).setPosition(x, y, z);
            } catch (Exception e) {
                SpongeImpl.getLogger().error(ExceptionUtils.getStackTrace(e));
            }
        }

        if (entity instanceof EntityHanging) {
            if (((EntityHanging) entity).facingDirection == null) {
                // TODO Some sort of detection of a valid direction?
                // i.e scan immediate blocks for something to attach onto.
                ((EntityHanging) entity).facingDirection = EnumFacing.NORTH;
            }
            if (!((EntityHanging) entity).onValidSurface()) {
                return Optional.empty();
            }
        }

        // Last chance to fix null fields
        if (entity instanceof EntityPotion) {
            // make sure EntityPotion.potionDamage is not null
            ((EntityPotion) entity).getPotionDamage();
        } else if (entity instanceof EntityPainting) {
            // This is default when art is null when reading from NBT, could
            // choose a random art instead?
            ((EntityPainting) entity).art = EnumArt.KEBAB;
        }

        return Optional.ofNullable(entity);
    }

    @Override
    public Optional<Entity> createEntity(EntityType type, Vector3i position) {
        return this.createEntity(type, position.toDouble());
    }

    @Override
    public Optional<Entity> createEntity(DataContainer entityContainer) {
        // TODO once entity containers are implemented
        return Optional.empty();
    }

    @Override
    public Optional<Entity> createEntity(DataContainer entityContainer, Vector3d position) {
        // TODO once entity containers are implemented
        return Optional.empty();
    }

    @Override
    public Optional<Entity> restoreSnapshot(EntitySnapshot snapshot, Vector3d position) {
        EntitySnapshot entitySnapshot = snapshot.withLocation(new Location<>(this, position));
        return entitySnapshot.restore();
    }

    @Override
    public WorldBorder getWorldBorder() {
        return (WorldBorder) shadow$getWorldBorder();
    }

    @Override
    public void spawnParticles(ParticleEffect particleEffect, Vector3d position) {
        this.spawnParticles(particleEffect, position, Integer.MAX_VALUE);
    }

    @Override
    public void spawnParticles(ParticleEffect particleEffect, Vector3d position, int radius) {
        checkNotNull(particleEffect, "The particle effect cannot be null!");
        checkNotNull(position, "The position cannot be null");
        checkArgument(radius > 0, "The radius has to be greater then zero!");

        List<Packet> packets = SpongeParticleHelper.toPackets((SpongeParticleEffect) particleEffect, position);

        if (!packets.isEmpty()) {
            ServerConfigurationManager manager = MinecraftServer.getServer().getConfigurationManager();

            double x = position.getX();
            double y = position.getY();
            double z = position.getZ();

            for (Packet packet : packets) {
                manager.sendToAllNear(x, y, z, radius, this.provider.getDimensionId(), packet);
            }
        }
    }

    @Override
    public Weather getWeather() {
        if (this.worldInfo.isThundering()) {
            return Weathers.THUNDER_STORM;
        } else if (this.worldInfo.isRaining()) {
            return Weathers.RAIN;
        } else {
            return Weathers.CLEAR;
        }
    }

    @Override
    public long getRemainingDuration() {
        Weather weather = getWeather();
        if (weather.equals(Weathers.CLEAR)) {
            if (this.worldInfo.getCleanWeatherTime() > 0) {
                return this.worldInfo.getCleanWeatherTime();
            } else {
                return Math.min(this.worldInfo.getThunderTime(), this.worldInfo.getRainTime());
            }
        } else if (weather.equals(Weathers.THUNDER_STORM)) {
            return this.worldInfo.getThunderTime();
        } else if (weather.equals(Weathers.RAIN)) {
            return this.worldInfo.getRainTime();
        }
        return 0;
    }

    @Override
    public void forecast(Weather weather) {
        this.forecast(weather, (300 + this.rand.nextInt(600)) * 20);
    }

    @Override
    public void forecast(Weather weather, long duration) {
        if (weather.equals(Weathers.CLEAR)) {
            this.worldInfo.setCleanWeatherTime((int) duration);
            this.worldInfo.setRainTime(0);
            this.worldInfo.setThunderTime(0);
            this.worldInfo.setRaining(false);
            this.worldInfo.setThundering(false);
        } else if (weather.equals(Weathers.RAIN)) {
            this.worldInfo.setCleanWeatherTime(0);
            this.worldInfo.setRainTime((int) duration);
            this.worldInfo.setThunderTime((int) duration);
            this.worldInfo.setRaining(true);
            this.worldInfo.setThundering(false);
        } else if (weather.equals(Weathers.THUNDER_STORM)) {
            this.worldInfo.setCleanWeatherTime(0);
            this.worldInfo.setRainTime((int) duration);
            this.worldInfo.setThunderTime((int) duration);
            this.worldInfo.setRaining(true);
            this.worldInfo.setThundering(true);
        }
    }

    @Override
    public Dimension getDimension() {
        return (Dimension) this.provider;
    }

    @Override
    public boolean doesKeepSpawnLoaded() {
        return this.keepSpawnLoaded;
    }

    @Override
    public void setKeepSpawnLoaded(boolean keepLoaded) {
        this.keepSpawnLoaded = keepLoaded;
    }

    @Override
    public SpongeConfig<SpongeConfig.WorldConfig> getWorldConfig() {
        return this.worldConfig;
    }

    @Override
    public void playSound(SoundType sound, Vector3d position, double volume) {
        this.playSound(sound, position, volume, 1);
    }

    @Override
    public void playSound(SoundType sound, Vector3d position, double volume, double pitch) {
        this.playSound(sound, position, volume, pitch, 0);
    }

    @Override
    public void playSound(SoundType sound, Vector3d position, double volume, double pitch, double minVolume) {
        this.playSoundEffect(position.getX(), position.getY(), position.getZ(), sound.getId(), (float) Math.max(minVolume, volume), (float) pitch);
    }

    @Override
    public Optional<Entity> getEntity(UUID uuid) {
        World spongeWorld = this;
        if (spongeWorld instanceof WorldServer) {
            return Optional.ofNullable((Entity) ((WorldServer) (Object) this).getEntityFromUuid(uuid));
        }
        for (net.minecraft.entity.Entity entity : this.loadedEntityList) {
            if (entity.getUniqueID().equals(uuid)) {
                return Optional.of((Entity) entity);
            }
        }
        return Optional.empty();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Iterable<Chunk> getLoadedChunks() {
        return ((ChunkProviderServer) this.getChunkProvider()).loadedChunks;
    }

    @Override
    public boolean unloadChunk(Chunk chunk) {
        return chunk != null && chunk.unloadChunk();
    }

    @Override
    public WorldCreationSettings getCreationSettings() {
        WorldProperties properties = this.getProperties();

        // Create based on WorldProperties
        WorldSettings settings = new WorldSettings(this.worldInfo);
        IMixinWorldSettings mixin = (IMixinWorldSettings) (Object) settings;
        mixin.setDimensionType(properties.getDimensionType());
        mixin.setGeneratorSettings(properties.getGeneratorSettings());
        mixin.setGeneratorModifiers(properties.getGeneratorModifiers());
        mixin.setEnabled(true);
        mixin.setKeepSpawnLoaded(this.keepSpawnLoaded);
        mixin.setLoadOnStartup(properties.loadOnStartup());

        return (WorldCreationSettings) (Object) settings;
    }

    @Override
    public void updateWorldGenerator() {
        // No need to wrap generator if no modifiers are present
        if (this.getProperties().getGeneratorModifiers().isEmpty()) {
            return;
        }

        IMixinWorldType worldType = (IMixinWorldType) this.getProperties().getGeneratorType();

        // Get the default generator for the world type
        DataContainer generatorSettings = this.getProperties().getGeneratorSettings();
        SpongeWorldGenerator newGenerator = worldType.createGenerator(this, generatorSettings);

        // Re-apply all world generator modifiers
        WorldCreationSettings creationSettings = this.getCreationSettings();

        for (WorldGeneratorModifier modifier : this.getProperties().getGeneratorModifiers()) {
            modifier.modifyWorldGenerator(creationSettings, generatorSettings, newGenerator);
        }

        // Set this world generator
        this.setWorldGenerator(newGenerator);
    }

    @Override
    public ImmutableList<Populator> getPopulators() {
        if (this.populators == null) {
            this.populators = ImmutableList.of();
        }
        return this.populators;
    }

    @Override
    public ImmutableList<GeneratorPopulator> getGeneratorPopulators() {
        if (this.generatorPopulators == null) {
            this.generatorPopulators = ImmutableList.of();
        }
        return this.generatorPopulators;
    }

    @Override
    public WorldProperties getProperties() {
        return (WorldProperties) this.worldInfo;
    }

    @Override
    public Location<World> getSpawnLocation() {
        return new Location<>(this, this.worldInfo.getSpawnX(), this.worldInfo.getSpawnY(), this.worldInfo.getSpawnZ());
    }

    @Override
    public Context getContext() {
        if (this.worldContext == null) {
            this.worldContext = new Context(Context.WORLD_KEY, getName());
        }
        return this.worldContext;
    }

    @Override
    public Optional<TileEntity> getTileEntity(int x, int y, int z) {
        net.minecraft.tileentity.TileEntity tileEntity = getTileEntity(new BlockPos(x, y, z));
        if (tileEntity == null) {
            return Optional.empty();
        } else {
            return Optional.of((TileEntity) tileEntity);
        }
    }

    @Override
    public Vector2i getBiomeMin() {
        return BIOME_MIN;
    }

    @Override
    public Vector2i getBiomeMax() {
        return BIOME_MAX;
    }

    @Override
    public Vector2i getBiomeSize() {
        return BIOME_SIZE;
    }

    @Override
    public Vector3i getBlockMin() {
        return BLOCK_MIN;
    }

    @Override
    public Vector3i getBlockMax() {
        return BLOCK_MAX;
    }

    @Override
    public Vector3i getBlockSize() {
        return BLOCK_SIZE;
    }

    @Override
    public boolean containsBiome(int x, int z) {
        return VecHelper.inBounds(x, z, BIOME_MIN, BIOME_MAX);
    }

    @Override
    public boolean containsBlock(int x, int y, int z) {
        return VecHelper.inBounds(x, y, z, BLOCK_MIN, BLOCK_MAX);
    }

    @Override
    public void setWorldGenerator(WorldGenerator generator) {
        // Replace populators with possibly modified list
        this.populators = ImmutableList.copyOf(generator.getPopulators());
        this.generatorPopulators = ImmutableList.copyOf(generator.getGeneratorPopulators());

        // Replace biome generator with possible modified one
        BiomeGenerator biomeGenerator = generator.getBiomeGenerator();
        WorldServer thisWorld = (WorldServer) (Object) this;
        thisWorld.provider.worldChunkMgr = CustomWorldChunkManager.of(biomeGenerator);

        // Replace generator populator with possibly modified one
        ((ChunkProviderServer) this.getChunkProvider()).serverChunkGenerator =
                CustomChunkProviderGenerate.of(thisWorld, biomeGenerator, generator.getBaseGeneratorPopulator(), this.generatorPopulators);
    }

    @Override
    public WorldGenerator getWorldGenerator() {
        // We have to create a new instance every time to satisfy the contract
        // of this method, namely that changing the state of the returned
        // instance does not affect the world without setWorldGenerator being
        // called
        ChunkProviderServer serverChunkProvider = (ChunkProviderServer) this.getChunkProvider();
        WorldServer world = (WorldServer) (Object) this;
        return new SpongeWorldGenerator(
                SpongeBiomeGenerator.of(getWorldChunkManager()),
                SpongeGeneratorPopulator.of(world, serverChunkProvider.serverChunkGenerator),
                getGeneratorPopulators(),
                getPopulators());
    }

    private void checkBiomeBounds(int x, int z) {
        if (!containsBiome(x, z)) {
            throw new PositionOutOfBoundsException(new Vector2i(x, z), BIOME_MIN, BIOME_MAX);
        }
    }

    private void checkBlockBounds(int x, int y, int z) {
        if (!containsBlock(x, y, z)) {
            throw new PositionOutOfBoundsException(new Vector3i(x, y, z), BLOCK_MIN, BLOCK_MAX);
        }
    }

    @Override
    public org.spongepowered.api.scoreboard.Scoreboard getScoreboard() {
        return this.spongeScoreboard;
    }

    @Override
    public void setScoreboard(org.spongepowered.api.scoreboard.Scoreboard scoreboard) {
        this.spongeScoreboard = checkNotNull(((SpongeScoreboard) scoreboard), "Scoreboard cannot be null!");
        this.worldScoreboard = ((SpongeScoreboard) scoreboard).createScoreboard(scoreboard);
    }

    @Override
    public Difficulty getDifficulty() {
        return (Difficulty) (Object) this.shadow$getDifficulty();
    }

    @SuppressWarnings("unchecked")
    private List<Player> getPlayers() {
        return ((net.minecraft.world.World) (Object) this).getPlayers(Player.class, Predicates.alwaysTrue());
    }

    @Override
    public void sendMessage(ChatType type, Text message) {
        for (Player player : getPlayers()) {
            player.sendMessages(type, message);
        }
    }

    @Override
    public void sendMessages(ChatType type, Text... messages) {
        for (Player player : getPlayers()) {
            player.sendMessages(type, messages);
        }
    }

    @Override
    public void sendMessages(ChatType type, Iterable<Text> messages) {
        for (Player player : getPlayers()) {
            player.sendMessages(type, messages);
        }
    }

    @Override
    public void sendTitle(Title title) {
        for (Player player : getPlayers()) {
            player.sendTitle(title);
        }
    }

    @Override
    public void resetTitle() {
        for (Player player : getPlayers()) {
            player.resetTitle();
        }
    }

    @Override
    public void clearTitle() {
        for (Player player : getPlayers()) {
            player.clearTitle();
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Collection<TileEntity> getTileEntities() {
        return Lists.newArrayList((List<TileEntity>) (Object) this.loadedTileEntityList);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Collection<TileEntity> getTileEntities(Predicate<TileEntity> filter) {
        return ((List<TileEntity>) (Object) this.loadedTileEntityList).stream()
                .filter(filter)
                .collect(Collectors.toList());
    }

    @Override
    public boolean isLoaded() {
        return DimensionManager.getWorldFromDimId(this.provider.getDimensionId()) != null;
    }

    @Override
    public Optional<String> getGameRule(String gameRule) {
        return this.getProperties().getGameRule(gameRule);
    }

    @Override
    public Map<String, String> getGameRules() {
        return this.getProperties().getGameRules();
    }

    @Override
    public void triggerExplosion(Explosion explosion) {
        checkNotNull(explosion, "explosion");
        checkNotNull(explosion.getOrigin(), "origin");

        newExplosion((net.minecraft.entity.Entity) explosion.getSourceExplosive().orElse(null), explosion
                .getOrigin().getX(), explosion.getOrigin().getY(), explosion.getOrigin().getZ(), explosion.getRadius(), explosion.canCauseFire(),
                explosion.shouldBreakBlocks());
    }

    @Override
    public Extent getExtentView(Vector3i newMin, Vector3i newMax) {
        checkBlockBounds(newMin.getX(), newMin.getY(), newMin.getZ());
        checkBlockBounds(newMax.getX(), newMax.getY(), newMax.getZ());
        return ExtentViewDownsize.newInstance(this, newMin, newMax);
    }

    @Override
    public Extent getExtentView(DiscreteTransform3 transform) {
        return ExtentViewTransform.newInstance(this, transform);
    }

    @Override
    public Extent getRelativeExtentView() {
        return getExtentView(DiscreteTransform3.fromTranslation(getBlockMin().negate()));
    }

    @Override
    public BlockSnapshot createSnapshot(int x, int y, int z) {
        World world = ((World) this);
        BlockState state = world.getBlock(x, y, z);
        Optional<TileEntity> te = world.getTileEntity(x, y, z);
        final SpongeBlockSnapshotBuilder builder = new SpongeBlockSnapshotBuilder()
                .blockState(state)
                .worldId(world.getUniqueId())
                .position(new Vector3i(x, y, z));
        if (te.isPresent()) {
            final TileEntity tileEntity = te.get();
            for (DataManipulator<?, ?> manipulator : tileEntity.getContainers()) {
                builder.add(manipulator);
            }
            final NBTTagCompound compound = new NBTTagCompound();
            ((net.minecraft.tileentity.TileEntity) tileEntity).writeToNBT(compound);
            builder.unsafeNbt(compound);
        }
        return builder.build();
    }

    @Override
    public boolean restoreSnapshot(BlockSnapshot snapshot, boolean force, boolean notifyNeighbors) {
        return snapshot.restore(force, notifyNeighbors);
    }

    @Override
    public boolean restoreSnapshot(int x, int y, int z, BlockSnapshot snapshot, boolean force, boolean notifyNeighbors) {
        snapshot = snapshot.withLocation(new Location<>(this, new Vector3i(x, y, z)));
        return snapshot.restore(force, notifyNeighbors);
    }

    @Override
    public <T extends Property<?, ?>> Optional<T> getProperty(int x, int y, int z, Class<T> propertyClass) {
        final Optional<PropertyStore<T>> optional = SpongePropertyRegistry.getInstance().getStore(propertyClass);
        if (optional.isPresent()) {
            return optional.get().getFor(new Location<>(this, x, y, z));
        }
        return Optional.empty();
    }

    @Override
    public <T extends Property<?, ?>> Optional<T> getProperty(int x, int y, int z, Direction direction, Class<T> propertyClass) {
        final Optional<PropertyStore<T>> optional = SpongePropertyRegistry.getInstance().getStore(propertyClass);
        if (optional.isPresent()) {
            return optional.get().getFor(new Location<>(this, x, y, z), direction);
        }
        return Optional.empty();
    }

    @Override
    public Collection<Property<?, ?>> getProperties(int x, int y, int z) {
        return SpongePropertyRegistry.getInstance().getPropertiesFor(new Location<World>(this, x, y, z));
    }

    @Override
    public Collection<Direction> getFacesWithProperty(int x, int y, int z, Class<? extends Property<?, ?>> propertyClass) {
        final Optional<? extends PropertyStore<? extends Property<?, ?>>> optional = SpongePropertyRegistry.getInstance().getStore(propertyClass);
        if (!optional.isPresent()) {
            return Collections.emptyList();
        }
        final PropertyStore<? extends Property<?, ?>> store = optional.get();
        final Location<World> loc = new Location<World>(this, x, y, z);
        ImmutableList.Builder<Direction> faces = ImmutableList.builder();
        for (EnumFacing facing : EnumFacing.values()) {
            Direction direction = DirectionFacingProvider.getInstance().getKey(facing).get();
            if (store.getFor(loc, direction).isPresent()) {
                faces.add(direction);
            }
        }
        return faces.build();
    }

    @Override
    public <E> Optional<E> get(int x, int y, int z, Key<? extends BaseValue<E>> key) {
        final Optional<E> optional = getBlock(x, y, z).get(key);
        if (optional.isPresent()) {
            return optional;
        } else {
            final Optional<TileEntity> tileEntityOptional = getTileEntity(x, y, z);
            if (tileEntityOptional.isPresent()) {
                return tileEntityOptional.get().get(key);
            }
        }
        return Optional.empty();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends DataManipulator<?, ?>> Optional<T> get(int x, int y, int z, Class<T> manipulatorClass) {
        final Collection<DataManipulator<?, ?>> manipulators = getManipulators(x, y, z);
        for (DataManipulator<?, ?> manipulator : manipulators) {
            if (manipulatorClass.isInstance(manipulator)) {
                return Optional.of((T) manipulator);
            }
        }
        return Optional.empty();
    }

    @Override
    public <T extends DataManipulator<?, ?>> Optional<T> getOrCreate(int x, int y, int z, Class<T> manipulatorClass) {
        final Optional<T> optional = get(x, y, z, manipulatorClass);
        if (optional.isPresent()) {
            return optional;
        } else {
            final Optional<TileEntity> tileEntity = getTileEntity(x, y, z);
            if (tileEntity.isPresent()) {
                return tileEntity.get().getOrCreate(manipulatorClass);
            }
        }
        return Optional.empty();
    }

    @Override
    public <E> E getOrNull(int x, int y, int z, Key<? extends BaseValue<E>> key) {
        return get(x, y, z, key).orElse(null);
    }

    @Override
    public <E> E getOrElse(int x, int y, int z, Key<? extends BaseValue<E>> key, E defaultValue) {
        return get(x, y, z, key).orElse(defaultValue);
    }

    @Override
    public <E, V extends BaseValue<E>> Optional<V> getValue(int x, int y, int z, Key<V> key) {
        final BlockState blockState = getBlock(x, y, z);
        if (blockState.supports(key)) {
            return blockState.getValue(key);
        } else {
            final Optional<TileEntity> tileEntity = getTileEntity(x, y, z);
            if (tileEntity.isPresent() && tileEntity.get().supports(key)) {
                return tileEntity.get().getValue(key);
            }
        }
        return Optional.empty();
    }

    @Override
    public boolean supports(int x, int y, int z, Key<?> key) {
        final BlockState blockState = getBlock(x, y, z);
        final boolean blockSupports = blockState.supports(key);
        final Optional<TileEntity> tileEntity = getTileEntity(x, y, z);
        final boolean tileEntitySupports = tileEntity.isPresent() && tileEntity.get().supports(key);
        return blockSupports || tileEntitySupports;
    }

    @Override
    public boolean supports(int x, int y, int z, BaseValue<?> value) {
        return supports(x, y, z, value.getKey());
    }

    @Override
    public boolean supports(int x, int y, int z, Class<? extends DataManipulator<?, ?>> manipulatorClass) {
        final BlockState blockState = getBlock(x, y, z);
        final List<ImmutableDataManipulator<?, ?>> immutableDataManipulators = blockState.getManipulators();
        boolean blockSupports = false;
        for (ImmutableDataManipulator<?, ?> manipulator : immutableDataManipulators) {
            if (manipulator.asMutable().getClass().isAssignableFrom(manipulatorClass)) {
                blockSupports = true;
                break;
            }
        }
        if (!blockSupports) {
            final Optional<TileEntity> tileEntity = getTileEntity(x, y, z);
            final boolean tileEntitySupports;
            if (tileEntity.isPresent()) {
                tileEntitySupports = tileEntity.get().supports(manipulatorClass);
            } else {
                tileEntitySupports = false;
            }
            return tileEntitySupports;
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean supports(int x, int y, int z, DataManipulator<?, ?> manipulator) {
        return supports(x, y, z, (Class<DataManipulator<?, ?>>) manipulator.getClass());
    }

    @Override
    public Set<Key<?>> getKeys(int x, int y, int z) {
        final ImmutableSet.Builder<Key<?>> builder = ImmutableSet.builder();
        final BlockState blockState = getBlock(x, y, z);
        builder.addAll(blockState.getKeys());
        final Optional<TileEntity> tileEntity = getTileEntity(x, y, z);
        if (tileEntity.isPresent()) {
            builder.addAll(tileEntity.get().getKeys());
        }
        return builder.build();
    }

    @Override
    public Set<ImmutableValue<?>> getValues(int x, int y, int z) {
        final ImmutableSet.Builder<ImmutableValue<?>> builder = ImmutableSet.builder();
        final BlockState blockState = getBlock(x, y, z);
        builder.addAll(blockState.getValues());
        final Optional<TileEntity> tileEntity = getTileEntity(x, y, z);
        if (tileEntity.isPresent()) {
            builder.addAll(tileEntity.get().getValues());
        }
        return builder.build();
    }

    @Override
    public <E> DataTransactionResult transform(int x, int y, int z, Key<? extends BaseValue<E>> key, Function<E, E> function) {
        if (supports(x, y, z, key)) {
            final Optional<E> optional = get(x, y, z, key);
            return offer(x, y, z, key, function.apply(optional.get()));
        }
        return DataTransactionResult.failNoData();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public <E> DataTransactionResult offer(int x, int y, int z, Key<? extends BaseValue<E>> key, E value) {
        final BlockState blockState = getBlock(x, y, z);
        if (blockState.supports(key)) {
            ImmutableValue<E> old = ((Value<E>) getValue(x, y, z, (Key) key).get()).asImmutable();
            setBlock(x, y, z, blockState.with(key, value).get());
            ImmutableValue<E> newVal = ((Value<E>) getValue(x, y, z, (Key) key).get()).asImmutable();
            return DataTransactionResult.successReplaceResult(newVal, old);
        }
        final Optional<TileEntity> tileEntity = getTileEntity(x, y, z);
        if (tileEntity.isPresent() && tileEntity.get().supports(key)) {
            return tileEntity.get().offer(key, value);
        }
        return DataTransactionResult.failNoData();
    }

    @Override
    public <E> DataTransactionResult offer(int x, int y, int z, BaseValue<E> value) {
        return offer(x, y, z, value.getKey(), value.get());
    }

    @Override
    public DataTransactionResult offer(int x, int y, int z, DataManipulator<?, ?> manipulator) {
        return offer(x, y, z, manipulator, MergeFunction.IGNORE_ALL);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public DataTransactionResult offer(int x, int y, int z, DataManipulator<?, ?> manipulator, MergeFunction function) {
        final BlockState blockState = getBlock(x, y, z);
        final ImmutableDataManipulator<?, ?> immutableDataManipulator = manipulator.asImmutable();
        if (blockState.supports((Class) immutableDataManipulator.getClass())) {
            final List<ImmutableValue<?>> old = new ArrayList<>(blockState.getValues());
            final BlockState newState = blockState.with(immutableDataManipulator).get();
            old.removeAll(newState.getValues());
            setBlock(x, y, z, newState);
            return DataTransactionResult.successReplaceResult(old, manipulator.getValues());
        } else {
            final Optional<TileEntity> tileEntityOptional = getTileEntity(x, y, z);
            if (tileEntityOptional.isPresent()) {
                return tileEntityOptional.get().offer(manipulator, function);
            }
        }
        return DataTransactionResult.failResult(manipulator.getValues());
    }

    @Override
    public DataTransactionResult offer(int x, int y, int z, Iterable<DataManipulator<?, ?>> manipulators) {
        final DataTransactionResult.Builder builder = DataTransactionResult.builder();
        for (DataManipulator<?, ?> manipulator : manipulators) {
            builder.absorbResult(offer(x, y, z, manipulator));
        }
        return builder.build();
    }

    @Override
    public DataTransactionResult remove(int x, int y, int z, Class<? extends DataManipulator<?, ?>> manipulatorClass) {
        final Optional<TileEntity> tileEntityOptional = getTileEntity(x, y, z);
        if (tileEntityOptional.isPresent()) {
            return tileEntityOptional.get().remove(manipulatorClass);
        }
        return DataTransactionResult.failNoData();
    }

    @Override
    public DataTransactionResult remove(int x, int y, int z, Key<?> key) {
        final Optional<TileEntity> tileEntityOptional = getTileEntity(x, y, z);
        if (tileEntityOptional.isPresent()) {
            return tileEntityOptional.get().remove(key);
        }
        return DataTransactionResult.failNoData();
    }

    @Override
    public DataTransactionResult undo(int x, int y, int z, DataTransactionResult result) {
        return DataTransactionResult.failNoData(); // todo
    }

    @Override
    public DataTransactionResult copyFrom(int xTo, int yTo, int zTo, DataHolder from) {
        return copyFrom(xTo, yTo, zTo, from, MergeFunction.IGNORE_ALL);
    }

    @Override
    public DataTransactionResult copyFrom(int xTo, int yTo, int zTo, int xFrom, int yFrom, int zFrom) {
        return copyFrom(xTo, yTo, zTo, new Location<World>(this, xFrom, yFrom, zFrom));
    }

    @Override
    public DataTransactionResult copyFrom(int xTo, int yTo, int zTo, DataHolder from, MergeFunction function) {
        final DataTransactionResult.Builder builder = DataTransactionResult.builder();
        final Collection<DataManipulator<?, ?>> manipulators = from.getContainers();
        for (DataManipulator<?, ?> manipulator : manipulators) {
            builder.absorbResult(offer(xTo, yTo, zTo, manipulator, function));
        }
        return builder.build();
    }

    @Override
    public DataTransactionResult copyFrom(int xTo, int yTo, int zTo, int xFrom, int yFrom, int zFrom, MergeFunction function) {
        return copyFrom(xTo, yTo, zTo, new Location<World>(this, xFrom, yFrom, zFrom), function);
    }

    @Override
    public Collection<DataManipulator<?, ?>> getManipulators(int x, int y, int z) {
        final List<DataManipulator<?, ?>> list = new ArrayList<>();
        final Collection<ImmutableDataManipulator<?, ?>> manipulators = this.getBlock(x, y, z).getManipulators();
        for (ImmutableDataManipulator<?, ?> immutableDataManipulator : manipulators) {
            list.add(immutableDataManipulator.asMutable());
        }
        final Optional<TileEntity> optional = getTileEntity(x, y, z);
        if (optional.isPresent()) {
            list.addAll(optional.get().getContainers());
        }
        return list;
    }

    @Override
    public boolean validateRawData(int x, int y, int z, DataView container) {
        return false; // todo
    }

    @Override
    public void setRawData(int x, int y, int z, DataView container) throws InvalidDataException {
        // todo
    }

    @Override
    public long getWeatherStartTime() {
        return this.weatherStartTime;
    }

    @Override
    public void setWeatherStartTime(long weatherStartTime) {
        this.weatherStartTime = weatherStartTime;
    }

    @Nullable
    @Override
    public EntityPlayer getClosestPlayerToEntityWhoAffectsSpawning(net.minecraft.entity.Entity entity, double distance) {
        return this.getClosestPlayerWhoAffectsSpawning(entity.posX, entity.posY, entity.posZ, distance);
    }

    @Nullable
    @Override
    public EntityPlayer getClosestPlayerWhoAffectsSpawning(double x, double y, double z, double distance) {
        double bestDistance = -1.0D;
        EntityPlayer result = null;

        for (Object entity : this.playerEntities) {
            EntityPlayer player = (EntityPlayer) entity;
            if (player == null || player.isDead || !((IMixinEntityPlayer) player).affectsSpawning()) {
                continue;
            }

            double playerDistance = player.getDistanceSq(x, y, z);

            if ((distance < 0.0D || playerDistance < distance * distance) && (bestDistance == -1.0D || playerDistance < bestDistance)) {
                bestDistance = playerDistance;
                result = player;
            }
        }

        return result;
    }

    @Override
    public boolean isAnyPlayerWithinRangeAtWhoAffectsSpawning(double x, double y, double z, double range) {
        for (Object entity : this.playerEntities) {
            EntityPlayer player = (EntityPlayer) entity;
            if (player == null || player.isDead || !((IMixinEntityPlayer) player).affectsSpawning()) {
                continue;
            }

            double distance = player.getDistanceSq(x, y, z);

            if (range < 0.0D || distance < range * range) {
                return true;
            }
        }

        return false;
    }
}
