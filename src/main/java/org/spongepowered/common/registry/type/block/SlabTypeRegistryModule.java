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
package org.spongepowered.common.registry.type.block;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.minecraft.block.BlockStoneSlab;
import net.minecraft.block.BlockStoneSlabNew;
import org.spongepowered.api.data.type.SlabType;
import org.spongepowered.api.data.type.SlabTypes;
import org.spongepowered.common.registry.CatalogRegistryModule;
import org.spongepowered.common.registry.util.RegisterCatalog;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public final class SlabTypeRegistryModule implements CatalogRegistryModule<SlabType> {

    @RegisterCatalog(SlabTypes.class)
    private final Map<String, SlabType> slabTypeMappings = new ImmutableMap.Builder<String, SlabType>()
        .put("brick", (SlabType) (Object) BlockStoneSlab.EnumType.BRICK)
        .put("cobblestone", (SlabType) (Object) BlockStoneSlab.EnumType.COBBLESTONE)
        .put("netherbrick", (SlabType) (Object) BlockStoneSlab.EnumType.NETHERBRICK)
        .put("quartz", (SlabType) (Object) BlockStoneSlab.EnumType.QUARTZ)
        .put("sand", (SlabType) (Object) BlockStoneSlab.EnumType.SAND)
        .put("smooth_brick", (SlabType) (Object) BlockStoneSlab.EnumType.SMOOTHBRICK)
        .put("stone", (SlabType) (Object) BlockStoneSlab.EnumType.STONE)
        .put("wood", (SlabType) (Object) BlockStoneSlab.EnumType.WOOD)
        .put("red_sand", (SlabType) (Object) BlockStoneSlabNew.EnumType.RED_SANDSTONE)
        .build();

    @Override
    public Optional<SlabType> getById(String id) {
        return Optional.ofNullable(this.slabTypeMappings.get(checkNotNull(id).toLowerCase()));
    }

    @Override
    public Collection<SlabType> getAll() {
        return ImmutableList.copyOf(this.slabTypeMappings.values());
    }

}
