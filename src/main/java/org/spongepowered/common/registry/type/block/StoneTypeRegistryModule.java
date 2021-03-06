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
import net.minecraft.block.BlockStone;
import org.spongepowered.api.data.type.StoneType;
import org.spongepowered.api.data.type.StoneTypes;
import org.spongepowered.common.registry.CatalogRegistryModule;
import org.spongepowered.common.registry.util.RegisterCatalog;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public final class StoneTypeRegistryModule implements CatalogRegistryModule<StoneType> {

    @RegisterCatalog(StoneTypes.class)
    private final Map<String, StoneType> stoneTypeMappings = new ImmutableMap.Builder<String, StoneType>()
        .put("stone", (StoneType) (Object) BlockStone.EnumType.STONE)
        .put("granite", (StoneType) (Object) BlockStone.EnumType.GRANITE)
        .put("smooth_granite", (StoneType) (Object) BlockStone.EnumType.GRANITE_SMOOTH)
        .put("diorite", (StoneType) (Object) BlockStone.EnumType.DIORITE)
        .put("smooth_diorite", (StoneType) (Object) BlockStone.EnumType.DIORITE_SMOOTH)
        .put("andesite", (StoneType) (Object) BlockStone.EnumType.ANDESITE)
        .put("smooth_andesite", (StoneType) (Object) BlockStone.EnumType.ANDESITE_SMOOTH)
        .build();

    @Override
    public Optional<StoneType> getById(String id) {
        return Optional.ofNullable(this.stoneTypeMappings.get(checkNotNull(id).toLowerCase()));
    }

    @Override
    public Collection<StoneType> getAll() {
        return ImmutableList.copyOf(this.stoneTypeMappings.values());
    }

}
