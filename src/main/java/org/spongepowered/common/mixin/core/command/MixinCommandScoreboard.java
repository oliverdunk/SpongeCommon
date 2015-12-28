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
package org.spongepowered.common.mixin.core.command;

import net.minecraft.command.CommandBase;
import net.minecraft.command.EntityNotFoundException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.server.CommandScoreboard;
import net.minecraft.entity.Entity;
import net.minecraft.scoreboard.Scoreboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import org.spongepowered.common.entity.living.human.EntityHuman;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

/**
 * This mixin is required to make entity selectors work with Humans, when using
 * the '/scoreboard' command. Without it, Humans will be added by their UUID,
 */
@Mixin(CommandScoreboard.class)
public abstract class MixinCommandScoreboard {

    // The static method's owner is CommandScoreboard for some odd reason, despite it coming from CommandBase
    private static final String GET_ENTITY_NAME = "Lnet/minecraft/command/server/CommandScoreboard;getEntityName(Lnet/minecraft/command/ICommandSender;Ljava/lang/String;)Ljava/lang/String;";
    private static final String GET_UNIQUE_ID = "Lnet/minecraft/entity/Entity;getUniqueID()Ljava/util/UUID;";

    private String realName;

    @Inject(method = "joinTeam", at = @At(value = "INVOKE", target = GET_UNIQUE_ID), locals = LocalCapture.CAPTURE_FAILHARD)
    public void onGetUUIDJoin(ICommandSender p_147190_1_, String p_147190_2_[], int p_147190_3_, CallbackInfo ci, Scoreboard scoreboard, String s, HashSet hashset, HashSet hashset1, String s1, List list, Iterator iterator, Entity entity) {
        this.onGetUniqueID(entity);
    }

    @Inject(method = "leaveTeam", at = @At(value = "INVOKE", target = GET_UNIQUE_ID, args = "log=true"), locals = LocalCapture.CAPTURE_FAILHARD)
    public void onGetUUIDLeave(ICommandSender p_147190_1_, String p_147190_2_[], int p_147190_3_, CallbackInfo ci, Scoreboard scoreboard, HashSet s, HashSet hashset, String s1, List list, Iterator iterator, Entity entity) {
        System.out.println("Getting unique ID");
        this.onGetUniqueID(entity);
    }

    @Redirect(method = "joinTeam", at = @At(value = "INVOKE", target = GET_ENTITY_NAME, ordinal = 0))
    public String onGetEntityNameJoin(ICommandSender sender, String string) throws EntityNotFoundException {
        return this.onGetEntityName(sender, string);
    }

    @Redirect(method = "leaveTeam", at = @At(value = "INVOKE", target = GET_ENTITY_NAME, ordinal = 0))
    public String onGetEntityNameLeave(ICommandSender sender, String string) throws EntityNotFoundException {
        System.out.println("Redirecting get name");
        return this.onGetEntityName(sender, string);
    }

    private void onGetUniqueID(Entity entity) {
        if (entity instanceof EntityHuman) {
            System.out.println("Setting real name!");
            this.realName = entity.getCustomNameTag();
        }
    }

    private String onGetEntityName(ICommandSender sender, String string) throws EntityNotFoundException {
        if (this.realName != null) {
            System.out.println("Using real name!");
            String newString = this.realName;
            this.realName = null;
            return newString;
        }
        return CommandBase.getEntityName(sender, string);
    }

}
