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

import java.util.Collection;
import java.util.Optional;

import javax.annotation.Nullable;

/**
 * Inventory operation result with detail
 */
public class OperationResultDetailed extends OperationResult {

    /**
     * Items rejected by the operation
     */
    private final Optional<Collection<ItemStack>> rejected;

    /**
     * Items replaced by the operation
     */
    private final Optional<Collection<ItemStack>> replaced;

    /**
     * Ctor
     * 
     * @param type result type
     * @param rejected Rejected items or null
     * @param replaced Replaced items or null
     */
    public OperationResultDetailed(Type type, @Nullable Collection<ItemStack> rejected, @Nullable Collection<ItemStack> replaced) {
        super(type);
        this.rejected = Optional.<Collection<ItemStack>>ofNullable(rejected);
        this.replaced = Optional.<Collection<ItemStack>>ofNullable(replaced);
    }

    /* (non-Javadoc)
     * @see OperationResult#getRejectedItems()
     */
    @Override
    public Optional<Collection<ItemStack>> getRejectedItems() {
        return this.rejected;
    }
    
    /* (non-Javadoc)
     * @see OperationResult#getReplacedItems()
     */
    @Override
    public Optional<Collection<ItemStack>> getReplacedItems() {
        return this.replaced;
    }
    
}
