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
package org.spongepowered.common.item.inventory.lens.impl.collections;

import org.spongepowered.common.item.inventory.lens.impl.struct.LensHandle;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import org.spongepowered.api.item.inventory.InventoryProperty;
import org.spongepowered.common.item.inventory.lens.Lens;
import org.spongepowered.common.item.inventory.lens.MutableLensCollection;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;


public class MutableLensCollectionImpl extends AbstractList<Lens<IInventory, ItemStack>> implements MutableLensCollection<IInventory, ItemStack> {

    protected final List<LensHandle<IInventory, ItemStack>> lenses;
    
    private boolean resizeSemaphore = false;

    public MutableLensCollectionImpl(int initialCapacity) {
        this.lenses = new ArrayList<LensHandle<IInventory, ItemStack>>(initialCapacity);
        this.resize(initialCapacity);
    }
    
    @Override
    public void add(Lens<IInventory, ItemStack> lens, InventoryProperty<?, ?>... properties) {
        this.lenses.add(new LensHandle<IInventory, ItemStack>(lens, properties));
    }
    
    @Override
    public void add(int index, Lens<IInventory, ItemStack> lens, InventoryProperty<?, ?>... properties) {
        this.lenses.add(index, new LensHandle<IInventory, ItemStack>(lens, properties));
    }

    private void resize(int size) {
        assert !this.resizeSemaphore;
        this.resizeSemaphore = true;
        for (int index = this.lenses.size(); index < size; index++) {
            this.lenses.add(index, new LensHandle<IInventory, ItemStack>());
        }
        this.resizeSemaphore = false;
    }
    
    protected final LensHandle<IInventory, ItemStack> getHandle(int index) {
        return this.getHandle(index, false);
    }
    
    protected final LensHandle<IInventory, ItemStack> getHandle(int index, boolean autoResize) {
        if (index >= this.lenses.size()) {
            if (!autoResize) {
                throw new IndexOutOfBoundsException("Index: " + index);
            }
            this.resize(index + 1);
        }
        return this.lenses.get(index);
    }

    @Override
    public void setProperty(Lens<IInventory, ItemStack> lens, InventoryProperty<?, ?> property) {
        this.setProperty(this.indexOf(lens), property);
    }

    @Override
    public void setProperty(int index, InventoryProperty<?, ?> property) {
        this.checkIndex(index);
        this.getHandle(index).setProperty(property);
    }
    
    @Override
    public void removeProperty(Lens<IInventory, ItemStack> lens, InventoryProperty<?, ?> property) {
        this.removeProperty(this.indexOf(lens), property);
    }
    
    @Override
    public void removeProperty(int index, InventoryProperty<?, ?> property) {
        this.checkIndex(index);
        this.getHandle(index).removeProperty(property);
    }
    
    @Override
    public Lens<IInventory, ItemStack> get(int index) {
        return this.getLens(index);
    }

    @Override
    public Lens<IInventory, ItemStack> getLens(int index) {
//        this.checkIndex(index);
        return this.lenses.get(index).lens;
    }

    @Override
    public int size() {
        return this.lenses.size();
    }
    
    @Override
    public boolean contains(Object o) {
        return this.indexOf(o) >= 0;
    }

    @Override
    public int indexOf(Object o) {
        if (o == null) {
            for (int i = 0; i < this.lenses.size(); i++) {
                if (this.lenses.get(i).lens == null) {
                    return i;
                }
            }
        } else {
            for (int i = 0; i < this.lenses.size(); i++) {
                if (o.equals(this.lenses.get(i).lens)) {
                    return i;
                }
            }
        }
        return -1;
    }

    @Override
    public Collection<InventoryProperty<?, ?>> getProperties(int index) {
        this.checkIndex(index);
        return this.getHandle(index).getProperties();
    }

    @Override
    public boolean has(Lens<IInventory, ItemStack> lens) {
        return this.contains(lens);
    }

    @Override
    public boolean isSubsetOf(Collection<Lens<IInventory, ItemStack>> c) {
        for (Iterator<Lens<IInventory, ItemStack>> iter = c.iterator(); iter.hasNext();) {
            if (!this.contains(iter.next())) {
                return false;
            }
        }
            
        return true;
    }

    private void checkIndex(int index) {
        if (index >= this.lenses.size()) {
            throw new IndexOutOfBoundsException("Index: " + index);
        }
    }

}
