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
package org.spongepowered.common.registry;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.reflect.TypeToken;
import com.google.inject.Singleton;
import net.minecraft.world.WorldSettings;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializers;
import org.spongepowered.api.CatalogType;
import org.spongepowered.api.GameRegistry;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.data.value.ValueFactory;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.entity.ai.task.AITaskType;
import org.spongepowered.api.entity.ai.task.AbstractAITask;
import org.spongepowered.api.entity.living.Agent;
import org.spongepowered.api.entity.living.player.gamemode.GameMode;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.recipe.RecipeRegistry;
import org.spongepowered.api.network.status.Favicon;
import org.spongepowered.api.profile.GameProfile;
import org.spongepowered.api.resourcepack.ResourcePack;
import org.spongepowered.api.scoreboard.displayslot.DisplaySlot;
import org.spongepowered.api.statistic.BlockStatistic;
import org.spongepowered.api.statistic.EntityStatistic;
import org.spongepowered.api.statistic.ItemStatistic;
import org.spongepowered.api.statistic.Statistic;
import org.spongepowered.api.statistic.StatisticGroup;
import org.spongepowered.api.statistic.TeamStatistic;
import org.spongepowered.api.text.format.TextColor;
import org.spongepowered.api.text.translation.Translation;
import org.spongepowered.api.util.ResettableBuilder;
import org.spongepowered.api.util.rotation.Rotation;
import org.spongepowered.api.world.extent.ExtentBufferFactory;
import org.spongepowered.api.world.gamerule.DefaultGameRules;
import org.spongepowered.api.world.gen.WorldGeneratorModifier;
import org.spongepowered.common.SpongeImpl;
import org.spongepowered.common.config.CatalogTypeTypeSerializer;
import org.spongepowered.common.data.DataRegistrar;
import org.spongepowered.common.data.SpongeDataManager;
import org.spongepowered.common.data.property.SpongePropertyRegistry;
import org.spongepowered.common.data.value.SpongeValueFactory;
import org.spongepowered.common.network.status.SpongeFavicon;
import org.spongepowered.common.registry.type.block.RotationRegistryModule;
import org.spongepowered.common.registry.type.entity.AITaskTypeModule;
import org.spongepowered.common.registry.type.scoreboard.DisplaySlotRegistryModule;
import org.spongepowered.common.registry.type.world.GeneratorModifierRegistryModule;
import org.spongepowered.common.registry.util.RegistrationDependency;
import org.spongepowered.common.registry.util.RegistryModuleLoader;
import org.spongepowered.common.text.translation.SpongeTranslation;
import org.spongepowered.common.util.graph.DirectedGraph;
import org.spongepowered.common.util.graph.TopologicalOrder;
import org.spongepowered.common.world.extent.SpongeExtentBufferFactory;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

@Singleton
public class SpongeGameRegistry implements GameRegistry {

    static {
        TypeSerializers.getDefaultSerializers().registerType(TypeToken.of(CatalogType.class), new CatalogTypeTypeSerializer());
    }

    public final RegistrationPhase getPhase() {
        return this.phase;
    }

    private RegistrationPhase phase = RegistrationPhase.PRE_REGISTRY; // Needed for module phase registrations

    protected final Map<Class<? extends CatalogType>, CatalogRegistryModule<?>> catalogRegistryMap = new IdentityHashMap<>();
    private List<Class<? extends RegistryModule>> orderedModules = new ArrayList<>();
    final Map<Class<? extends RegistryModule>, RegistryModule> classMap = new IdentityHashMap<>();
    private final Map<Class<?>, Supplier<?>> builderSupplierMap = new IdentityHashMap<>();
    private final Set<RegistryModule> registryModules = new HashSet<>();

    public SpongeGameRegistry() {
    }

    public void preRegistryInit() {
        CommonModuleRegistry.getInstance().registerDefaultModules();
        final DirectedGraph<Class<? extends RegistryModule>> graph = new DirectedGraph<>();
        for (RegistryModule module : this.registryModules) {
            this.classMap.put(module.getClass(), module);
            addToGraph(module, graph);
        }
        // Now we need ot do the catalog ones
        for (CatalogRegistryModule<?> module : this.catalogRegistryMap.values()) {
            this.classMap.put(module.getClass(), module);
            addToGraph(module, graph);
        }

        this.orderedModules.addAll(TopologicalOrder.createOrderedLoad(graph));

        registerModulePhase();
        DataRegistrar.setupSerialization(SpongeImpl.getGame());
    }

    /**
     * Registers the {@link CatalogRegistryModule} for handling the registry stuffs.
     *
     * @param catalogClass
     * @param registryModule
     * @param <T>
     */
    public <T extends CatalogType> SpongeGameRegistry registerModule(Class<T> catalogClass, CatalogRegistryModule<T> registryModule) {
        checkArgument(!this.catalogRegistryMap.containsKey(catalogClass), "Already registered a registry module!");
        this.catalogRegistryMap.put(catalogClass, registryModule);
        return this;
    }

    public SpongeGameRegistry registerModule(RegistryModule module) {
        this.registryModules.add(checkNotNull(module));
        return this;
    }

    public <T> SpongeGameRegistry registerBuilderSupplier(Class<T> builderClass, Supplier<? extends T> supplier) {
        checkArgument(!this.builderSupplierMap.containsKey(builderClass), "Already registered a builder supplier!");
        this.builderSupplierMap.put(builderClass, supplier);
        return this;
    }

    @SuppressWarnings("unchecked")
    public <T extends CatalogType> CatalogRegistryModule<T> getRegistryModuleFor(Class<T> catalogClass) {
        checkNotNull(catalogClass);
        return (CatalogRegistryModule<T>) this.catalogRegistryMap.get(catalogClass);
    }

    @SuppressWarnings("unchecked")
    public <T extends CatalogType> void registerAdditionalType(Class<T> catalogClass, T extra) {
        CatalogRegistryModule<T> module = getRegistryModuleFor(catalogClass);
        if (module instanceof AdditionalCatalogRegistryModule) {
            ((AdditionalCatalogRegistryModule<T>) module).registerAdditionalCatalog(checkNotNull(extra));
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public <TUnknown, T extends CatalogType> boolean isAdditionalRegistered(Class<TUnknown> clazz, Class<T> catalogType) {
        CatalogRegistryModule<T> module = getRegistryModuleFor(catalogType);
        checkArgument(module instanceof ExtraClassCatalogRegistryModule);
        ExtraClassCatalogRegistryModule<T, ?> classModule = (ExtraClassCatalogRegistryModule<T, ?>) module;
        return classModule.hasRegistrationFor((Class) clazz);
    }

    public <TUnknown, T extends CatalogType> T getTranslated(Class<TUnknown> clazz, Class<T> catalogClazz) {
        CatalogRegistryModule<T> module = getRegistryModuleFor(catalogClazz);
        checkArgument(module instanceof ExtraClassCatalogRegistryModule);
        ExtraClassCatalogRegistryModule<T, TUnknown> classModule = (ExtraClassCatalogRegistryModule<T, TUnknown>) module;
        return classModule.getForClass(clazz);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends CatalogType> Optional<T> getType(Class<T> typeClass, String id) {
        CatalogRegistryModule<T> registryModule = (CatalogRegistryModule<T>) this.catalogRegistryMap.get(typeClass);
        if (registryModule == null) {
            return Optional.empty();
        } else {
            return registryModule.getById(id.toLowerCase());
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends CatalogType> Collection<T> getAllOf(Class<T> typeClass) {
        CatalogRegistryModule<T> registryModule = (CatalogRegistryModule<T>) this.catalogRegistryMap.get(typeClass);
        if (registryModule == null) {
            return Collections.emptyList();
        } else {
            return registryModule.getAll();
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends ResettableBuilder<?, ? super T>> T createBuilder(Class<T> builderClass) {
        checkNotNull(builderClass, "Builder class was null!");
        checkArgument(this.builderSupplierMap.containsKey(builderClass), "Could not find a Supplier for the provided class: " + builderClass.getCanonicalName());
        return (T) this.builderSupplierMap.get(builderClass).get();
    }

    @Override
    public List<String> getDefaultGameRules() {

        List<String> gameruleList = new ArrayList<>();
        for (Field f : DefaultGameRules.class.getFields()) {
            try {
                gameruleList.add((String) f.get(null));
            } catch (Exception e) {
                // Ignoring error
            }
        }
        return gameruleList;
    }

    public WorldSettings.GameType getGameType(GameMode mode) {
        // TODO: This is client-only
        //return WorldSettings.GameType.getByName(mode.getTranslation().getId());
        throw new UnsupportedOperationException();
    }

    @Override
    public void registerWorldGeneratorModifier(WorldGeneratorModifier modifier) {
        GeneratorModifierRegistryModule.getInstance().registerModifier(modifier);
    }

    @Override
    public AITaskType registerAITaskType(Object plugin, String id, String name, Class<? extends AbstractAITask<? extends Agent>> aiClass) {

        return AITaskTypeModule.getInstance().createAITaskType(plugin, id, name, aiClass);
    }

    @Override
    public Optional<Rotation> getRotationFromDegree(int degrees) {
        for (Rotation rotation : RotationRegistryModule.rotationMap.values()) {
            if (rotation.getAngle() == degrees) {
                return Optional.of(rotation);
            }
        }
        return Optional.empty();
    }

    @Override
    public GameProfile createGameProfile(UUID uuid, String name) {
        return (GameProfile) new com.mojang.authlib.GameProfile(uuid, name);
    }

    @Override
    public Favicon loadFavicon(String raw) throws IOException {
        return SpongeFavicon.load(raw);
    }

    @Override
    public Favicon loadFavicon(Path path) throws IOException {
        return SpongeFavicon.load(path);
    }

    @Override
    public Favicon loadFavicon(URL url) throws IOException {
        return SpongeFavicon.load(url);
    }

    @Override
    public Favicon loadFavicon(InputStream in) throws IOException {
        return SpongeFavicon.load(in);
    }

    @Override
    public Favicon loadFavicon(BufferedImage image) throws IOException {
        return SpongeFavicon.load(image);
    }

    @Override
    public RecipeRegistry getRecipeRegistry() {
        throw new UnsupportedOperationException(); // TODO
    }


    @Override
    public Optional<Translation> getTranslationById(String id) {
        return Optional.<Translation>of(new SpongeTranslation(id));
    }

    @Override
    public Optional<EntityStatistic> getEntityStatistic(StatisticGroup statisticGroup, EntityType entityType) {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public Optional<ItemStatistic> getItemStatistic(StatisticGroup statisticGroup, ItemType itemType) {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public Optional<BlockStatistic> getBlockStatistic(StatisticGroup statisticGroup, BlockType blockType) {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public Optional<TeamStatistic> getTeamStatistic(StatisticGroup statisticGroup, TextColor teamColor) {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public Collection<Statistic> getStatistics(StatisticGroup statisticGroup) {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public void registerStatistic(Statistic stat) {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public Optional<ResourcePack> getResourcePackById(String id) {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public Optional<DisplaySlot> getDisplaySlotForColor(TextColor color) {
        return Optional.ofNullable(DisplaySlotRegistryModule.getInstance().displaySlotMappings.get(color.getId()));
    }

    @Override
    public ExtentBufferFactory getExtentBufferFactory() {
        return SpongeExtentBufferFactory.INSTANCE;
    }

    @Override
    public ValueFactory getValueFactory() {
        return SpongeValueFactory.getInstance();
    }

    private void registerModulePhase() {
        for (Class<? extends RegistryModule> moduleClass : this.orderedModules) {
            if (!this.classMap.containsKey(moduleClass)) {
                throw new IllegalStateException("Something funky happened!");
            }
            final RegistryModule module = this.classMap.get(moduleClass);
            RegistryModuleLoader.tryModulePhaseRegistration(module);
        }
        registerAdditionalPhase();
    }

    private void registerAdditionalPhase() {
        for (Class<? extends RegistryModule> moduleClass : this.orderedModules) {
            final RegistryModule module = this.classMap.get(moduleClass);
            RegistryModuleLoader.tryAdditionalRegistration(module);
        }
    }

    private void addToGraph(RegistryModule module, DirectedGraph<Class<? extends RegistryModule>> graph) {
        graph.add(module.getClass());
        RegistrationDependency dependency = module.getClass().getAnnotation(RegistrationDependency.class);
        if (dependency != null) {
            for (Class<? extends RegistryModule> dependent : dependency.value()) {
                graph.addEdge(checkNotNull(module.getClass(), "Dependency class was null!"), dependent);
            }
        }
    }

    public void preInit() {
        this.phase = RegistrationPhase.PRE_INIT;
        registerModulePhase();
    }

    public void init() {
        this.phase = RegistrationPhase.INIT;
        registerModulePhase();
    }

    public void postInit() {
        this.phase = RegistrationPhase.POST_INIT;
        registerModulePhase();
        SpongePropertyRegistry.completeRegistration();
        SpongeDataManager.finalizeRegistration();
        this.phase = RegistrationPhase.LOADED;
    }

    public void registerAdditionals() {
        registerAdditionalPhase();
    }
}
