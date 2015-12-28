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
package org.spongepowered.common.mixin.core.scoreboard;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S3BPacketScoreboardObjective;
import net.minecraft.network.play.server.S3CPacketUpdateScore;
import net.minecraft.network.play.server.S3DPacketDisplayScoreboard;
import net.minecraft.network.play.server.S3EPacketTeams;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.ServerScoreboard;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.ServerConfigurationManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.common.interfaces.IMixinServerScoreboard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

@Mixin(ServerScoreboard.class)
public abstract class MixinServerScoreboardPacketSending extends MixinScoreboardLogic implements IMixinServerScoreboard {

    private static final String SEND_PACKET_METHOD = "Lnet/minecraft/server/management/ServerConfigurationManager;sendPacketToAllPlayers(Lnet/minecraft/network/Packet;)V";
    private static final String SET_CONTAINS = "Ljava/util/Set;contains(Ljava/lang/Object;)Z";

    @Shadow
    MinecraftServer scoreboardMCServer;

    private List<EntityPlayerMP> players = new ArrayList<>();

    @Override
    public void sendToPlayers(Packet packet) {
        for (EntityPlayerMP player: this.players) {
            player.playerNetServerHandler.sendPacket(packet);
        }
    }

    @Override
    public void addPlayer(EntityPlayerMP player) {
        System.out.println("Adding player!");
        this.players.add(player);
        this.sendScoreboard(player);
    }

    void sendScoreboard(EntityPlayerMP player) {
        for (ScorePlayerTeam team: (Collection<ScorePlayerTeam>) this.getTeams()) {
            player.playerNetServerHandler.sendPacket(new S3EPacketTeams(team, 0));
        }

        for (ScoreObjective objective: (Collection<ScoreObjective>) this.getScoreObjectives()) {
            player.playerNetServerHandler.sendPacket(new S3BPacketScoreboardObjective(objective, 0));
            for (Score score: (Collection<Score>) this.getSortedScores(objective)) {
                player.playerNetServerHandler.sendPacket(new S3CPacketUpdateScore(score));
            }
        }

        for (int i = 0; i < 19; ++i) {
            player.playerNetServerHandler.sendPacket(new S3DPacketDisplayScoreboard(i, this.getObjectiveInDisplaySlot(i)));
        }
    }

    @Override
    public void removePlayer(EntityPlayerMP player) {
        this.players.remove(player);
        this.removeScoreboard(player);
    }

    void removeScoreboard(EntityPlayerMP player) {
        this.removeTeams(player);
        this.removeObjectives(player);
    }

    @SuppressWarnings("unchecked")
    void removeTeams(EntityPlayerMP player) {
        for (ScorePlayerTeam team: (Collection<ScorePlayerTeam>) this.getTeams()) {
            player.playerNetServerHandler.sendPacket(new S3EPacketTeams(team, 1));
        }
    }

    @SuppressWarnings("unchecked")
    void removeObjectives(EntityPlayerMP player) {
        for (ScoreObjective objective: (Collection<ScoreObjective>) this.getScoreObjectives()) {
            player.playerNetServerHandler.sendPacket(new S3BPacketScoreboardObjective(objective, 1));
        }
    }

    @Redirect(method = "func_96536_a", at = @At(value = "INVOKE", target = SEND_PACKET_METHOD))
    public void onUpdateScoreValue(ServerConfigurationManager manager, Packet packet) {
        this.sendToPlayers(packet);
    }

    @Redirect(method = "func_96536_a", at = @At(value = "INVOKE", target = SET_CONTAINS))
    public boolean onUpdateScoreValue(Set set, Object object) {
        return true;
    }

    @Redirect(method = "func_96516_a", at = @At(value = "INVOKE", target = SEND_PACKET_METHOD))
    public void onRemoveScore(ServerConfigurationManager manager, Packet packet) {
        this.sendToPlayers(packet);
    }

    @Redirect(method = "func_178820_a", at = @At(value = "INVOKE", target = SEND_PACKET_METHOD))
    public void onRemoveScoreForObjective(ServerConfigurationManager manager, Packet packet) {
        this.sendToPlayers(packet);
    }

    //@Redirect(method = "setObjectiveInDisplaySlot", at = @At(value = "INVOKE", target = SEND_PACKET_METHOD))
    /*public void onSetObjectiveInDisplaySlot(ServerConfigurationManager manager, Packet packet) {
        this.sendToPlayers(packet);
    }*/

    @Redirect(method = "addPlayerToTeam", at = @At(value = "INVOKE", target = SEND_PACKET_METHOD))
    public void onAddPlayerToTeam(ServerConfigurationManager manager, Packet packet) {
        this.sendToPlayers(packet);
    }

    @Redirect(method = "removePlayerFromTeam", at = @At(value = "INVOKE", target = SEND_PACKET_METHOD))
    public void onRemovePlayerFromTeam(ServerConfigurationManager manager, Packet packet) {
        this.sendToPlayers(packet);
    }

    @Redirect(method = "func_96532_b", at = @At(value = "INVOKE", target = SEND_PACKET_METHOD))
    public void onUpdateObjective(ServerConfigurationManager manager, Packet packet) {
        this.sendToPlayers(packet);
    }

    @Redirect(method = "func_96532_b", at = @At(value = "INVOKE", target = SET_CONTAINS))
    public boolean onUpdateObjective(Set set, Object object) {
        return true;
    }

    @Redirect(method = "func_96533_c", at = @At(value = "INVOKE", target = SET_CONTAINS))
    public boolean onSendDisplayPackets(Set set, Object object) {
        return true;
    }

    @Redirect(method = "broadcastTeamCreated", at = @At(value = "INVOKE", target = SEND_PACKET_METHOD))
    public void onBroadcastTeamCreated(ServerConfigurationManager manager, Packet packet) {
        this.sendToPlayers(packet);
    }

    @Redirect(method = "sendTeamUpdate", at = @At(value = "INVOKE", target = SEND_PACKET_METHOD))
    public void onSendTeamUpdate(ServerConfigurationManager manager, Packet packet) {
        this.sendToPlayers(packet);
    }

    @Redirect(method = "func_96513_c", at = @At(value = "INVOKE", target = SEND_PACKET_METHOD))
    public void onRemoveTeam(ServerConfigurationManager manager, Packet packet) {
        this.sendToPlayers(packet);
    }

    @SuppressWarnings("rawtypes")
    @Redirect(method = "func_96549_e", at = @At(value = "INVOKE", target = "Ljava/util/List;iterator()Ljava/util/Iterator;", ordinal = 0, remap =
            false))
    public Iterator onGetPlayerIteratorForObjectives(List list) {
        return this.players.iterator();
    }

    @SuppressWarnings("rawtypes")
    @Redirect(method = "getPlayerIterator", at = @At(value = "INVOKE", target = "Ljava/util/List;iterator()Ljava/util/Iterator;", ordinal = 0, remap =
            false))
    public Iterator onGetPlayerIterator(List list) {
        return this.players.iterator();
    }
}
