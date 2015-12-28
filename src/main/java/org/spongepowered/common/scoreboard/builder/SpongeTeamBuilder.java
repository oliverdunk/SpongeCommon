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
package org.spongepowered.common.scoreboard.builder;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import net.minecraft.scoreboard.ScorePlayerTeam;
import org.spongepowered.api.scoreboard.Team;
import org.spongepowered.api.scoreboard.Visibilities;
import org.spongepowered.api.scoreboard.Visibility;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.text.format.TextColor;
import org.spongepowered.api.text.format.TextColors;

import java.util.HashSet;
import java.util.Set;

public class SpongeTeamBuilder implements Team.Builder {

    private String name;
    private Text displayName;
    private TextColor color;
    private Text prefix;
    private Text suffix;
    private boolean allowFriendlyFire;
    private boolean showFriendlyInvisibles;
    private Visibility nameTagVisibility;
    private Visibility deathMessageVisibility;
    private Set<Text> members;

    public SpongeTeamBuilder() {
        reset();
    }

    @Override
    public Team.Builder name(String name) {
        this.name = checkNotNull(name, "Name cannot be null!");
        if (this.displayName == null) {
            this.displayName = Texts.of(this.name);
        }
        return this;
    }

    @Override
    public Team.Builder color(TextColor color) throws IllegalArgumentException {
        checkNotNull(color, "Color cannot be null!");
        if (color == TextColors.RESET) {
            throw new IllegalArgumentException("Color cannot be TextColors.RESET!");
        }
        this.color = color;
        return this;
    }

    @Override
    public Team.Builder displayName(Text displayName) throws IllegalArgumentException {
        this.displayName = checkNotNull(displayName, "DisplayName cannot be null!");
        return this;
    }

    @Override
    public Team.Builder prefix(Text prefix) {
        this.prefix = checkNotNull(prefix, "Prefix cannot be null!");
        return this;
    }

    @Override
    public Team.Builder suffix(Text suffix) {
        this.suffix = checkNotNull(suffix, "Suffix cannot be null!");
        return this;
    }

    @Override
    public Team.Builder allowFriendlyFire(boolean enabled) {
        this.allowFriendlyFire = enabled;
        return this;
    }

    @Override
    public Team.Builder canSeeFriendlyInvisibles(boolean enabled) {
        this.showFriendlyInvisibles = enabled;
        return this;
    }

    @Override
    public Team.Builder nameTagVisibility(Visibility visibility) {
        this.nameTagVisibility = checkNotNull(visibility, "Visibility cannot be null!");
        return this;
    }

    @Override
    public Team.Builder deathTextVisibility(Visibility visibility) {
        this.deathMessageVisibility = checkNotNull(visibility, "Visibility cannot be null!");
        return this;
    }

    @Override
    public Team.Builder members(Set<Text> members) {
        this.members = new HashSet<>(checkNotNull(members, "Members cannot be null!"));
        return this;
    }

    @Override
    public Team.Builder from(Team value) {
        this.name(value.getName())
            .displayName(value.getDisplayName())
            .prefix(value.getPrefix())
            .color(value.getColor())
            .allowFriendlyFire(value.allowFriendlyFire())
            .canSeeFriendlyInvisibles(value.canSeeFriendlyInvisibles())
            .suffix(value.getSuffix())
            .nameTagVisibility(value.getNameTagVisibility())
            .deathTextVisibility(value.getDeathMessageVisibility())
            .members(value.getMembers());
        return this;
    }

    @Override
    public SpongeTeamBuilder reset() {
        this.name = null;
        this.displayName = null;
        this.color = TextColors.RESET;
        this.prefix = Texts.of();
        this.suffix = Texts.of();
        this.allowFriendlyFire = false;
        this.showFriendlyInvisibles = false;
        this.nameTagVisibility = Visibilities.ALL;
        this.deathMessageVisibility = Visibilities.ALL;
        this.members = new HashSet<>();
        return this;
    }

    @Override
    public Team build() throws IllegalStateException {
        checkState(this.name != null, "Name cannot be null!");
        checkState(this.displayName != null, "DisplayName cannot be null!");

        Team team = (Team) new ScorePlayerTeam(null, this.name);
        team.setDisplayName(displayName);
        team.setColor(this.color);
        team.setPrefix(this.prefix);
        team.setSuffix(this.suffix);
        team.setAllowFriendlyFire(this.allowFriendlyFire);
        team.setCanSeeFriendlyInvisibles(this.showFriendlyInvisibles);
        team.setNameTagVisibility(this.nameTagVisibility);
        team.setDeathMessageVisibility(this.deathMessageVisibility);
        for (Text member: this.members) {
            team.addMember(member);
        }

        return team;
    }
}
