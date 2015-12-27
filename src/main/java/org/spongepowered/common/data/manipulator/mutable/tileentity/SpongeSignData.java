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
package org.spongepowered.common.data.manipulator.mutable.tileentity;

import com.google.common.collect.Lists;
import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.MemoryDataContainer;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.manipulator.immutable.tileentity.ImmutableSignData;
import org.spongepowered.api.data.manipulator.mutable.tileentity.SignData;
import org.spongepowered.api.data.value.mutable.ListValue;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.serializer.TextSerializers;
import org.spongepowered.api.util.annotation.NonnullByDefault;
import org.spongepowered.common.data.manipulator.immutable.tileentity.ImmutableSpongeSignData;
import org.spongepowered.common.data.manipulator.mutable.common.AbstractData;
import org.spongepowered.common.data.util.DataConstants;
import org.spongepowered.common.data.util.ImplementationRequiredForTest;
import org.spongepowered.common.data.value.mutable.SpongeListValue;

import java.util.List;
import java.util.stream.Collectors;

@ImplementationRequiredForTest
@NonnullByDefault
public class SpongeSignData extends AbstractData<SignData, ImmutableSignData> implements SignData {
    private final List<Text> lines;

    public SpongeSignData() {
        this(Lists.newArrayList(Text.of(), Text.of(), Text.of(), Text.of()));
    }

    public SpongeSignData(List<Text> lines) {
        super(SignData.class);
        registerGettersAndSetters();
        this.lines = lines;
    }

    @Override
    public int compareTo(SignData o) {
        return 0;
    }

    @Override
    public DataContainer toContainer() {
        List<String> jsonLines = Lists.newArrayListWithExpectedSize(4);
        jsonLines.addAll(this.lines.stream().map(TextSerializers.JSON::serialize).collect(Collectors.toList()));
        return new MemoryDataContainer().set(Keys.SIGN_LINES.getQuery(), jsonLines);
    }

    @Override
    public ListValue<Text> lines() {
        return new SpongeListValue<>(Keys.SIGN_LINES, Lists.newArrayList(this.lines));
    }

    @Override
    public SignData copy() {
        return new SpongeSignData(Lists.newArrayList(this.lines));
    }

    @Override
    public ImmutableSignData asImmutable() {
        return new ImmutableSpongeSignData(this.lines);
    }

    public List<Text> getLines() {
        return Lists.newArrayList(this.lines);
    }

    public void setLines(List<Text> lines) {
        for (int i = 0; i < 4; i++) {
            this.lines.set(i, lines.get(i));
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void registerGettersAndSetters() {
        registerFieldGetter(Keys.SIGN_LINES, SpongeSignData.this::getLines);
        registerFieldSetter(Keys.SIGN_LINES, SpongeSignData.this::setLines);
        registerKeyValue(Keys.SIGN_LINES, SpongeSignData.this::lines);
    }
}
