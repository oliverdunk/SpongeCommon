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
package org.spongepowered.common.service.pagination;

import static org.spongepowered.common.util.SpongeCommonTranslationHelper.t;

import org.spongepowered.api.service.pagination.PaginationCalculator;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.format.TextStyles;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Holds logic for an active pagination that is occurring.
 */
abstract class ActivePagination {

    private static final Text SLASH_TEXT = Text.of("/");
    private static final Text DIVIDER_TEXT = Text.of(" ");
    private final WeakReference<CommandSource> src;
    private final UUID id = UUID.randomUUID();
    private final Text nextPageText;
    private final Text prevPageText;
    private final Text title;
    private final Text header;
    private final Text footer;
    private int currentPage;
    private final int maxContentLinesPerPage;
    protected final PaginationCalculator<CommandSource> calc;
    private final String padding;

    public ActivePagination(CommandSource src, PaginationCalculator<CommandSource> calc, Text title,
            Text header, Text footer, String padding) {
        this.src = new WeakReference<>(src);
        this.calc = calc;
        this.title = title;
        this.header = header;
        this.footer = footer;
        this.padding = padding;
        this.nextPageText = t("»").builder()
                .color(TextColors.BLUE)
                .style(TextStyles.UNDERLINE)
                .onClick(TextActions.runCommand("/pagination " + this.id.toString() + " next")).build();
        this.prevPageText = t("«").builder()
                .color(TextColors.BLUE)
                .style(TextStyles.UNDERLINE)
                .onClick(TextActions.runCommand("/pagination " + this.id.toString() + " prev")).build();
        int maxContentLinesPerPage = calc.getLinesPerPage(src) - 1;
        if (title != null) {
            maxContentLinesPerPage -= calc.getLines(src, title);
        }
        if (header != null) {
            maxContentLinesPerPage -= calc.getLines(src, header);
        }
        if (footer != null) {
            maxContentLinesPerPage -= calc.getLines(src, footer);
        }
        this.maxContentLinesPerPage = maxContentLinesPerPage;

    }

    public UUID getId() {
        return this.id;
    }

    protected abstract Iterable<Text> getLines(int page) throws CommandException;

    protected abstract boolean hasPrevious(int page);

    protected abstract boolean hasNext(int page);

    protected abstract int getTotalPages();

    public void nextPage() throws CommandException {
        specificPage(this.currentPage + 1);
    }

    public void previousPage() throws CommandException {
        specificPage(this.currentPage - 1);
    }

    public void currentPage() throws CommandException {
        specificPage(this.currentPage);
    }

    protected int getCurrentPage() {
        return this.currentPage;
    }

    protected int getMaxContentLinesPerPage() {
        return this.maxContentLinesPerPage;
    }

    public void specificPage(int page) throws CommandException {
        CommandSource src = this.src.get();
        if (src == null) {
            throw new CommandException(t("Source for pagination %s is no longer active!", getId()));
        }
        this.currentPage = page;

        List<Text> toSend = new ArrayList<>();
        Text title = this.title;
        if (title != null) {
            toSend.add(title);
        }
        Text header = this.header;
        if (header != null) {
            toSend.add(header);
        }

        for (Text line : getLines(page)) {
            toSend.add(line);
        }

        Text footer = calculateFooter(page);
        if (footer != null) {
            toSend.add(this.calc.center(src, footer, this.padding));
        }
        if (this.footer != null) {
            toSend.add(this.footer);
        }
        src.sendMessages(toSend);
    }

    protected Text calculateFooter(int currentPage) {
        boolean hasPrevious = hasPrevious(currentPage);
        boolean hasNext = hasNext(currentPage);

        Text.Builder ret = Text.builder();
        if (hasPrevious) {
            ret.append(this.prevPageText).append(DIVIDER_TEXT);
        } else {
            ret.append(Text.of("«")).append(DIVIDER_TEXT);
        }
        boolean needsDiv = false;
        int totalPages = getTotalPages();
        if (totalPages > 1) {
            ret.append(Text.of(currentPage)).append(SLASH_TEXT).append(Text.of(totalPages));
            needsDiv = true;
        }
        if (hasNext) {
            if (needsDiv) {
                ret.append(DIVIDER_TEXT);
            }
            ret.append(this.nextPageText);
        } else {
            if (needsDiv) {
                ret.append(DIVIDER_TEXT);
            }
            ret.append(Text.of("»"));
        }
        if (this.title != null) {
            ret.color(this.title.getColor());
            ret.style(this.title.getStyle());
        }
        return ret.build();
    }
}
