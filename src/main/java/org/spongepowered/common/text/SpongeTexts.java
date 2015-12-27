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
package org.spongepowered.common.text;

import static org.spongepowered.api.text.serializer.TextSerializers.JSON;
import static org.spongepowered.api.text.serializer.TextSerializers.LEGACY;

import com.google.common.collect.Lists;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.IChatComponent;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.serializer.TextSerializers;
import org.spongepowered.common.interfaces.text.IMixinChatComponent;
import org.spongepowered.common.interfaces.text.IMixinText;

import java.util.List;
import java.util.Locale;

public final class SpongeTexts {

    public static final char COLOR_CHAR = '\u00A7';

    private SpongeTexts() {
    }

    public static Locale getDefaultLocale() { // TODO: Get this from the MC client?
        return Locale.getDefault();
    }

    public static IChatComponent toComponent(Text text) {
        return toComponent(text, getDefaultLocale());
    }

    public static IChatComponent toComponent(Text text, Locale locale) {
        return ((IMixinText) text).toComponent(locale);
    }

    public static Text toText(IChatComponent component) {
        return ((IMixinChatComponent) component).toText();
    }

    public static String toPlain(IChatComponent component) {
        return ((IMixinChatComponent) component).toPlain();
    }

    public static String toLegacy(IChatComponent component) {
        return toLegacy(component, COLOR_CHAR);
    }

    public static String toLegacy(IChatComponent component, char code) {
        return ((IMixinChatComponent) component).toLegacy(code);
    }

    private static String getLegacyFormatting(Text text) {
        return ((IMixinText) text).getLegacyFormatting();
    }

    public static Text fixActionBarFormatting(Text text) {
        Text result = text;
        if (!text.getChildren().isEmpty()) {
            Text.Builder fixed = text.builder().removeAll();

            for (Text child : text.getChildren()) {
                fixed.append(fixActionBarFormatting(child));
            }

            result = fixed.build();
        }

        return Text.builder(getLegacyFormatting(text)).append(result).build();
    }

    public static List<String> asJson(List<Text> list) {
        List<String> json = Lists.newArrayList();
        for (Text line : list) {
            json.add(line.to(JSON));
        }
        return json;
    }

    public static List<Text> fromJson(List<String> json) {
        List<Text> list = Lists.newArrayList();
        for (String line : json) {
           list.add(JSON.parse(line));
        }
        return list;
    }

    @SuppressWarnings("deprecation")
    public static List<Text> fromLegacy(NBTTagList legacy) {
        List<Text> list = Lists.newArrayList();
        for (int i = 0; i < legacy.tagCount(); i++) {
            list.add(LEGACY.parse(legacy.getStringTagAt(i)));
        }
        return list;
    }

    public static NBTTagList asLegacy(List<Text> list) {
        final NBTTagList legacy = new NBTTagList();
        for (Text line : list) {
            legacy.appendTag(new NBTTagString(line.to(LEGACY)));
        }
        return legacy;
    }
}
