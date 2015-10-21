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
package org.spongepowered.common.item.inventory.util;

import org.spongepowered.api.item.inventory.ItemStack;

import java.util.Optional;

public abstract class ItemStackUtil {
    
    private ItemStackUtil() {
    }

    public static net.minecraft.item.ItemStack cloneDefensiveNative(net.minecraft.item.ItemStack stack) {
        return new net.minecraft.item.ItemStack(stack.getItem(), stack.stackSize, stack.getItemDamage());
    }
    
    public static net.minecraft.item.ItemStack cloneDefensiveNative(net.minecraft.item.ItemStack stack, int newSize) {
        return new net.minecraft.item.ItemStack(stack.getItem(), newSize, stack.getItemDamage());
    }
    
    public static ItemStack cloneDefensive(net.minecraft.item.ItemStack stack) {
        return (ItemStack) ItemStackUtil.cloneDefensiveNative(stack);
    }
    
    public static ItemStack cloneDefensive(net.minecraft.item.ItemStack stack, int newSize) {
        return (ItemStack) ItemStackUtil.cloneDefensiveNative(stack, newSize);
    }
    
    public static Optional<ItemStack> cloneDefensiveOptional(net.minecraft.item.ItemStack stack) {
        if (stack == null) {
            return Optional.<ItemStack>empty();
        }
        return Optional.<ItemStack>of(ItemStackUtil.cloneDefensive(stack));
    }

    public static Optional<ItemStack> cloneDefensiveOptional(net.minecraft.item.ItemStack stack, int withdraw) {
        if (stack == null) {
            return Optional.<ItemStack>empty();
        }
        return Optional.<ItemStack>of(ItemStackUtil.cloneDefensive(stack));
    }

    public static boolean compare(net.minecraft.item.ItemStack stack1, net.minecraft.item.ItemStack stack2) {
        return stack1.isItemEqual(stack2);
    }
    
    public static boolean compare(net.minecraft.item.ItemStack stack1, ItemStack stack2) {
        if (stack2 instanceof net.minecraft.item.ItemStack) {
            return ItemStackUtil.compare(stack1, (net.minecraft.item.ItemStack) stack2);
        }
        
        return false; // TODO compare
    }
    
}
