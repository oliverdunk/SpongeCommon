package org.spongepowered.common.world;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.state.IBlockState;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.entity.EntityHanging;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.effect.EntityWeatherEffect;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.entity.item.EntityTNTPrimed;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.projectile.EntityFishHook;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.Slot;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C01PacketChatMessage;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C0EPacketClickWindow;
import net.minecraft.network.play.client.C10PacketCreativeInventoryAction;
import net.minecraft.network.play.server.S23PacketBlockChange;
import net.minecraft.network.play.server.S2FPacketSetSlot;
import net.minecraft.stats.StatList;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ReportedException;
import net.minecraft.world.IBlockAccess;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.block.tileentity.TileEntity;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntitySnapshot;
import org.spongepowered.api.entity.Item;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.Cancellable;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.NamedCause;
import org.spongepowered.api.event.entity.DestructEntityEvent;
import org.spongepowered.api.event.entity.SpawnEntityEvent;
import org.spongepowered.api.event.item.inventory.DropItemEvent;
import org.spongepowered.api.event.world.chunk.PopulateChunkEvent;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.text.sink.MessageSink;
import org.spongepowered.api.world.Chunk;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.gen.PopulatorType;
import org.spongepowered.common.SpongeImpl;
import org.spongepowered.common.SpongeImplFactory;
import org.spongepowered.common.block.SpongeBlockSnapshot;
import org.spongepowered.common.block.SpongeBlockSnapshotBuilder;
import org.spongepowered.common.data.util.NbtDataUtil;
import org.spongepowered.common.entity.PlayerTracker;
import org.spongepowered.common.event.SpongeCommonEventFactory;
import org.spongepowered.common.interfaces.IMixinChunk;
import org.spongepowered.common.interfaces.IMixinWorld;
import org.spongepowered.common.interfaces.entity.IMixinEntity;
import org.spongepowered.common.util.SpongeHooks;
import org.spongepowered.common.util.StaticMixinHelper;
import org.spongepowered.common.util.VecHelper;
import org.spongepowered.common.world.gen.SpongePopulatorType;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class CauseTracker {

    public boolean processingCaptureCause = false;
    public boolean captureEntitySpawns = true;
    public boolean captureBlockDecay = false;
    public boolean captureTerrainGen = false;
    public boolean captureBlocks = false;
    public boolean restoringBlocks = false;
    private boolean worldSpawnerRunning;
    private boolean chunkSpawnerRunning;
    public SpongeBlockSnapshotBuilder builder = new SpongeBlockSnapshotBuilder();
    public List<Entity> capturedEntities = new ArrayList<>();
    public List<Entity> capturedEntityItems = new ArrayList<>();
    public List<Entity> capturedOnBlockAddedEntities = new ArrayList<>();
    public List<Entity> capturedOnBlockAddedItems = new ArrayList<>();
    public BlockSnapshot currentTickBlock = null;
    public BlockSnapshot currentTickOnBlockAdded = null;
    public Entity currentTickEntity = null;
    public TileEntity currentTickTileEntity = null;
    public List<BlockSnapshot> capturedSpongeBlockBreaks = new ArrayList<>();
    public List<BlockSnapshot> capturedSpongeBlockDecays = new ArrayList<>();
    public List<BlockSnapshot> capturedSpongeBlockPlaces = new ArrayList<>();
    public List<BlockSnapshot> capturedSpongeBlockModifications = new ArrayList<>();
    public List<BlockSnapshot> capturedSpongeBlockFluids = new ArrayList<>();
    public Map<PopulatorType, List<Transaction<BlockSnapshot>>> capturedSpongePopulators = Maps.newHashMap();
    public Map<CaptureType, List<BlockSnapshot>> captureBlockLists = Maps.newHashMap();

    private final World world;
    private final IMixinWorld mWorld;
    private final net.minecraft.world.World mcWorld;

    public CauseTracker(World world) {
        this.world = (World) (this.mWorld = (IMixinWorld) (this.mcWorld = (net.minecraft.world.World) world));
        this.captureBlocks = true;
        this.captureEntitySpawns = true;
        this.captureBlockLists.put(CaptureType.BREAK, this.capturedSpongeBlockBreaks);
        this.captureBlockLists.put(CaptureType.DECAY, this.capturedSpongeBlockDecays);
        this.captureBlockLists.put(CaptureType.FLUID, this.capturedSpongeBlockFluids);
        this.captureBlockLists.put(CaptureType.MODIFY, this.capturedSpongeBlockModifications);
        this.captureBlockLists.put(CaptureType.PLACE, this.capturedSpongeBlockPlaces);
    }

    /**
     * @author bloodmc
     *
     *         Purpose: Rewritten to support capturing blocks
     */
    public boolean setBlockState(BlockPos pos, IBlockState newState, int flags) {
        net.minecraft.world.chunk.Chunk chunk = this.mcWorld.getChunkFromBlockCoords(pos);
        IBlockState currentState = chunk.getBlockState(pos);
        if (currentState == newState) {
            return false;
        }

        Block block = newState.getBlock();
        BlockSnapshot originalBlockSnapshot = null;
        BlockSnapshot newBlockSnapshot = null;
        Transaction<BlockSnapshot> transaction = null;

        // Don't capture if we are restoring blocks
        if (!this.mcWorld.isRemote && !this.restoringBlocks) {
            originalBlockSnapshot = createSpongeBlockSnapshot(currentState,
                    currentState.getBlock().getActualState(currentState, (IBlockAccess) this, pos), pos, flags);

            if (StaticMixinHelper.runningGenerator != null
                    && net.minecraft.world.gen.feature.WorldGenerator.class.isAssignableFrom(StaticMixinHelper.runningGenerator)) {
                SpongePopulatorType populatorType = null;
                populatorType = StaticMixinHelper.populator;

                if (populatorType == null) {
                    populatorType =
                            (SpongePopulatorType) SpongeImpl.getRegistry().getTranslated(StaticMixinHelper.runningGenerator, PopulatorType.class);
                }

                if (populatorType != null) {
                    if (this.capturedSpongePopulators.get(populatorType) == null) {
                        this.capturedSpongePopulators.put(populatorType, new ArrayList<>());
                    }

                    transaction = new Transaction<>(originalBlockSnapshot, originalBlockSnapshot.withState((BlockState) newState));
                    this.capturedSpongePopulators.get(populatorType).add(transaction);
                }
            } else if (block.getMaterial().isLiquid() || currentState.getBlock().getMaterial().isLiquid()) {
                this.capturedSpongeBlockFluids.add(originalBlockSnapshot);
            } else if (this.captureBlockDecay) {
                this.capturedSpongeBlockDecays.add(originalBlockSnapshot);
            } else if (block == Blocks.air) {
                this.capturedSpongeBlockBreaks.add(originalBlockSnapshot);
            } else if (block != currentState.getBlock()) {
                this.capturedSpongeBlockPlaces.add(originalBlockSnapshot);
            } else {
                this.capturedSpongeBlockModifications.add(originalBlockSnapshot);
            }
        }

        int oldLight = currentState.getBlock().getLightValue();

        IBlockState iblockstate1 = ((IMixinChunk) chunk).setBlockState(pos, newState, currentState, newBlockSnapshot);

        if (iblockstate1 == null) {
            if (originalBlockSnapshot != null) {
                this.capturedSpongeBlockBreaks.remove(originalBlockSnapshot);
                this.capturedSpongeBlockDecays.remove(originalBlockSnapshot);
                this.capturedSpongeBlockFluids.remove(originalBlockSnapshot);
                this.capturedSpongeBlockPlaces.remove(originalBlockSnapshot);
                this.capturedSpongeBlockModifications.remove(originalBlockSnapshot);
            }
            return false;
        } else {
            Block block1 = iblockstate1.getBlock();

            if (block.getLightOpacity() != block1.getLightOpacity() || block.getLightValue() != oldLight) {
                this.mcWorld.theProfiler.startSection("checkLight");
                this.mcWorld.checkLight(pos);
                this.mcWorld.theProfiler.endSection();
            }

            // Don't notify clients or update physics while capturing
            // blockstates
            if (originalBlockSnapshot == null) {
                // Modularize client and physic updates
                markAndNotifyNeighbors(pos, chunk, iblockstate1, newState, flags);
            }

            return true;
        }
    }

    public void markAndNotifyNeighbors(BlockPos pos, net.minecraft.world.chunk.Chunk chunk, IBlockState old, IBlockState new_, int flags) {
        if ((flags & 2) != 0 && (!this.mcWorld.isRemote || (flags & 4) == 0) && (chunk == null || chunk.isPopulated())) {
            this.mcWorld.markBlockForUpdate(pos);
        }

        if (!this.mcWorld.isRemote && (flags & 1) != 0) {
            this.mcWorld.notifyNeighborsRespectDebug(pos, old.getBlock());

            if (new_.getBlock().hasComparatorInputOverride()) {
                this.mcWorld.updateComparatorOutputLevel(pos, new_.getBlock());
            }
        }
    }

    @SuppressWarnings("unchecked")
    public boolean spawnEntity(Entity entity, Cause cause) {
        checkNotNull(entity, "Entity cannot be null!");
        checkNotNull(cause, "Cause cannot be null!");

        net.minecraft.entity.Entity entityIn = (net.minecraft.entity.Entity) entity;
        // do not drop any items while restoring blocksnapshots. Prevents dupes
        if (!this.mcWorld.isRemote && (entityIn == null || (entityIn instanceof net.minecraft.entity.item.EntityItem && this.restoringBlocks))) {
            return false;
        }

        int i = MathHelper.floor_double(entityIn.posX / 16.0D);
        int j = MathHelper.floor_double(entityIn.posZ / 16.0D);
        boolean flag = entityIn.forceSpawn;

        if (entityIn instanceof EntityPlayer) {
            flag = true;
        }

        if (!flag && !this.mcWorld.isChunkLoaded(i, j, true)) {
            return false;
        } else {
            if (entityIn instanceof EntityPlayer) {
                EntityPlayer entityplayer = (EntityPlayer) entityIn;
                this.mcWorld.playerEntities.add(entityplayer);
                this.mcWorld.updateAllPlayersSleepingFlag();
            }

            if (this.mcWorld.isRemote || flag) {
                this.mcWorld.getChunkFromChunkCoords(i, j).addEntity(entityIn);
                this.mcWorld.loadedEntityList.add(entityIn);
                this.mcWorld.onEntityAdded(entityIn);
                return true;
            }

            if (!flag && this.processingCaptureCause) {
                BlockSnapshot tickBlock = null;
                if (this.currentTickBlock != null) {
                    tickBlock = this.currentTickBlock;
                } else if (this.currentTickOnBlockAdded != null) {
                    tickBlock = this.currentTickOnBlockAdded;
                }
                if (tickBlock != null) {
                    BlockPos sourcePos = VecHelper.toBlockPos(tickBlock.getPosition());
                    Block targetBlock = this.mcWorld.getBlockState(entityIn.getPosition()).getBlock();
                    SpongeHooks.tryToTrackBlockAndEntity(this.mcWorld, tickBlock, entityIn, sourcePos, targetBlock, entityIn.getPosition(),
                            PlayerTracker.Type.NOTIFIER);
                }
                if (this.currentTickEntity != null) {
                    Optional<User> creator = ((IMixinEntity) this.currentTickEntity).getTrackedPlayer(NbtDataUtil.SPONGE_ENTITY_CREATOR);
                    if (creator.isPresent()) { // transfer user to next entity.
                                               // This occurs with falling
                                               // blocks that change into items
                        ((IMixinEntity) entityIn).trackEntityUniqueId(NbtDataUtil.SPONGE_ENTITY_CREATOR, creator.get().getUniqueId());
                    }
                }
                if (entityIn instanceof EntityItem) {
                    if (this.currentTickOnBlockAdded != null) {
                        this.capturedOnBlockAddedItems.add((Item) entityIn);
                    } else {
                        this.capturedEntityItems.add((Item) entityIn);
                    }
                } else {
                    if (this.currentTickOnBlockAdded != null) {
                        this.capturedOnBlockAddedEntities.add((Entity) entityIn);
                    } else {
                        this.capturedEntities.add((Entity) entityIn);
                    }
                }
                return true;
            } else { // Custom

                if (entityIn instanceof EntityFishHook && ((EntityFishHook) entityIn).angler == null) {
                    // TODO MixinEntityFishHook.setShooter makes angler null
                    // sometimes, but that will cause NPE when ticking
                    return false;
                }

                EntityLivingBase specialCause = null;
                String causeName = "";
                // Special case for throwables
                if (!(entityIn instanceof EntityPlayer) && entityIn instanceof EntityThrowable) {
                    EntityThrowable throwable = (EntityThrowable) entityIn;
                    specialCause = throwable.getThrower();

                    if (specialCause != null) {
                        causeName = NamedCause.THROWER;
                        if (specialCause instanceof Player) {
                            Player player = (Player) specialCause;
                            ((IMixinEntity) entityIn).trackEntityUniqueId(NbtDataUtil.SPONGE_ENTITY_CREATOR, player.getUniqueId());
                        }
                    }
                }
                // Special case for TNT
                else if (!(entityIn instanceof EntityPlayer) && entityIn instanceof EntityTNTPrimed) {
                    EntityTNTPrimed tntEntity = (EntityTNTPrimed) entityIn;
                    specialCause = tntEntity.getTntPlacedBy();
                    causeName = NamedCause.IGNITER;

                    if (specialCause instanceof Player) {
                        Player player = (Player) specialCause;
                        ((IMixinEntity) entityIn).trackEntityUniqueId(NbtDataUtil.SPONGE_ENTITY_CREATOR, player.getUniqueId());
                    }
                }
                // Special case for Tameables
                else if (!(entityIn instanceof EntityPlayer) && entityIn instanceof EntityTameable) {
                    EntityTameable tameable = (EntityTameable) entityIn;
                    if (tameable.getOwnerEntity() != null) {
                        specialCause = tameable.getOwnerEntity();
                        causeName = NamedCause.OWNER;
                    }
                }

                if (specialCause != null) {
                    if (!cause.all().contains(specialCause)) {
                        cause = cause.with(NamedCause.of(causeName, specialCause));
                    }
                }

                org.spongepowered.api.event.Event event = null;
                ImmutableList.Builder<EntitySnapshot> entitySnapshotBuilder = new ImmutableList.Builder<>();
                entitySnapshotBuilder.add(((Entity) entityIn).createSnapshot());

                if (entityIn instanceof EntityItem) {
                    this.capturedEntityItems.add((Item) entityIn);
                    event =
                            SpongeEventFactory.createDropItemEventCustom(SpongeImpl.getGame(), cause,
                                    (List<Entity>) (List<?>) this.capturedEntityItems,
                                    entitySnapshotBuilder.build(), (World) (Object) this);
                } else {
                    event =
                            SpongeEventFactory.createSpawnEntityEventCustom(SpongeImpl.getGame(), cause, this.capturedEntities,
                                    entitySnapshotBuilder.build(), (World) (Object) this);
                }
                SpongeImpl.postEvent(event);

                if (!((Cancellable) event).isCancelled()) {
                    if (entityIn instanceof EntityWeatherEffect) {
                        return this.mcWorld.addWeatherEffect(entityIn);
                    }

                    this.mcWorld.getChunkFromChunkCoords(i, j).addEntity(entityIn);
                    this.mcWorld.loadedEntityList.add(entityIn);
                    this.mcWorld.onEntityAdded(entityIn);
                    if (entityIn instanceof EntityItem) {
                        this.capturedEntityItems.remove(entityIn);
                    } else {
                        this.capturedEntities.remove(entityIn);
                    }
                    return true;
                }

                return false;
            }
        }
    }

    private void markAndNotifyBlockPost(List<Transaction<BlockSnapshot>> transactions, CaptureType type, Cause cause) {
        for (Transaction<BlockSnapshot> transaction : transactions) {
            // Handle custom replacements
            if (transaction.isValid() && transaction.getCustom().isPresent()) {
                this.restoringBlocks = true;
                transaction.getFinal().restore(true, false);
                this.restoringBlocks = false;
            }

            SpongeBlockSnapshot oldBlockSnapshot = (SpongeBlockSnapshot) transaction.getOriginal();
            SpongeBlockSnapshot newBlockSnapshot = (SpongeBlockSnapshot) transaction.getFinal();
            SpongeHooks.logBlockAction(cause, (net.minecraft.world.World) (Object) this, type, transaction);
            int updateFlag = oldBlockSnapshot.getUpdateFlag();
            BlockPos pos = VecHelper.toBlockPos(oldBlockSnapshot.getPosition());
            IBlockState originalState = (IBlockState) oldBlockSnapshot.getState();
            IBlockState newState = (IBlockState) newBlockSnapshot.getState();
            // Containers get placed automatically
            if (newState != null && !SpongeImplFactory.blockHasTileEntity(newState.getBlock(), newState)) {
                this.currentTickOnBlockAdded = this.createSpongeBlockSnapshot(newState,
                        newState.getBlock().getActualState(newState, (IBlockAccess) this, pos), pos, updateFlag);
                newState.getBlock().onBlockAdded((net.minecraft.world.World) (Object) this, pos, newState);
                if (this.capturedOnBlockAddedItems.size() > 0) {
                    Cause blockCause = Cause.of(NamedCause.source(this.currentTickOnBlockAdded));
                    if (this.captureTerrainGen) {
                        net.minecraft.world.chunk.Chunk chunk = this.mcWorld.getChunkFromBlockCoords(pos);
                        if (chunk != null && ((IMixinChunk) chunk).getCurrentPopulateCause() != null) {
                            blockCause = blockCause.with(((IMixinChunk) chunk).getCurrentPopulateCause().all());
                        }
                    }
                    handleDroppedItems(blockCause, this.capturedOnBlockAddedItems, null, this.mcWorld.getBlockState(pos) != newState);
                }
                if (this.capturedOnBlockAddedEntities.size() > 0) {
                    Cause blockCause = Cause.of(this.currentTickOnBlockAdded);
                    if (this.captureTerrainGen) {
                        net.minecraft.world.chunk.Chunk chunk = this.mcWorld.getChunkFromBlockCoords(pos);
                        if (chunk != null && ((IMixinChunk) chunk).getCurrentPopulateCause() != null) {
                            blockCause = blockCause.with(((IMixinChunk) chunk).getCurrentPopulateCause().all());
                        }
                    }
                    handleEntitySpawns(blockCause, this.capturedOnBlockAddedEntities, null);
                }

                this.currentTickOnBlockAdded = null;
            }

            markAndNotifyNeighbors(pos, null, originalState, newState, updateFlag);
        }
    }

    public void notifyBlockOfStateChange(BlockPos notifyPos, final Block sourceBlock, BlockPos sourcePos) {
        if (!this.mcWorld.isRemote) {
            IBlockState iblockstate = this.mcWorld.getBlockState(notifyPos);

            try {
                if (!this.mcWorld.isRemote) {
                    if (StaticMixinHelper.packetPlayer != null) {
                        IMixinChunk spongeChunk = (IMixinChunk) this.mcWorld.getChunkFromBlockCoords(notifyPos);
                        if (spongeChunk != null) {
                            spongeChunk.addTrackedBlockPosition(iblockstate.getBlock(), notifyPos, (User) StaticMixinHelper.packetPlayer,
                                    PlayerTracker.Type.NOTIFIER);
                        }
                    } else {
                        Object source = null;
                        if (this.currentTickBlock != null) {
                            source = this.currentTickBlock;
                            sourcePos = VecHelper.toBlockPos(this.currentTickBlock.getPosition());
                        } else if (this.currentTickOnBlockAdded != null) {
                            source = this.currentTickOnBlockAdded;
                            sourcePos = VecHelper.toBlockPos(this.currentTickOnBlockAdded.getPosition());
                        } else if (this.currentTickTileEntity != null) {
                            source = this.currentTickTileEntity;
                            sourcePos = ((net.minecraft.tileentity.TileEntity) this.currentTickTileEntity).getPos();
                        } else if (this.currentTickEntity != null) { // Falling
                                                                     // Blocks
                            IMixinEntity spongeEntity = (IMixinEntity) this.currentTickEntity;
                            sourcePos = ((net.minecraft.entity.Entity) this.currentTickEntity).getPosition();
                            Optional<User> owner = spongeEntity.getTrackedPlayer(NbtDataUtil.SPONGE_ENTITY_CREATOR);
                            Optional<User> notifier = spongeEntity.getTrackedPlayer(NbtDataUtil.SPONGE_ENTITY_NOTIFIER);
                            if (notifier.isPresent()) {
                                IMixinChunk spongeChunk = (IMixinChunk) this.mcWorld.getChunkFromBlockCoords(notifyPos);
                                spongeChunk.addTrackedBlockPosition(iblockstate.getBlock(), notifyPos, notifier.get(), PlayerTracker.Type.NOTIFIER);
                            } else if (owner.isPresent()) {
                                IMixinChunk spongeChunk = (IMixinChunk) this.mcWorld.getChunkFromBlockCoords(notifyPos);
                                spongeChunk.addTrackedBlockPosition(iblockstate.getBlock(), notifyPos, owner.get(), PlayerTracker.Type.NOTIFIER);
                            }
                        }

                        if (source != null) {
                            SpongeHooks.tryToTrackBlock(this.mcWorld, source, sourcePos, iblockstate.getBlock(), notifyPos,
                                    PlayerTracker.Type.NOTIFIER);
                        }
                    }
                }

                iblockstate.getBlock().onNeighborBlockChange(this.mcWorld, notifyPos, iblockstate, sourceBlock);
            } catch (Throwable throwable) {
                CrashReport crashreport = CrashReport.makeCrashReport(throwable, "Exception while updating neighbours");
                CrashReportCategory crashreportcategory = crashreport.makeCategory("Block being updated");
                // TODO
                /*
                 * crashreportcategory.addCrashSectionCallable(
                 * "Source block type", new Callable() { public String call() {
                 * try { return String.format("ID #%d (%s // %s)", new Object[]
                 * {Integer.valueOf(Block.getIdFromBlock(blockIn)),
                 * blockIn.getUnlocalizedName(),
                 * blockIn.getClass().getCanonicalName()}); } catch (Throwable
                 * throwable1) { return "ID #" + Block.getIdFromBlock(blockIn);
                 * } } });
                 */
                CrashReportCategory.addBlockInfo(crashreportcategory, notifyPos, iblockstate);
                throw new ReportedException(crashreport);
            }
        }
    }

    public boolean processingCaptureCause() {
        return this.processingCaptureCause;
    }

    public void setProcessingCaptureCause(boolean flag) {
        this.processingCaptureCause = flag;
    }

    public void setCurrentTickBlock(BlockSnapshot snapshot) {
        this.currentTickBlock = snapshot;
    }

    public Optional<BlockSnapshot> getCurrentTickBlock() {
        return Optional.ofNullable(this.currentTickBlock);
    }

    public Optional<Entity> getCurrentTickEntity() {
        return Optional.ofNullable(this.currentTickEntity);
    }

    public Optional<TileEntity> getCurrentTickTileEntity() {
        return Optional.ofNullable(this.currentTickTileEntity);
    }

    public List<Entity> getCapturedEntities() {
        return this.capturedEntities;
    }

    public List<Entity> getCapturedEntityItems() {
        return this.capturedEntityItems;
    }

    public boolean capturingBlocks() {
        return this.captureBlocks;
    }

    public boolean capturingTerrainGen() {
        return this.captureTerrainGen;
    }

    public void setCapturingTerrainGen(boolean flag) {
        this.captureTerrainGen = flag;
    }

    public void setCapturingEntitySpawns(boolean flag) {
        this.captureEntitySpawns = flag;
    }

    public void setCapturingBlockDecay(boolean flag) {
        this.captureBlockDecay = flag;
    }

    public List<BlockSnapshot> getBlockBreakList() {
        return this.capturedSpongeBlockBreaks;
    }

    @SuppressWarnings("unchecked")
    public void handlePostTickCaptures(Cause cause) {
        if (this.mcWorld.isRemote || this.restoringBlocks || cause == null || cause.isEmpty()) {
            return;
        } else if (this.capturedEntities.size() == 0 && this.capturedEntityItems.size() == 0 && this.capturedSpongeBlockBreaks.size() == 0
                && this.capturedSpongeBlockModifications.size() == 0 && this.capturedSpongeBlockPlaces.size() == 0
                && this.capturedSpongeBlockFluids.size() == 0 && this.capturedSpongePopulators.size() == 0
                && StaticMixinHelper.packetPlayer == null) {
            return; // nothing was captured, return
        }

        net.minecraft.world.World world = (net.minecraft.world.World) (Object) this;
        EntityPlayerMP player = StaticMixinHelper.packetPlayer;
        Packet packetIn = StaticMixinHelper.processingPacket;
        List<Transaction<BlockSnapshot>> invalidTransactions = new ArrayList<>();
        boolean destructDrop = false;

        // Attempt to find a Player cause if we do not have one
        if (!cause.first(User.class).isPresent()) {
            if ((cause.first(BlockSnapshot.class).isPresent() || cause.first(TileEntity.class).isPresent())) {
                // Check for player at pos of first transaction
                Optional<BlockSnapshot> snapshot = cause.first(BlockSnapshot.class);
                Optional<TileEntity> te = cause.first(TileEntity.class);
                BlockPos pos = null;
                if (snapshot.isPresent()) {
                    pos = VecHelper.toBlockPos(snapshot.get().getPosition());
                } else {
                    pos = ((net.minecraft.tileentity.TileEntity) te.get()).getPos();
                }
                net.minecraft.world.chunk.Chunk chunk = this.mcWorld.getChunkFromBlockCoords(pos);
                if (chunk != null) {
                    IMixinChunk spongeChunk = (IMixinChunk) chunk;

                    Optional<User> owner = spongeChunk.getBlockOwner(pos);
                    Optional<User> notifier = spongeChunk.getBlockNotifier(pos);
                    if (owner.isPresent()) {
                        cause = cause.with(NamedCause.owner(owner.get()));
                    }
                    if (notifier.isPresent()) {
                        if (!cause.all().contains(notifier.get())) {
                            Cause newCause = Cause.of(NamedCause.notifier(notifier.get()));
                            cause = newCause.with(cause.all());
                        }
                    }
                }
            } else if (cause.first(Entity.class).isPresent()) {
                Entity entity = cause.first(Entity.class).get();
                if (entity instanceof EntityTameable) {
                    EntityTameable tameable = (EntityTameable) entity;
                    if (tameable.getOwnerEntity() != null) {
                        cause = cause.with(NamedCause.owner(tameable.getOwnerEntity()));
                    }
                } else {
                    Optional<User> owner = ((IMixinEntity) entity).getTrackedPlayer(NbtDataUtil.SPONGE_ENTITY_CREATOR);
                    if (owner.isPresent()) {
                        cause = cause.with(NamedCause.owner(owner.get()));
                    }
                }
            }
        }

        // Handle Block captures
        for (Map.Entry<CaptureType, List<BlockSnapshot>> mapEntry : this.captureBlockLists.entrySet()) {
            CaptureType captureType = mapEntry.getKey();
            List<BlockSnapshot> capturedBlockList = mapEntry.getValue();

            if (capturedBlockList.size() > 0) {
                ImmutableList<Transaction<BlockSnapshot>> blockTransactions;
                ImmutableList.Builder<Transaction<BlockSnapshot>> builder = new ImmutableList.Builder<>();

                Iterator<BlockSnapshot> iterator = capturedBlockList.iterator();
                while (iterator.hasNext()) {
                    BlockSnapshot blockSnapshot = iterator.next();
                    BlockPos pos = VecHelper.toBlockPos(blockSnapshot.getPosition());
                    IBlockState currentState = this.mcWorld.getBlockState(pos);
                    builder.add(new Transaction<>(blockSnapshot, createSpongeBlockSnapshot(currentState, currentState.getBlock()
                            .getActualState(currentState, (IBlockAccess) this, pos), pos, 0)));
                    iterator.remove();
                }
                blockTransactions = builder.build();

                if (blockTransactions.size() > 0) {
                    ChangeBlockEvent event = null;

                    if (captureType == CaptureType.BREAK) {
                        if (player != null && packetIn instanceof C07PacketPlayerDigging) {
                            C07PacketPlayerDigging digPacket = (C07PacketPlayerDigging) packetIn;
                            if (digPacket.getStatus() == C07PacketPlayerDigging.Action.START_DESTROY_BLOCK) {
                                destructDrop = true;
                                // make the first destroyed block root cause
                                Cause newCause = Cause.of(blockTransactions.get(0).getOriginal());
                                newCause = newCause.with(cause.all());
                                cause = newCause;
                            }
                        }

                        event = SpongeEventFactory.createChangeBlockEventBreak(SpongeImpl.getGame(), cause, (World) world, blockTransactions);
                    } else if (captureType == CaptureType.DECAY) {
                        event = SpongeEventFactory.createChangeBlockEventDecay(SpongeImpl.getGame(), cause, (World) world, blockTransactions);
                    } else if (captureType == CaptureType.FLUID) {
                        event = SpongeEventFactory.createChangeBlockEventFluid(SpongeImpl.getGame(), cause, (World) world, blockTransactions);
                    } else if (captureType == CaptureType.MODIFY) {
                        event = SpongeEventFactory.createChangeBlockEventModify(SpongeImpl.getGame(), cause, (World) world, blockTransactions);
                    } else if (captureType == CaptureType.PLACE) {
                        event = SpongeEventFactory.createChangeBlockEventPlace(SpongeImpl.getGame(), cause, (World) world, blockTransactions);
                    }

                    SpongeImpl.postEvent(event);

                    C08PacketPlayerBlockPlacement packet = null;

                    if (packetIn instanceof C08PacketPlayerBlockPlacement) {
                        packet = (C08PacketPlayerBlockPlacement) packetIn;
                    }

                    if (event.isCancelled()) {
                        // Restore original blocks
                        for (Transaction<BlockSnapshot> transaction : event.getTransactions()) {
                            this.restoringBlocks = true;
                            transaction.getOriginal().restore(true, false);
                            this.restoringBlocks = false;
                        }

                        handlePostPlayerBlockEvent(captureType, player, world, event.getTransactions());

                        // clear entity list and return to avoid spawning items
                        this.capturedEntities.clear();
                        this.capturedEntityItems.clear();
                        return;
                    } else {
                        for (Transaction<BlockSnapshot> transaction : event.getTransactions()) {
                            if (!transaction.isValid()) {
                                this.restoringBlocks = true;
                                transaction.getOriginal().restore(true, false);
                                this.restoringBlocks = false;
                                invalidTransactions.add(transaction);
                            } else {
                                if (captureType == CaptureType.BREAK && cause.first(User.class).isPresent()) {
                                    BlockPos pos = VecHelper.toBlockPos(transaction.getOriginal().getPosition());
                                    for (EntityHanging hanging : SpongeHooks.findHangingEntities(world, pos)) {
                                        if (hanging != null) {
                                            if (hanging instanceof EntityItemFrame) {
                                                EntityItemFrame itemFrame = (EntityItemFrame) hanging;
                                                net.minecraft.entity.Entity dropCause = null;
                                                if (cause.root().get() instanceof net.minecraft.entity.Entity) {
                                                    dropCause = (net.minecraft.entity.Entity) cause.root().get();
                                                }

                                                itemFrame.dropItemOrSelf(dropCause, true);
                                                itemFrame.setDead();
                                            }
                                        }
                                    }
                                }

                                if (captureType == CaptureType.PLACE && player != null
                                        && transaction.getOriginal().getState().getType() == BlockTypes.AIR) {
                                    BlockPos pos = VecHelper.toBlockPos(transaction.getFinal().getPosition());
                                    IMixinChunk spongeChunk = (IMixinChunk) this.mcWorld.getChunkFromBlockCoords(pos);
                                    spongeChunk.addTrackedBlockPosition((net.minecraft.block.Block) transaction.getFinal().getState().getType(), pos,
                                            (User) player, PlayerTracker.Type.OWNER);
                                }
                            }
                        }

                        if (invalidTransactions.size() > 0) {
                            handlePostPlayerBlockEvent(captureType, player, world, invalidTransactions);
                        }

                        if (this.capturedEntityItems.size() > 0) {
                            handleDroppedItems(cause, (List<Entity>) (List<?>) this.capturedEntityItems, invalidTransactions,
                                    captureType == CaptureType.BREAK ? true : destructDrop);
                        }

                        markAndNotifyBlockPost(event.getTransactions(), captureType, cause);

                        if (captureType == CaptureType.PLACE && player != null && packet != null && packet.getStack() != null) {
                            player.addStat(StatList.objectUseStats[net.minecraft.item.Item.getIdFromItem(packet.getStack().getItem())], 1);
                        }
                    }
                }
            }
        }

        // Handle Populators
        boolean handlePopulators = false;

        for (List<Transaction<BlockSnapshot>> transactions : this.capturedSpongePopulators.values()) {
            if (transactions.size() > 0) {
                handlePopulators = true;
                break;
            }
        }

        if (handlePopulators && cause.first(Chunk.class).isPresent()) {
            Chunk targetChunk = cause.first(Chunk.class).get();
            PopulateChunkEvent.Post event =
                    SpongeEventFactory.createPopulateChunkEventPost(SpongeImpl.getGame(), cause, ImmutableMap.copyOf(this.capturedSpongePopulators),
                            targetChunk);
            SpongeImpl.postEvent(event);

            for (List<Transaction<BlockSnapshot>> transactions : event.getPopulatedTransactions().values()) {
                markAndNotifyBlockPost(transactions, CaptureType.POPULATE, cause);
            }

            for (List<Transaction<BlockSnapshot>> transactions : this.capturedSpongePopulators.values()) {
                transactions.clear();
            }
        }

        // Handle Player Toss
        if (player != null && packetIn instanceof C07PacketPlayerDigging) {
            C07PacketPlayerDigging digPacket = (C07PacketPlayerDigging) packetIn;
            if (digPacket.getStatus() == C07PacketPlayerDigging.Action.DROP_ITEM) {
                destructDrop = false;
            }
        }

        // Handle Player kill commands
        if (player != null && packetIn instanceof C01PacketChatMessage) {
            C01PacketChatMessage chatPacket = (C01PacketChatMessage) packetIn;
            if (chatPacket.getMessage().contains("kill")) {
                if (!cause.all().contains(player)) {
                    cause = cause.with(player);
                }
                destructDrop = true;
            }
        }

        // Handle Player Entity destruct
        if (player != null && packetIn instanceof C02PacketUseEntity) {
            C02PacketUseEntity packet = (C02PacketUseEntity) packetIn;
            if (packet.getAction() == C02PacketUseEntity.Action.ATTACK) {
                net.minecraft.entity.Entity entity = packet.getEntityFromWorld(this.mcWorld);
                if (entity != null && entity.isDead && !(entity instanceof EntityLivingBase)) {
                    Player spongePlayer = (Player) player;
                    MessageSink originalSink = spongePlayer.getMessageSink();
                    MessageSink sink = spongePlayer.getMessageSink();

                    DestructEntityEvent event = SpongeEventFactory.createDestructEntityEvent(SpongeImpl.getGame(), cause, Texts.of(), Texts.of(),
                            originalSink, sink, (Entity) entity);
                    SpongeImpl.getGame().getEventManager().post(event);
                    Text returned = Texts.format(event.getMessage());
                    if (returned != Texts.of()) {
                        event.getSink().sendMessage(returned);
                    }
                    StaticMixinHelper.lastDestroyedEntityId = entity.getEntityId();
                }
            }
        }

        // Inventory Events
        if (player != null && player.getHealth() > 0 && StaticMixinHelper.lastOpenContainer != null) {
            if (packetIn instanceof C10PacketCreativeInventoryAction && !StaticMixinHelper.ignoreCreativeInventoryPacket) {
                SpongeCommonEventFactory.handleCreativeClickInventoryEvent(Cause.of(player), player, (C10PacketCreativeInventoryAction) packetIn);
            } else {
                SpongeCommonEventFactory.handleInteractInventoryOpenCloseEvent(Cause.of(player), player, packetIn);
                if (packetIn instanceof C0EPacketClickWindow) {
                    SpongeCommonEventFactory.handleClickInteractInventoryEvent(Cause.of(player), player, (C0EPacketClickWindow) packetIn);
                }
            }
        }

        // Handle Entity captures
        if (this.capturedEntityItems.size() > 0) {
            if (StaticMixinHelper.dropCause != null) {
                cause = StaticMixinHelper.dropCause;
                destructDrop = true;
            }
            handleDroppedItems(cause, (List<Entity>) (List<?>) this.capturedEntityItems, invalidTransactions, destructDrop);
        }
        if (this.capturedEntities.size() > 0) {
            handleEntitySpawns(cause, this.capturedEntities, invalidTransactions);
        }

        StaticMixinHelper.dropCause = null;
    }

    private void handlePostPlayerBlockEvent(CaptureType captureType, EntityPlayerMP player, net.minecraft.world.World world,
            List<Transaction<BlockSnapshot>> transactions) {
        if (captureType == CaptureType.BREAK && player != null) {
            // Let the client know the blocks still exist
            for (Transaction<BlockSnapshot> transaction : transactions) {
                BlockSnapshot snapshot = transaction.getOriginal();
                BlockPos pos = VecHelper.toBlockPos(snapshot.getPosition());
                player.playerNetServerHandler.sendPacket(new S23PacketBlockChange(world, pos));

                // Update any tile entity data for this block
                net.minecraft.tileentity.TileEntity tileentity = world.getTileEntity(pos);
                if (tileentity != null) {
                    Packet pkt = tileentity.getDescriptionPacket();
                    if (pkt != null) {
                        player.playerNetServerHandler.sendPacket(pkt);
                    }
                }
            }
        } else if (captureType == CaptureType.PLACE && player != null) {
            sendItemChangeToPlayer(player);
        }
    }

    private void sendItemChangeToPlayer(EntityPlayerMP player) {
        if (StaticMixinHelper.lastPlayerItem == null) {
            return;
        }

        // handle revert
        player.isChangingQuantityOnly = true;
        player.inventory.mainInventory[player.inventory.currentItem] = StaticMixinHelper.lastPlayerItem;
        Slot slot = player.openContainer.getSlotFromInventory(player.inventory, player.inventory.currentItem);
        player.openContainer.detectAndSendChanges();
        player.isChangingQuantityOnly = false;
        // force client itemstack update if place event was cancelled
        player.playerNetServerHandler.sendPacket(new S2FPacketSetSlot(player.openContainer.windowId, slot.slotNumber,
                StaticMixinHelper.lastPlayerItem));
    }

    public void handleDroppedItems(Cause cause, List<Entity> entities, List<Transaction<BlockSnapshot>> invalidTransactions, boolean destructItems) {
        Iterator<Entity> iter = entities.iterator();
        ImmutableList.Builder<EntitySnapshot> entitySnapshotBuilder = new ImmutableList.Builder<>();
        while (iter.hasNext()) {
            Entity currentEntity = iter.next();
            if (cause.first(User.class).isPresent()) {
                // store user UUID with entity to track later
                User user = cause.first(User.class).get();
                ((IMixinEntity) currentEntity).trackEntityUniqueId(NbtDataUtil.SPONGE_ENTITY_CREATOR, user.getUniqueId());
            } else if (cause.first(Entity.class).isPresent()) {
                IMixinEntity spongeEntity = (IMixinEntity) cause.first(Entity.class).get();
                Optional<User> owner = spongeEntity.getTrackedPlayer(NbtDataUtil.SPONGE_ENTITY_CREATOR);
                if (owner.isPresent()) {
                    if (!cause.any(NamedCause.OWNER)) {
                        cause = cause.with(NamedCause.of(NamedCause.OWNER, owner.get()));
                    }
                    ((IMixinEntity) currentEntity).trackEntityUniqueId(NbtDataUtil.SPONGE_ENTITY_CREATOR, owner.get().getUniqueId());
                }
            }
            entitySnapshotBuilder.add(currentEntity.createSnapshot());
        }

        DropItemEvent event = null;

        if (destructItems) {
            event = SpongeEventFactory.createDropItemEventDestruct(SpongeImpl.getGame(), cause, entities, entitySnapshotBuilder.build(),
                    (World) this);
        } else {
            event = SpongeEventFactory.createDropItemEventDispense(SpongeImpl.getGame(), cause, entities, entitySnapshotBuilder.build(),
                    (World) this);
        }

        if (!(SpongeImpl.postEvent(event))) {
            // Handle player deaths
            for (Player causePlayer : cause.allOf(Player.class)) {
                EntityPlayerMP playermp = (EntityPlayerMP) causePlayer;
                if (playermp.getHealth() <= 0 || playermp.isDead) {
                    if (!playermp.worldObj.getGameRules().getGameRuleBooleanValue("keepInventory")) {
                        playermp.inventory.clear();
                    } else {
                        // don't drop anything if keepInventory is enabled
                        this.capturedEntityItems.clear();
                    }
                }
            }

            Iterator<Entity> iterator =
                    event instanceof DropItemEvent.Destruct ? ((DropItemEvent.Destruct) event).getEntities().iterator()
                            : ((DropItemEvent.Dispense) event).getEntities().iterator();
            while (iterator.hasNext()) {
                Entity entity = iterator.next();
                boolean invalidSpawn = false;
                if (invalidTransactions != null) {
                    for (Transaction<BlockSnapshot> transaction : invalidTransactions) {
                        if (transaction.getOriginal().getLocation().get().getBlockPosition().equals(entity.getLocation().getBlockPosition())) {
                            invalidSpawn = true;
                            break;
                        }
                    }

                    if (invalidSpawn) {
                        iterator.remove();
                        continue;
                    }
                }

                net.minecraft.entity.Entity nmsEntity = (net.minecraft.entity.Entity) entity;
                int x = MathHelper.floor_double(nmsEntity.posX / 16.0D);
                int z = MathHelper.floor_double(nmsEntity.posZ / 16.0D);
                this.mcWorld.getChunkFromChunkCoords(x, z).addEntity(nmsEntity);
                this.mcWorld.loadedEntityList.add(nmsEntity);
                this.mcWorld.onEntityAdded(nmsEntity);
                SpongeHooks.logEntitySpawn(cause, nmsEntity);
                iterator.remove();
            }
        } else {
            if (cause.root().get() == StaticMixinHelper.packetPlayer) {
                sendItemChangeToPlayer(StaticMixinHelper.packetPlayer);
            }
            this.capturedEntityItems.clear();
        }
    }

    private void handleEntitySpawns(Cause cause, List<Entity> entities, List<Transaction<BlockSnapshot>> invalidTransactions) {
        Iterator<Entity> iter = entities.iterator();
        ImmutableList.Builder<EntitySnapshot> entitySnapshotBuilder = new ImmutableList.Builder<>();
        while (iter.hasNext()) {
            Entity currentEntity = iter.next();
            if (cause.first(User.class).isPresent()) {
                // store user UUID with entity to track later
                User user = cause.first(User.class).get();
                ((IMixinEntity) currentEntity).trackEntityUniqueId(NbtDataUtil.SPONGE_ENTITY_CREATOR, user.getUniqueId());
            } else if (cause.first(Entity.class).isPresent()) {
                IMixinEntity spongeEntity = (IMixinEntity) cause.first(Entity.class).get();
                Optional<User> owner = spongeEntity.getTrackedPlayer(NbtDataUtil.SPONGE_ENTITY_CREATOR);
                if (owner.isPresent()) {
                    if (!cause.all().contains(owner.get())) {
                        cause = cause.with(NamedCause.of(NamedCause.OWNER, owner.get()));
                    }
                    ((IMixinEntity) currentEntity).trackEntityUniqueId(NbtDataUtil.SPONGE_ENTITY_CREATOR, owner.get().getUniqueId());
                }
            }
            entitySnapshotBuilder.add(currentEntity.createSnapshot());
        }

        SpawnEntityEvent event = null;

        if (this.worldSpawnerRunning) {
            event =
                    SpongeEventFactory.createSpawnEntityEventSpawner(SpongeImpl.getGame(), cause, entities, entitySnapshotBuilder.build(),
                            (World) (Object) this);
        } else if (this.chunkSpawnerRunning) {
            event =
                    SpongeEventFactory.createSpawnEntityEventChunkLoad(SpongeImpl.getGame(), cause, entities, entitySnapshotBuilder.build(),
                            (World) (Object) this);
        } else {
            event =
                    SpongeEventFactory
                            .createSpawnEntityEvent(SpongeImpl.getGame(), cause, entities, entitySnapshotBuilder.build(), (World) (Object) this);
        }

        if (!(SpongeImpl.postEvent(event))) {
            Iterator<Entity> iterator = event.getEntities().iterator();
            while (iterator.hasNext()) {
                Entity entity = iterator.next();
                boolean invalidSpawn = false;
                if (invalidTransactions != null) {
                    for (Transaction<BlockSnapshot> transaction : invalidTransactions) {
                        if (transaction.getOriginal().getLocation().get().getBlockPosition().equals(entity.getLocation().getBlockPosition())) {
                            invalidSpawn = true;
                            break;
                        }
                    }

                    if (invalidSpawn) {
                        iterator.remove();
                        continue;
                    }
                }
                net.minecraft.entity.Entity nmsEntity = (net.minecraft.entity.Entity) entity;
                if (nmsEntity instanceof EntityWeatherEffect) {
                    this.mcWorld.addWeatherEffect(nmsEntity);
                } else {
                    int x = MathHelper.floor_double(nmsEntity.posX / 16.0D);
                    int z = MathHelper.floor_double(nmsEntity.posZ / 16.0D);
                    this.mcWorld.getChunkFromChunkCoords(x, z).addEntity(nmsEntity);
                    this.mcWorld.loadedEntityList.add(nmsEntity);
                    this.mcWorld.onEntityAdded(nmsEntity);
                    SpongeHooks.logEntitySpawn(cause, nmsEntity);
                }
                iterator.remove();
            }
        } else {
            this.capturedEntities.clear();
        }
    }

    public SpongeBlockSnapshot createSpongeBlockSnapshot(IBlockState state, IBlockState extended, BlockPos pos, int updateFlag) {
        this.builder.reset();
        Location<World> location = new Location<>((World) this, VecHelper.toVector(pos));
        this.builder.blockState((BlockState) state)
                .extendedState((BlockState) extended)
                .worldId(location.getExtent().getUniqueId())
                .position(location.getBlockPosition());
        if (state.getBlock() instanceof ITileEntityProvider) {
            net.minecraft.tileentity.TileEntity te = this.mcWorld.getTileEntity(pos);
            NBTTagCompound nbt = null;
            if (te != null) {
                nbt = new NBTTagCompound();
                te.writeToNBT(nbt);
            }
            if (nbt != null) {
                this.builder.unsafeNbt(nbt);
            }
        }

        return new SpongeBlockSnapshot(this.builder, updateFlag);
    }

    public boolean restoringBlocks() {
        return this.restoringBlocks;
    }

    public boolean isWorldSpawnerRunning() {
        return this.worldSpawnerRunning;
    }

    public boolean isChunkSpawnerRunning() {
        return this.chunkSpawnerRunning;
    }

    public void setWorldSpawnerRunning(boolean flag) {
        this.worldSpawnerRunning = flag;
    }

    public void setChunkSpawnerRunning(boolean flag) {
        this.chunkSpawnerRunning = flag;
    }

    // == NEW ==

    public void captureTickingEntity(Entity entityIn) {
        this.processingCaptureCause = true;
        this.currentTickEntity = entityIn;
    }

    public void finishTickingEntityCapture() {
        this.currentTickEntity = null;
        this.processingCaptureCause = false;
    }

    public boolean isCapturingEntity() {
        return this.currentTickEntity != null;
    }

    public void captureTickingTile(TileEntity tile) {
        this.processingCaptureCause = true;
        this.currentTickTileEntity = tile;

    }

    public void finishTickingTileCapture() {
        this.currentTickTileEntity = null;
        this.processingCaptureCause = false;
    }

    public boolean isCapturingTile() {
        return this.currentTickTileEntity != null;
    }

    public boolean isCapturingBlock() {
        return this.currentTickBlock != null;
    }

    public void captureTickingBlock(SpongeBlockSnapshot block) {
        this.processingCaptureCause = true;
        this.currentTickBlock = block;
    }

    public void finishTickingBlockCapture() {
        this.currentTickBlock = null;
        this.processingCaptureCause = false;
    }

}
