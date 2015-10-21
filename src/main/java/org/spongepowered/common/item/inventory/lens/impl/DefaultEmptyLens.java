/*
 * This file is part of SpongeCommon, licensed under the MIT License (MIT).
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
package org.spongepowered.common.item.inventory.lens.impl;

import gnu.trove.set.hash.TIntHashSet;

import gnu.trove.impl.unmodifiable.TUnmodifiableIntSet;
import gnu.trove.set.TIntSet;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.InventoryProperty;
import org.spongepowered.common.item.inventory.adapter.InventoryAdapter;
import org.spongepowered.common.item.inventory.lens.Lens;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;


public class DefaultEmptyLens extends ObservableLens<IInventory, ItemStack> {

    private static final TIntSet EMPTY_SLOT_SET = new TUnmodifiableIntSet(new TIntHashSet());
    
    protected final InventoryAdapter<IInventory, ItemStack> adapter;
    
    public DefaultEmptyLens(InventoryAdapter<IInventory, ItemStack> adapter) {
        this.adapter = adapter;
    }

    @Override
    public Class<? extends Inventory> getAdapterType() {
        return this.adapter.getClass();
    }

    @Override
    public InventoryAdapter<IInventory, ItemStack> getAdapter(IInventory inv) {
        return this.adapter;
    }

    @Override
    public int slotCount() {
        return 0;
    }

    @Override
    public int getRealIndex(IInventory inv, int ordinal) {
        return -1;
    }

    @Override
    public ItemStack getStack(IInventory inv, int ordinal) {
        return null;
    }
    
    @Override
    public boolean setStack(IInventory inv, int index, ItemStack stack) {
        return false;
    }

    @Override
    public int getMaxStackSize(IInventory inv) {
        return 0;
    }

    @Override
    public Collection<Lens<IInventory, ItemStack>> getChildren() {
        return Collections.<Lens<IInventory, ItemStack>>emptyList();
    }

    @Override
    public Collection<Lens<IInventory, ItemStack>> getSpanningChildren() {
        return Collections.<Lens<IInventory, ItemStack>>emptyList();
    }

    @Override
    public void invalidate(IInventory inv) {
    }

    @Override
    public Lens<IInventory, ItemStack> getLens(int index) {
        return null;
    }

    @Override
    public Collection<InventoryProperty<?, ?>> getProperties(int index) {
        return Collections.<InventoryProperty<?, ?>>emptyList();
    }

    @Override
    public boolean has(Lens<IInventory, ItemStack> lens) {
        return false;
    }

    @Override
    public boolean isSubsetOf(Collection<Lens<IInventory, ItemStack>> c) {
        return true;
    }
    
    @Override
    public TIntSet getSlots() {
        return DefaultEmptyLens.EMPTY_SLOT_SET;
    }
    
    @Override
    public boolean hasSlot(int index) {
        return false;
    }

    @Override
    public Iterator<Lens<IInventory, ItemStack>> iterator() {
        // TODO 
        return null;
    }

}
