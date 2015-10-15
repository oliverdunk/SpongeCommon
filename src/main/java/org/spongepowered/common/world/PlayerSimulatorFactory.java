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
package org.spongepowered.common.world;

import com.google.common.collect.Maps;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.WorldServer;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.world.UnloadWorldEvent;
import org.spongepowered.api.world.World;
import org.spongepowered.common.Sponge;

import java.util.Map;

/**
 * The simulated player idea is based on Forge's FakePlayer.
 */
public class PlayerSimulatorFactory {

    public static PlayerSimulatorFactory instance = new PlayerSimulatorFactory();

    private final Map<WorldServer, SpongePlayerSimulator> players = Maps.newHashMap();

    public PlayerSimulatorFactory() {
        if (instance != null) {
            Sponge.getGame().getEventManager().unregisterListeners(instance);
        }
        Sponge.getGame().getEventManager().registerListeners(Sponge.getPlugin(), this);
    }

    public final SpongePlayerSimulator getSimulator(WorldServer world) {
        SpongePlayerSimulator instance = this.players.get(world);
        if (instance == null) {
            instance = new SpongePlayerSimulator(this.createPlayer(world));
            this.players.put(world, instance);
        }
        return instance;
    }

    protected EntityPlayerMP createPlayer(WorldServer world) {
        return new SimulatedPlayer(world);
    }

    @Listener
    public void onWorldUnload(UnloadWorldEvent event) {
        World world = event.getTargetWorld();
        if (this.players.containsKey(world)) {
            this.players.remove(world).unload();
        }
    }
}
