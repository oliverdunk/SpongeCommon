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

import com.flowpowered.math.vector.Vector3i;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.world.PlayerSimulator;
import org.spongepowered.common.registry.SpongeGameRegistry;

public class SpongePlayerSimulator implements PlayerSimulator {

    private EntityPlayerMP player;

    SpongePlayerSimulator(EntityPlayerMP player) {
        this.player = player;
        reset();
    }

    private SpongePlayerSimulator reset() {
        return resetAt(0, 0, 0);
    }

    private SpongePlayerSimulator resetAt(int x, int y, int z) {
        this.player.inventory.clear();
        this.player.onGround = true;
        return this.at(x, y, z);
    }

    void unload() {
        reset();
        this.player.worldObj = null;
        this.player.theItemInWorldManager.setWorld(null);
        this.player = null;
    }

    public SpongePlayerSimulator at(double x, double y, double z) {
        this.player.posX = x;
        this.player.posY = y;
        this.player.posZ = z;
        return this;
    }

    public SpongePlayerSimulator holdStack(ItemStack itemStack) {
        this.player.inventory.setItemStack((net.minecraft.item.ItemStack) itemStack);
        this.player.inventory.mainInventory[this.player.inventory.currentItem] = (net.minecraft.item.ItemStack) itemStack;
        return this;
    }

    public EntityPlayerMP getPlayer() {
        return this.player;
    }

    @Override
    public void interactBlock(int x, int y, int z, Direction side) {
        interactBlockWith(x, y, z, null, side);
    }

    @Override
    public void interactBlockWith(int x, int y, int z, ItemStack itemStack, Direction side) {
        checkArgument(side.isCardinal() || side.isUpright(), "Direction must be a valid block face");
        resetAt(x, y, z).holdStack(itemStack);
        EnumFacing facing = SpongeGameRegistry.directionMap.get(side);
        this.player.theItemInWorldManager.activateBlockOrUseItem(this.player, this.player.worldObj, (net.minecraft.item.ItemStack) itemStack,
                new BlockPos(x, y, z),
                facing, 0, 0, 0);
    }

    @Override
    public boolean digBlock(int x, int y, int z) {
        return digBlockWith(x, y, z, null);
    }

    @Override
    public boolean digBlockWith(int x, int y, int z, ItemStack itemStack) {
        resetAt(x, y, z).holdStack(itemStack);
        return this.player.theItemInWorldManager.tryHarvestBlock(new BlockPos(x, y, z));
    }

    @Override
    public int getBlockDigTimeWith(int x, int y, int z, ItemStack itemStack) {
        reset().holdStack(itemStack);
        BlockPos pos = new BlockPos(x, y, z);
        World w = this.player.worldObj;
        // A value from 0.0 to 1.0 representing the percentage of the block
        // broken in one tick. We return the inverse.
        float percentagePerTick = w.getBlockState(pos).getBlock().getPlayerRelativeBlockHardness(this.player, w, pos);
        return MathHelper.ceiling_float_int(1 / percentagePerTick);
    }

    @Override
    public void interactBlock(Vector3i position, Direction side) {
        interactBlock(checkNotNull(position, "position").getX(), position.getY(), position.getZ(), checkNotNull(side, "side"));
    }

    @Override
    public void interactBlockWith(Vector3i position, ItemStack itemStack, Direction side) {
        interactBlockWith(checkNotNull(position, "position").getX(), position.getY(), position.getZ(), checkNotNull(itemStack, "itemStack"),
                checkNotNull(side, "side"));
    }

    @Override
    public boolean digBlock(Vector3i position) {
        return digBlock(checkNotNull(position, "position").getX(), position.getY(), position.getZ());
    }

    @Override
    public boolean digBlockWith(Vector3i position, ItemStack itemStack) {
        return digBlockWith(checkNotNull(position, "position").getX(), position.getY(), position.getZ(), checkNotNull(itemStack, "itemStack"));
    }

    @Override
    public int getBlockDigTimeWith(Vector3i position, ItemStack itemStack) {
        return getBlockDigTimeWith(checkNotNull(position, "position").getX(), position.getY(), position.getZ(), checkNotNull(itemStack, "itemStack"));
    }

}
