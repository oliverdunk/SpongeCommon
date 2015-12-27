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
package org.spongepowered.common.data.processor.value.tileentity;

import static org.spongepowered.api.text.serializer.TextSerializers.LEGACY;

import net.minecraft.world.IWorldNameable;
import org.spongepowered.api.data.DataTransactionResult;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.value.ValueContainer;
import org.spongepowered.api.data.value.immutable.ImmutableValue;
import org.spongepowered.api.data.value.mutable.Value;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.serializer.TextSerializers;
import org.spongepowered.common.SpongeImpl;
import org.spongepowered.common.data.processor.common.AbstractSpongeValueProcessor;
import org.spongepowered.common.data.util.DataConstants;
import org.spongepowered.common.data.value.immutable.ImmutableSpongeValue;
import org.spongepowered.common.data.value.mutable.SpongeValue;
import org.spongepowered.common.interfaces.data.IMixinCustomNameable;

import java.util.Optional;

public class TileEntityDisplayNameValueProcessor extends AbstractSpongeValueProcessor<IWorldNameable, Text, Value<Text>> {

    public TileEntityDisplayNameValueProcessor() {
        super(IWorldNameable.class, Keys.DISPLAY_NAME);
    }

    @Override
    protected Value<Text> constructValue(Text defaultValue) {
        return new SpongeValue<>(Keys.DISPLAY_NAME, Text.of(), defaultValue);
    }

    @SuppressWarnings("deprecation")
    @Override
    protected boolean set(IWorldNameable container, Text value) {
        if (container instanceof IMixinCustomNameable) {
            final String legacy = value.to(LEGACY);
            try {
                ((IMixinCustomNameable) container).setCustomDisplayName(legacy);
            } catch (Exception e) {
                SpongeImpl.getLogger().error("There was an issue trying to replace the display name of an tile entity!", e);
            }
        }
        return true;
    }

    @SuppressWarnings("deprecation")
    @Override
    protected Optional<Text> getVal(IWorldNameable container) {
        if (container.hasCustomName()) {
            return Optional.of(TextSerializers.LEGACY.parse(container.getCommandSenderName()));
        }
        return Optional.empty();
    }

    @Override
    protected ImmutableValue<Text> constructImmutableValue(Text value) {
        return new ImmutableSpongeValue<>(Keys.DISPLAY_NAME, Text.of(), value);
    }

    @Override
    public DataTransactionResult removeFrom(ValueContainer<?> container) {
        return DataTransactionResult.failNoData();
    }
}
