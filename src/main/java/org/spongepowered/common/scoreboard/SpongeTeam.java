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
package org.spongepowered.common.scoreboard;

import static org.spongepowered.api.text.serializer.TextSerializers.LEGACY;

import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.util.EnumChatFormatting;
import org.spongepowered.api.scoreboard.Scoreboard;
import org.spongepowered.api.scoreboard.Team;
import org.spongepowered.api.scoreboard.Visibilities;
import org.spongepowered.api.scoreboard.Visibility;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColor;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.serializer.TextSerializers;
import org.spongepowered.common.interfaces.IMixinScoreboard;
import org.spongepowered.common.interfaces.IMixinTeam;
import org.spongepowered.common.text.format.SpongeTextColor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SpongeTeam implements Team {

    private Map<net.minecraft.scoreboard.Scoreboard, ScorePlayerTeam> teams = new HashMap<>();
    private Set<Text> members = new HashSet<>();

    private String name;
    private Text displayName;
    private TextColor color = TextColors.RESET;
    private Text prefix = Text.of();
    private Text suffix = Text.of();

    private boolean allowFriendlyFire = true;
    private boolean seeFriendlyInvisibles = true;

    private Visibility nameTagVisibility = Visibilities.ALL;
    private Visibility deathMessageVisibility = Visibilities.ALL;

    public boolean allowRecursion = true;

    public SpongeTeam(String name, Text displayName, TextColor color, Text prefix, Text suffix, boolean allowFriendlyFire,
                      boolean seeFriendlyInvisibles, Visibility nameTagVisibility, Visibility deathMessageVisibility, Set<Text> members) {
        this.members = members;
        this.name = name;
        this.displayName = displayName;
        this.color = color;
        this.prefix = prefix;
        this.suffix = suffix;
        this.allowFriendlyFire = allowFriendlyFire;
        this.seeFriendlyInvisibles = seeFriendlyInvisibles;
        this.nameTagVisibility = nameTagVisibility;
        this.deathMessageVisibility = deathMessageVisibility;
    }

    @SuppressWarnings("deprecation")
    public SpongeTeam(String name) {
        this.name = name;
        this.displayName = TextSerializers.LEGACY.parse(name);
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public Text getDisplayName() {
        return this.displayName;
    }

    @Override
    public TextColor getColor() {
        return this.color;
    }

    @Override
    public void setColor(TextColor color) {
        this.color = color;
        this.updateColor();
    }

    private void updateColor() {
        this.allowRecursion = false;
        EnumChatFormatting enumChatFormatting = ((SpongeTextColor) this.color).getHandle();
        for (ScorePlayerTeam team: this.teams.values()) {
            team.setChatFormat(enumChatFormatting);
        }
        this.allowRecursion = true;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void setDisplayName(Text displayName) throws IllegalArgumentException {
        if (displayName.to(LEGACY).length() > 32) {
            throw new IllegalArgumentException("Team display name length cannot be greater than 32 characters!");
        }
        this.displayName = displayName;
        this.updateDisplayName();
    }

    @SuppressWarnings("deprecation")
    private void updateDisplayName() {
        this.allowRecursion = false;
        String displayName = this.displayName.to(LEGACY);
        for (ScorePlayerTeam team: this.teams.values()) {
            team.setTeamName(displayName);
        }
        this.allowRecursion = true;
    }

    @Override
    public Text getPrefix() {
        return this.prefix;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void setPrefix(Text prefix) throws IllegalArgumentException {
        if (prefix.to(LEGACY).length() > 16) {
            throw new IllegalArgumentException("Prefix length cannot be greater than 16 characters!");
        }
        this.prefix = prefix;
        this.updatePrefix();
    }

    @SuppressWarnings("deprecation")
    private void updatePrefix() {
        this.allowRecursion = false;
        String prefix = this.prefix.to(LEGACY);
        for (ScorePlayerTeam team: this.teams.values()) {
            team.setNamePrefix(prefix);
        }
        this.allowRecursion = true;
    }

    @Override
    public Text getSuffix() {
        return this.suffix;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void setSuffix(Text suffix) throws IllegalArgumentException {
        if (suffix.to(LEGACY).length() > 16) {
            throw new IllegalArgumentException("Suffix length cannot be greater than 16 characters!");
        }
        this.suffix = suffix;
        this.updateSuffix();
    }

    @SuppressWarnings("deprecation")
    private void updateSuffix() {
        this.allowRecursion = false;
        String suffix = this.suffix.to(LEGACY);
        for (ScorePlayerTeam team: this.teams.values()) {
            team.setNameSuffix(suffix);
        }
        this.allowRecursion = true;
    }

    @Override
    public boolean allowFriendlyFire() {
        return this.allowFriendlyFire;
    }

    @Override
    public void setAllowFriendlyFire(boolean enabled) {
        this.allowFriendlyFire = enabled;
        this.updateAllowFriendlyFire();
    }

    private void updateAllowFriendlyFire() {
        this.allowRecursion = false;
        for (ScorePlayerTeam team: this.teams.values()) {
            team.setAllowFriendlyFire(this.allowFriendlyFire);
        }
        this.allowRecursion = true;
    }

    @Override
    public boolean canSeeFriendlyInvisibles() {
        return this.seeFriendlyInvisibles;
    }

    @Override
    public void setCanSeeFriendlyInvisibles(boolean enabled) {
        this.seeFriendlyInvisibles = enabled;
        this.updateCanSeeFriendlyInvisibles();
    }

    private void updateCanSeeFriendlyInvisibles() {
        this.allowRecursion = false;
        for (ScorePlayerTeam team: this.teams.values()) {
            team.setSeeFriendlyInvisiblesEnabled(this.seeFriendlyInvisibles);
        }
        this.allowRecursion = true;
    }

    @Override
    public Visibility getNameTagVisibility() {
        return this.nameTagVisibility;
    }

    @Override
    public void setNameTagVisibility(Visibility visibility) {
        this.nameTagVisibility = visibility;
        this.updateNameTagVisibility();
    }

    private void updateNameTagVisibility() {
        this.allowRecursion = false;
        net.minecraft.scoreboard.Team.EnumVisible visible = ((SpongeVisibility) this.nameTagVisibility).getHandle();
        for (ScorePlayerTeam team: this.teams.values()) {
            team.setNameTagVisibility(visible);
        }
        this.allowRecursion = true;
    }

    @Override
    public Visibility getDeathTextVisibility() {
        return this.deathMessageVisibility;
    }

    @Override
    public void setDeathTextVisibility(Visibility visibility) {
        this.deathMessageVisibility = visibility;
        this.updateDeathMessageVisibility();
    }

    private void updateDeathMessageVisibility() {
        this.allowRecursion = false;
        net.minecraft.scoreboard.Team.EnumVisible visible = ((SpongeVisibility) this.deathMessageVisibility).getHandle();
        for (ScorePlayerTeam team: this.teams.values()) {
            team.setDeathMessageVisibility(visible);
        }
        this.allowRecursion = true;
    }

    @Override
    public Set<Text> getMembers() {
        return new HashSet<>(this.members);
    }

    @Override
    public void addMember(Text member) {
        if (this.members.contains(member)) {
            return;
        }
        this.allowRecursion = false;
        this.members.add(member);
        for (Scoreboard scoreboard: this.getScoreboards()) {
            ((SpongeScoreboard) scoreboard).addMemberToTeam(member, this);
        }
        this.allowRecursion = true;
    }

    @Override
    public boolean removeMember(Text member) {
        this.allowRecursion = false;
        boolean present = this.members.remove(member);
        for (Scoreboard scoreboard: this.getScoreboards()) {
            ((SpongeScoreboard) scoreboard).removeMemberFromTeam(member);
        }
        this.allowRecursion = true;
        return present;
    }

    @Override
    public Set<Scoreboard> getScoreboards() {
        HashSet<Scoreboard> scoreboards = new HashSet<>();
        for (net.minecraft.scoreboard.Scoreboard scoreboard: this.teams.keySet()) {
            scoreboards.add(((IMixinScoreboard) scoreboard).getSpongeScoreboard());
        }
        return scoreboards;
    }

    @SuppressWarnings("deprecation")
    public void addToScoreboard(net.minecraft.scoreboard.Scoreboard scoreboard, ScorePlayerTeam team) {
        if (team == null) {
            team = scoreboard.createTeam(this.name);
            ((IMixinTeam) team).setSpongeTeam(this);
        }

        this.teams.put(scoreboard, team);

        team.setTeamName(this.displayName.to(LEGACY));
        team.setSeeFriendlyInvisiblesEnabled(this.seeFriendlyInvisibles);
        team.setAllowFriendlyFire(this.allowFriendlyFire);
        team.setChatFormat(((SpongeTextColor) this.color).getHandle());
        team.setNamePrefix(this.prefix.to(LEGACY));
        team.setNameSuffix(this.suffix.to(LEGACY));
        team.setNameTagVisibility(((SpongeVisibility) this.nameTagVisibility).getHandle());
        team.setDeathMessageVisibility(((SpongeVisibility) this.deathMessageVisibility).getHandle());

        for (Text member: this.members) {
            scoreboard.addPlayerToTeam(member.to(LEGACY), team.getRegisteredName());
        }

    }

    public void removeFromScoreboard(net.minecraft.scoreboard.Scoreboard scoreboard) {
        ScorePlayerTeam team = this.getTeam(scoreboard);
        if (team != null) {
            scoreboard.removeTeam(team);
        }
        this.teams.remove(scoreboard);
    }

    public ScorePlayerTeam getTeam(net.minecraft.scoreboard.Scoreboard scoreboard) {
        return this.teams.get(scoreboard);
    }

    public Map<net.minecraft.scoreboard.Scoreboard, ScorePlayerTeam> getTeams() {
        return this.teams;
    }
}
