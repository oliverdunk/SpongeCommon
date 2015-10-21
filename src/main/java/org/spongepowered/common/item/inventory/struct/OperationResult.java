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
package org.spongepowered.common.item.inventory.struct;

import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.transaction.InventoryOperationResult;

import java.util.Collection;
import java.util.Optional;


/**
 * An inventory operation result with type
 */
public class OperationResult implements InventoryOperationResult {

    /**
     * The result type 
     */
    private final Type type;

    /**
     * ctor
     * 
     * @param type result type
     */
    public OperationResult(Type type) {
        this.type = type;
    }

    /* (non-Javadoc)
     * @see InventoryOperationResult#getType()
     */
    @Override
    public final Type getType() {
        return this.type;
    }

    /* (non-Javadoc)
     * @see InventoryOperationResult#getRejectedItems()
     */
    @Override
    public Optional<Collection<ItemStack>> getRejectedItems() {
        return Optional.<Collection<ItemStack>>empty();
    }

    /* (non-Javadoc)
     * @see InventoryOperationResult#getReplacedItems()
     */
    @Override
    public Optional<Collection<ItemStack>> getReplacedItems() {
        return Optional.<Collection<ItemStack>>empty();
    }

}
