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

import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.Scoreboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.common.interfaces.IMixinScore;
import org.spongepowered.common.interfaces.IMixinScoreboard;
import org.spongepowered.common.interfaces.IMixinServerScoreboard;
import org.spongepowered.common.scoreboard.SpongeScore;

@Mixin(Score.class)
public abstract class MixinScore implements IMixinScore {

    @Shadow public Scoreboard theScoreboard;

    public SpongeScore spongeScore;

    @Override
    public SpongeScore getSpongeScore() {
        return this.spongeScore;
    }

    @Override
    public void setSpongeScore(SpongeScore score) {
        this.spongeScore = score;
    }

    @Inject(method = "setScorePoints", at = @At("HEAD"), cancellable = true)
    public void onSetScorePoints(int points, CallbackInfo ci) {
        if (this.theScoreboard != null && ((IMixinScoreboard) this.theScoreboard).isClient()) {
            return; // Let the normal logic take over.
        }
        if (this.spongeScore == null) {
            System.err.println("Returning score because null score!");
            ci.cancel();
            return;
        }
        this.spongeScore.setScore(points);
        ci.cancel();
    }

}
