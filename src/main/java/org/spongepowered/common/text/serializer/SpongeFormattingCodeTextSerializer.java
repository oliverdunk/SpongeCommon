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
package org.spongepowered.common.text.serializer;

import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.serializer.FormattingCodeTextSerializer;

import java.util.Locale;

public final class SpongeFormattingCodeTextSerializer implements FormattingCodeTextSerializer {

    private final char formattingChar;

    public SpongeFormattingCodeTextSerializer(char formattingChar) {
        this.formattingChar = formattingChar;
    }

    @Override
    public char getCharacter() {
        return this.formattingChar;
    }

    @Override
    public String serialize(Text text) {
        return LegacyTexts.serialize(text, this.formattingChar);
    }

    @Override
    public Text parse(String input) {
        return LegacyTexts.parse(input, this.formattingChar);
    }

    @Override
    public String stripCodes(String text) {
        return LegacyTexts.strip(text, this.formattingChar);
    }

    @Override
    public String replaceCodes(String text, char to) {
        return LegacyTexts.replace(text, this.formattingChar, to);
    }

}
