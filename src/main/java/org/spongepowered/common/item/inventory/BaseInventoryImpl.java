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
package org.spongepowered.common.item.inventory;

import com.google.common.collect.ImmutableList;
import org.spongepowered.api.item.inventory.EmptyInventory;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.transaction.InventoryOperationResult;
import org.spongepowered.api.item.inventory.transaction.InventoryOperationResult.Type;
import org.spongepowered.api.text.translation.Translation;
import org.spongepowered.common.item.inventory.observer.InventoryEventArgs;
import org.spongepowered.common.item.inventory.struct.OperationResultDetailed;
import org.spongepowered.common.text.translation.SpongeTranslation;
import org.spongepowered.common.util.observer.Observer;

/**
 * Base for all inventory implementation classes, contains boilerplate stuff
 * which would otherwise just be duplicated amongst {@link Inventory}
 * implementors
 */
public abstract class BaseInventoryImpl implements Inventory, Observer<InventoryEventArgs> {

    public static final Translation DEFAULT_NAME = new SpongeTranslation("inventory.default.title");
    
    protected final Translation name; 
    
    protected Inventory parent;
    
    protected Inventory next;
    
    /**
     * All inventories have their own empty inventory with themselves as the
     * parent. This empty inventory is initialised on-demand but returned for
     * every query which fails. This saves us from creating a new empty
     * inventory with this inventory as the parent for every failed query.
     */
    private EmptyInventory empty;  
    
    protected BaseInventoryImpl(Translation name, Inventory parent) {
        this.name = name;
        this.parent = parent;
    }
    
    @Override
    public Translation getName() {
        return this.name;
    }

    @Override
    public Inventory parent() {
        return this.parent;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Inventory> T first() {
        return (T) this.iterator().next();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Inventory> T next() {
        return (T) this.emptyInventory();
    }
    
    @Override
    public boolean offer(ItemStack stack) {
        return false;
    }

    @Override
    public InventoryOperationResult set(ItemStack stack) {
        return new OperationResultDetailed(Type.FAILURE, ImmutableList.<ItemStack>of(stack), null);
    }

    @Override
    public void notify(Object source, InventoryEventArgs eventArgs) {
    }
    
    protected final EmptyInventory emptyInventory() {
        if (this.empty == null) {
            this.empty = new EmptyInventoryImpl(this);
        }
        return this.empty;
    }

}
