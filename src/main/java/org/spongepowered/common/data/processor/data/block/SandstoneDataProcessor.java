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
package org.spongepowered.common.data.processor.data.block;

import net.minecraft.block.BlockSandStone;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.manipulator.immutable.block.ImmutableSandstoneData;
import org.spongepowered.api.data.manipulator.mutable.block.SandstoneData;
import org.spongepowered.api.data.type.SandstoneType;
import org.spongepowered.api.data.type.SandstoneTypes;
import org.spongepowered.api.data.value.mutable.Value;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.common.data.manipulator.mutable.block.SpongeSandstoneData;
import org.spongepowered.common.data.processor.common.AbstractCatalogDataProcessor;

public class SandstoneDataProcessor extends AbstractCatalogDataProcessor<SandstoneType, Value<SandstoneType>, SandstoneData, ImmutableSandstoneData> {

    public SandstoneDataProcessor() {
        super(Keys.SANDSTONE_TYPE, input -> input.getItem() == ItemTypes.SANDSTONE || input.getItem() == ItemTypes.SANDSTONE_STAIRS
                || input.getItem() == ItemTypes.RED_SANDSTONE || input.getItem() == ItemTypes.RED_SANDSTONE_STAIRS);
    }

    @Override
    protected int setToMeta(SandstoneType value) {
        return ((BlockSandStone.EnumType) (Object) value).getMetadata();
    }

    @Override
    protected SandstoneType getFromMeta(int meta) {
        return (SandstoneType) (Object) BlockSandStone.EnumType.byMetadata(meta);
    }

    @Override
    public SandstoneData createManipulator() {
        return new SpongeSandstoneData();
    }

    @Override
    protected SandstoneType getDefaultValue() {
        return SandstoneTypes.DEFAULT;
    }
}
