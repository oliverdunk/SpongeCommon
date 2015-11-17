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
package org.spongepowered.common.world;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.world.WorldServer;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.profile.GameProfile;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectCollection;
import org.spongepowered.api.service.permission.SubjectData;
import org.spongepowered.api.service.permission.context.Context;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.world.PlayerSimulator;
import org.spongepowered.api.world.World;
import org.spongepowered.common.registry.provider.DirectionFacingProvider;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class SpongePlayerSimulator implements PlayerSimulator {

    private WeakReference<EntityPlayerMP> player;
    private WeakReference<WorldServer> world;
    private final GameProfile profile;
    private Cause eventCause;

    SpongePlayerSimulator(GameProfile profile) {
        this.profile = profile;
        this.player = new WeakReference<>(null);
        this.world = new WeakReference<>(null);
        this.setCause(null);
    }

    private EntityPlayerMP setUpPlayer(int x, int y, int z, ItemStack itemInHand) {
        checkState(this.world.get() != null, "World not set or world is unloaded");
        EntityPlayerMP player = this.player.get();
        if (player == null) {
            player = PlayerSimulatorFactory.instance.createPlayer(this.world.get(), this.profile);
            this.player = new WeakReference<>(player);
        }
        player.posX = x;
        player.posY = y;
        player.posZ = z;
        player.onGround = true;
        player.inventory.setItemStack((net.minecraft.item.ItemStack) itemInHand);
        player.inventory.mainInventory[player.inventory.currentItem] = (net.minecraft.item.ItemStack) itemInHand;
        return player;
    }

    private static void tearDownPlayer(EntityPlayerMP player) {
        player.inventory.clear();
        player.worldObj = null;
        player.theItemInWorldManager.setWorld(null);
    }

    // TODO THIS IS VERY WIP
    private void testPermission(String permission) {
        if (this.world.get() == null) {
            return;
        }
        permission = "sponge.simulated." + this.profile.getName() + "." + permission;
        // checkState(this.hasPermission(Sets.newHashSet(((World)
        // this.world.get()).getContext()), permission), "Permission denied");
    }

    @Override
    public GameProfile getProfile() {
        return this.profile;
    }

    @Override
    public Optional<World> getWorld() {
        return Optional.ofNullable((World) this.world.get());
    }

    @Override
    public void setWorld(org.spongepowered.api.world.World world) {
        if (this.world.get() != world) {
            this.world = new WeakReference<>((WorldServer) world);
        }
    }

    @Override
    public Cause getCause() {
        return this.eventCause;
    }

    @Override
    public void setCause(Cause cause) {
        if (cause == null) {
            cause = Cause.of(this);
        }
        this.eventCause = cause;
    }

    @Override
    public void interactBlock(int x, int y, int z, Direction side) {
        interactBlockWith(x, y, z, null, side);
    }

    @Override
    public void interactBlockWith(int x, int y, int z, ItemStack itemStack, Direction side) {
        checkArgument(checkNotNull(side, "side").isCardinal() || side.isUpright(), "Direction must be a valid block face");
        testPermission("interact");
        EntityPlayerMP player = setUpPlayer(x, y, z, itemStack);
        EnumFacing facing = DirectionFacingProvider.directionMap.get(side);
        player.theItemInWorldManager.activateBlockOrUseItem(player, player.worldObj, (net.minecraft.item.ItemStack) itemStack, new BlockPos(x, y, z),
                facing, 0, 0, 0);
        tearDownPlayer(player);
    }

    @Override
    public boolean digBlock(int x, int y, int z) {
        return digBlockWith(x, y, z, null);
    }

    @Override
    public boolean digBlockWith(int x, int y, int z, ItemStack itemStack) {
        testPermission("dig");
        EntityPlayerMP player = setUpPlayer(x, y, z, itemStack);
        boolean result = player.theItemInWorldManager.tryHarvestBlock(new BlockPos(x, y, z));
        tearDownPlayer(player);
        return result;
    }

    @Override
    public int getBlockDigTimeWith(int x, int y, int z, ItemStack itemStack) {
        testPermission("digtime");
        EntityPlayerMP player = setUpPlayer(x, y, z, itemStack);
        BlockPos pos = new BlockPos(x, y, z);
        net.minecraft.world.World w = player.worldObj;
        // A value from 0.0 to 1.0 representing the percentage of the block
        // broken in one tick. We return the inverse.
        float percentagePerTick = w.getBlockState(pos).getBlock().getPlayerRelativeBlockHardness(player, w, pos);
        tearDownPlayer(player);
        return MathHelper.ceiling_float_int(1 / percentagePerTick);
    }

    @Override
    public boolean setBlock(int x, int y, int z, BlockState block) {
        checkNotNull(block, "block");
        testPermission("place");
        EntityPlayerMP player = setUpPlayer(x, y, z, null);
        // TODO Actually use the player to simulate
        // Need to convert block to an ItemBlock then let the player 'place' the
        // stack
        boolean result = player.worldObj.setBlockState(new BlockPos(x, y, z), (IBlockState) block);
        tearDownPlayer(player);
        return result;
    }

    @Override
    public String getIdentifier() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Optional<CommandSource> getCommandSource() {
        return Optional.empty();
    }

    @Override
    public SubjectCollection getContainingCollection() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public SubjectData getSubjectData() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public SubjectData getTransientSubjectData() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean hasPermission(Set<Context> contexts, String permission) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean hasPermission(String permission) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Tristate getPermissionValue(Set<Context> contexts, String permission) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isChildOf(Subject parent) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isChildOf(Set<Context> contexts, Subject parent) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public List<Subject> getParents() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<Subject> getParents(Set<Context> contexts) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Set<Context> getActiveContexts() {
        // TODO Auto-generated method stub
        return null;
    }

}
