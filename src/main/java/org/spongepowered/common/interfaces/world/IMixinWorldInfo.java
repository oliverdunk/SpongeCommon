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
package org.spongepowered.common.interfaces.world;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.scoreboard.ServerScoreboard;
import org.spongepowered.api.world.DimensionType;
import org.spongepowered.common.config.SpongeConfig;
import org.spongepowered.common.config.SpongeConfig.WorldConfig;

import java.util.Optional;
import java.util.UUID;

public interface IMixinWorldInfo {

    NBTTagCompound getSpongeRootLevelNbt();

    NBTTagCompound getSpongeNbt();

    int getIndexForUniqueId(UUID uuid);

    Optional<UUID> getUniqueIdForIndex(int index);

    int getDimensionId();

    boolean getIsMod();

    SpongeConfig<WorldConfig> getWorldConfig();

    void setDimensionId(int id);

    void setSpongeRootLevelNBT(NBTTagCompound nbt);

    void setUUID(UUID uuid);

    void setDimensionType(DimensionType type);

    void setSeed(long seed);

    void setWorldName(String name);

    void readSpongeNbt(NBTTagCompound spongeNbt);

    void setIsMod(boolean isMod);

    void setWorldConfig(SpongeConfig<WorldConfig> config);

    void createWorldConfig();

    void setScoreboard(ServerScoreboard scoreboard);
}
