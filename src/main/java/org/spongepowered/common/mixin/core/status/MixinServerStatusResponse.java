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
package org.spongepowered.common.mixin.core.status;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Throwables;
import net.minecraft.network.ServerStatusResponse;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;
import org.spongepowered.api.MinecraftVersion;
import org.spongepowered.api.event.server.ClientPingServerEvent;
import org.spongepowered.api.network.status.Favicon;
import org.spongepowered.api.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.common.network.status.SpongeFavicon;
import org.spongepowered.common.text.SpongeTexts;

import java.io.IOException;
import java.util.Optional;

import javax.annotation.Nullable;

@Mixin(ServerStatusResponse.class)
public abstract class MixinServerStatusResponse implements ClientPingServerEvent.Response {

    @Shadow private IChatComponent serverMotd;
    @Shadow private ServerStatusResponse.PlayerCountData playerCount;
    @Shadow private ServerStatusResponse.MinecraftProtocolVersionIdentifier protocolVersion;
    @Shadow private String favicon;

    private Text description;
    private ServerStatusResponse.PlayerCountData playerBackup;
    private Favicon faviconHandle;

    @Inject(method = "<init>", at = @At("RETURN"))
    public void onInit(CallbackInfo ci) {
        setServerDescription(null);
    }

    @Override
    public Text getDescription() {
        return this.description;
    }

    @Override
    public void setDescription(Text description) {
        this.description = checkNotNull(description, "description");
        this.serverMotd = SpongeTexts.toComponent(description); // TODO: Hope we get sent the locale
    }

    @Overwrite
    public void setServerDescription(@Nullable IChatComponent motd) {
        if (motd != null) {
            this.serverMotd = motd;
            this.description = SpongeTexts.toText(motd);
        } else {
            this.serverMotd = new ChatComponentText("");
            this.description = Text.of();
        }
    }

    @Override
    public Optional<Players> getPlayers() {
        return Optional.ofNullable((Players) this.playerCount);
    }

    @Override
    public void setHidePlayers(boolean hide) {
        if ((this.playerCount == null) != hide) {
            if (hide) {
                this.playerBackup = this.playerCount;
                this.playerCount = null;
            } else {
                this.playerCount = this.playerBackup;
                this.playerBackup = null;
            }
        }
    }

    @Override
    public MinecraftVersion getVersion() {
        return (MinecraftVersion) this.protocolVersion;
    }

    @Override
    public Optional<Favicon> getFavicon() {
        return Optional.ofNullable(this.faviconHandle);
    }

    @Override
    public void setFavicon(@Nullable Favicon favicon) {
        this.faviconHandle = favicon;
        if (this.faviconHandle != null) {
            this.favicon = ((SpongeFavicon) this.faviconHandle).getEncoded();
        } else {
            this.favicon = null;
        }
    }

    @Overwrite
    public void setFavicon(String faviconBlob) {
        if (faviconBlob == null) {
            this.favicon = null;
            this.faviconHandle = null;
        } else {
            try {
                this.faviconHandle = new SpongeFavicon(faviconBlob);
                this.favicon = faviconBlob;
            } catch (IOException e) {
                throw Throwables.propagate(e);
            }
        }
    }

}
