package org.spongepowered.common.text.serializer;

import org.spongepowered.api.text.serializer.LegacyTextSerializer;
import org.spongepowered.api.text.serializer.TextSerializerFactory;

public class SpongeTextSerializerFactory implements TextSerializerFactory {

    @Override
    public LegacyTextSerializer getLegacyTextSerializer(char legacyChar) {
        return new SpongeLegacyTextSerializer(legacyChar);
    }

}
