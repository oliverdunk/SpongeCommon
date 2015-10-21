/*
 * This file is part of SpongeCommon, licensed under the MIT License (MIT).
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
package org.spongepowered.common.item.inventory.name;

import net.minecraft.util.IChatComponent;
import org.spongepowered.api.text.translation.Translation;

import java.util.Locale;


/**
 * Implementation of {@link Translation} which isn't actually a localisable
 * string, basically allows for pluging literal text into interfaces which use
 * {@link Translation}. 
 */
public class Untranslatable implements Translation {
    
    /**
     * Literal text
     */
    private final String text;

    /**
     * Create a {@link Translation} wrapper for the specified source
     * 
     * @param source text source
     */
    public Untranslatable(IChatComponent source) {
        this.text = source.getUnformattedText();
    }

    /**
     * Create a {@link Translation} wrapper for the specified source
     * 
     * @param text text to wrap
     */
    public Untranslatable(String text) {
        this.text = text;
    }
    
    /* (non-Javadoc)
     * @see Translation#getId()
     */
    @Override
    public String getId() {
        return this.text;
    }

    /* (non-Javadoc)
     * @see Translation#get(java.util.Locale)
     */
    @Override
    public String get(Locale locale) {
        return this.text;
    }

    /* (non-Javadoc)
     * @see Translation#get(java.util.Locale, java.lang.Object[])
     */
    @Override
    public String get(Locale locale, Object... args) {
        return this.text;
    }
}
