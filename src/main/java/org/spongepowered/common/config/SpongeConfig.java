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
package org.spongepowered.common.config;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.reflect.TypeToken;
import ninja.leaping.configurate.ConfigurationOptions;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.commented.SimpleCommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.objectmapping.ObjectMapper;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializers;
import org.spongepowered.api.util.Functional;
import org.spongepowered.common.SpongeImpl;
import org.spongepowered.common.util.IpSet;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;

import javax.annotation.Nullable;

public class SpongeConfig<T extends SpongeConfig.ConfigBase> {

    public enum Type {
        GLOBAL(GlobalConfig.class),
        DIMENSION(DimensionConfig.class),
        WORLD(WorldConfig.class);

        private final Class<? extends ConfigBase> type;

        Type(Class<? extends ConfigBase> type) {
            this.type = type;
        }
    }

    public static final String CONFIG_ENABLED = "config-enabled";

    // DEBUG
    public static final String DEBUG_THREAD_CONTENTION_MONITORING = "thread-contention-monitoring";
    public static final String DEBUG_DUMP_CHUNKS_ON_DEADLOCK = "dump-chunks-on-deadlock";
    public static final String DEBUG_DUMP_HEAP_ON_DEADLOCK = "dump-heap-on-deadlock";
    public static final String DEBUG_DUMP_THREADS_ON_WARN = "dump-threads-on-warn";

    // ENTITY
    public static final String ENTITY_MAX_BOUNDING_BOX_SIZE = "max-bounding-box-size";
    public static final String ENTITY_MAX_SPEED = "max-speed";
    public static final String ENTITY_COLLISION_WARN_SIZE = "collision-warn-size";
    public static final String ENTITY_COUNT_WARN_SIZE = "count-warn-size";
    public static final String ENTITY_ITEM_DESPAWN_RATE = "item-despawn-rate";
    public static final String ENTITY_ACTIVATION_RANGE_CREATURE = "creature-activation-range";
    public static final String ENTITY_ACTIVATION_RANGE_MONSTER = "monster-activation-range";
    public static final String ENTITY_ACTIVATION_RANGE_AQUATIC = "aquatic-activation-range";
    public static final String ENTITY_ACTIVATION_RANGE_AMBIENT = "ambient-activation-range";
    public static final String ENTITY_ACTIVATION_RANGE_MISC = "misc-activation-range";
    public static final String ENTITY_HUMAN_PLAYER_LIST_REMOVE_DELAY = "human-player-list-remove-delay";
    public static final String ENTITY_PAINTING_RESPAWN_DELAY = "entity-painting-respawn-delay";

    // BUNGEECORD
    public static final String BUNGEECORD_IP_FORWARDING = "ip-forwarding";

    // EULA
    public static final String EULA_SHUTDOWN_SERVER = "shutdown-server";

    // GENERAL
    public static final String GENERAL_DISABLE_WARNINGS = "disable-warnings";
    public static final String GENERAL_CHUNK_LOAD_OVERRIDE = "chunk-load-override";

    // LOGGING
    public static final String LOGGING_BLOCK_BREAK = "block-break";
    public static final String LOGGING_BLOCK_MODIFY = "block-modify";
    public static final String LOGGING_BLOCK_PLACE = "block-place";
    public static final String LOGGING_BLOCK_POPULATE = "block-populate";
    public static final String LOGGING_BLOCK_TRACKING = "block-tracking";
    public static final String LOGGING_CHUNK_LOAD = "chunk-load";
    public static final String LOGGING_CHUNK_UNLOAD = "chunk-unload";
    public static final String LOGGING_ENTITY_DEATH = "entity-death";
    public static final String LOGGING_ENTITY_DESPAWN = "entity-despawn";
    public static final String LOGGING_ENTITY_COLLISION_CHECKS = "entity-collision-checks";
    public static final String LOGGING_ENTITY_SPAWN = "entity-spawn";
    public static final String LOGGING_ENTITY_SPEED_REMOVAL = "entity-speed-removal";
    public static final String LOGGING_EXPLOIT_SIGN_COMMAND_UPDATES = "exploit-sign-command-updates";
    public static final String LOGGING_EXPLOIT_ITEMSTACK_NAME_OVERFLOW = "exploit-itemstack-name-overflow";
    public static final String LOGGING_EXPLOIT_RESPAWN_INVISIBILITY = "exploit-respawn-invisibility";
    public static final String LOGGING_STACKTRACES = "log-stacktraces";

    // BLOCK TRACKING BLACKLIST
    public static final String BLOCK_TRACKING = "block-tracking";
    public static final String BLOCK_TRACKING_BLACKLIST = "block-blacklist";
    public static final String BLOCK_TRACKING_ENABLED = "enabled";

    // MODULES
    public static final String MODULE_ENTITY_ACTIVATION_RANGE = "entity-activation-range";
    public static final String MODULE_BUNGEECORD = "bungeecord";
    public static final String MODULE_SHUTDOWN_ON_EULA = "shutdown-on-eula";

    // WORLD
    public static final String WORLD_PVP_ENABLED = "pvp-enabled";
    public static final String WORLD_ENABLED = "world-enabled";
    public static final String WORLD_FLOWING_LAVA_DECAY = "flowing-lava-decay";
    public static final String WORLD_INFINITE_WATER_SOURCE = "infinite-water-source";
    public static final String WORLD_KEEP_SPAWN_LOADED = "keep-spawn-loaded";
    public static final String WORLD_LOAD_ON_STARTUP = "load-on-startup";
    public static final String WORLD_GEN_MODIFIERS = "world-generation-modifiers";

    private static final String HEADER = "1.0\n"
            + "\n"
            + "# If you need help with the configuration or have any questions related to Sponge,\n"
            + "# join us at the IRC or drop by our forums and leave a post.\n"
            + "\n"
            + "# IRC: #sponge @ irc.esper.net ( http://webchat.esper.net/?channel=sponge )\n"
            + "# Forums: https://forums.spongepowered.org/\n";

    private Type type;
    private HoconConfigurationLoader loader;
    private CommentedConfigurationNode root = SimpleCommentedConfigurationNode.root(ConfigurationOptions.defaults()
            .setHeader(HEADER));
    private ObjectMapper<T>.BoundInstance configMapper;
    private T configBase;
    private String modId;

    @SuppressWarnings({"unchecked", "rawtypes"})
    public SpongeConfig(Type type, Path path, String modId) {

        this.type = type;
        this.modId = modId;

        try {
            Files.createDirectories(path.getParent());
            if (Files.notExists(path)) {
                Files.createFile(path);
            }

            this.loader = HoconConfigurationLoader.builder().setPath(path).build();
            this.configMapper = (ObjectMapper.BoundInstance) ObjectMapper.forClass(this.type.type).bindToNew();

            reload();
            save();
        } catch (Exception e) {
            SpongeImpl.getLogger().error("Failed to initialize configuration", e);
        }
    }

    public T getConfig() {
        return this.configBase;
    }

    public void save() {
        try {
            this.configMapper.serialize(this.root.getNode(this.modId));
            this.loader.save(this.root);
        } catch (IOException | ObjectMappingException e) {
            SpongeImpl.getLogger().error("Failed to save configuration", e);
        }
    }

    public void reload() {
        try {
            this.root = this.loader.load(ConfigurationOptions.defaults()
                    .setSerializers(
                            TypeSerializers.getDefaultSerializers().newChild().registerType(TypeToken.of(IpSet.class), new IpSet.IpSetSerializer()))
                    .setHeader(HEADER));
            this.configBase = this.configMapper.populate(this.root.getNode(this.modId));
        } catch (Exception e) {
            SpongeImpl.getLogger().error("Failed to load configuration", e);
        }
    }

    public CompletableFuture<CommentedConfigurationNode> updateSetting(String key, Object value) {
        return Functional.asyncFailableFuture(() -> {
            CommentedConfigurationNode upd = getSetting(key);
            upd.setValue(value);
            this.configBase = this.configMapper.populate(this.root.getNode(this.modId));
            this.loader.save(this.root);
            return upd;
        }, ForkJoinPool.commonPool());
    }

    public CommentedConfigurationNode getRootNode() {
        return this.root.getNode(this.modId);
    }

    public CommentedConfigurationNode getSetting(String key) {
        if (key.equalsIgnoreCase(SpongeConfig.CONFIG_ENABLED)) {
            return getRootNode().getNode(key);
        } else if (!key.contains(".") || key.indexOf('.') == key.length() - 1) {
            return null;
        } else {
            String category = key.substring(0, key.indexOf('.'));
            String prop = key.substring(key.indexOf('.') + 1);
            return getRootNode().getNode(category).getNode(prop);
        }
    }

    public Type getType() {
        return this.type;
    }

    public static class GlobalConfig extends ConfigBase {

        @Setting(comment = "Configuration options related to the Sql service, including connection aliases etc")
        private SqlCategory sql = new SqlCategory();

        @Setting
        private CommandsCategory commands = new CommandsCategory();

        @Setting(value = "modules")
        private ModuleCategory mixins = new ModuleCategory();

        @Setting("ip-sets")
        private Map<String, List<IpSet>> ipSets = new HashMap<>();

        @Setting(value = MODULE_BUNGEECORD)
        private BungeeCordCategory bungeeCord = new BungeeCordCategory();

        @Setting(MODULE_SHUTDOWN_ON_EULA)
        private ShutdownOnEulaCategory eulaShutdown = new ShutdownOnEulaCategory();

        public BungeeCordCategory getBungeeCord() {
            return this.bungeeCord;
        }

        public ShutdownOnEulaCategory getEulaShutdown() {
            return this.eulaShutdown;
        }

        public SqlCategory getSql() {
            return this.sql;
        }

        public CommandsCategory getCommands() {
            return this.commands;
        }

        public ModuleCategory getModules() {
            return this.mixins;
        }

        public Map<String, Predicate<InetAddress>> getIpSets() {
            return ImmutableMap.copyOf(Maps.transformValues(this.ipSets, new Function<List<IpSet>, Predicate<InetAddress>>() {
                @Nullable
                @Override
                public Predicate<InetAddress> apply(List<IpSet> input) {
                    return Predicates.and(input);
                }
            }));
        }

        public Predicate<InetAddress> getIpSet(String name) {
            return this.ipSets.containsKey(name) ? Predicates.and(this.ipSets.get(name)) : null;
        }
    }

    public static class DimensionConfig extends ConfigBase {

        @Setting(
                value = CONFIG_ENABLED,
                comment = "Enabling config will override Global.")
        protected boolean configEnabled = true;

        public DimensionConfig() {
            this.configEnabled = false;
        }

        public boolean isConfigEnabled() {
            return this.configEnabled;
        }

        public void setConfigEnabled(boolean configEnabled) {
            this.configEnabled = configEnabled;
        }
    }

    public static class WorldConfig extends ConfigBase {

        @Setting(
                value = CONFIG_ENABLED,
                comment = "Enabling config will override Dimension and Global.")
        protected boolean configEnabled = true;

        @Setting(value = WORLD_GEN_MODIFIERS, comment = "WorldGenerationModifiers to apply to the world")
        private List<String> worldModifiers = new ArrayList<>();

        public WorldConfig() {
            this.configEnabled = false;
        }

        public boolean isConfigEnabled() {
            return this.configEnabled;
        }

        public void setConfigEnabled(boolean configEnabled) {
            this.configEnabled = configEnabled;
        }

        public List<String> getWorldGenModifiers() {
            return this.worldModifiers;
        }
    }

    public static class ConfigBase {

        @Setting(value = BLOCK_TRACKING)
        private BlockTrackingCategory blockTracking = new BlockTrackingCategory();
        @Setting
        private DebugCategory debug = new DebugCategory();
        @Setting
        private EntityCategory entity = new EntityCategory();
        @Setting(value = MODULE_ENTITY_ACTIVATION_RANGE)
        private EntityActivationRangeCategory entityActivationRange = new EntityActivationRangeCategory();
        @Setting
        private GeneralCategory general = new GeneralCategory();
        @Setting
        private LoggingCategory logging = new LoggingCategory();
        @Setting
        private WorldCategory world = new WorldCategory();
        @Setting
        private TimingsCategory timings = new TimingsCategory();

        public BlockTrackingCategory getBlockTracking() {
            return this.blockTracking;
        }

        public DebugCategory getDebug() {
            return this.debug;
        }

        public EntityCategory getEntity() {
            return this.entity;
        }

        public EntityActivationRangeCategory getEntityActivationRange() {
            return this.entityActivationRange;
        }

        public GeneralCategory getGeneral() {
            return this.general;
        }

        public LoggingCategory getLogging() {
            return this.logging;
        }

        public WorldCategory getWorld() {
            return this.world;
        }

        public TimingsCategory getTimings() {
            return this.timings;
        }
    }

    @ConfigSerializable
    public static class SqlCategory extends Category {
        @Setting(comment = "Aliases for SQL connections, in the format jdbc:protocol://[username[:password]@]host/database")
        private Map<String, String> aliases = new HashMap<>();

        public Map<String, String> getAliases() {
            return this.aliases;
        }
    }

    @ConfigSerializable
    public static class CommandsCategory extends Category {
        @Setting(comment = "A mapping from unqualified command alias to plugin id of the plugin that should handle a certain command")
        private Map<String, String> aliases = new HashMap<>();

        public Map<String, String> getAliases() {
            return this.aliases;
        }
    }

    @ConfigSerializable
    public static class DebugCategory extends Category {

        @Setting(value = DEBUG_THREAD_CONTENTION_MONITORING, comment = "Enable Java's thread contention monitoring for thread dumps")
        private boolean enableThreadContentionMonitoring = false;
        @Setting(value = DEBUG_DUMP_CHUNKS_ON_DEADLOCK, comment = "Dump chunks in the event of a deadlock")
        private boolean dumpChunksOnDeadlock = false;
        @Setting(value = DEBUG_DUMP_HEAP_ON_DEADLOCK, comment = "Dump the heap in the event of a deadlock")
        private boolean dumpHeapOnDeadlock = false;
        @Setting(value = DEBUG_DUMP_THREADS_ON_WARN, comment = "Dump the server thread on deadlock warning")
        private boolean dumpThreadsOnWarn = false;

        public boolean isEnableThreadContentionMonitoring() {
            return this.enableThreadContentionMonitoring;
        }

        public void setEnableThreadContentionMonitoring(boolean enableThreadContentionMonitoring) {
            this.enableThreadContentionMonitoring = enableThreadContentionMonitoring;
        }

        public boolean dumpChunksOnDeadlock() {
            return this.dumpChunksOnDeadlock;
        }

        public void setDumpChunksOnDeadlock(boolean dumpChunksOnDeadlock) {
            this.dumpChunksOnDeadlock = dumpChunksOnDeadlock;
        }

        public boolean dumpHeapOnDeadlock() {
            return this.dumpHeapOnDeadlock;
        }

        public void setDumpHeapOnDeadlock(boolean dumpHeapOnDeadlock) {
            this.dumpHeapOnDeadlock = dumpHeapOnDeadlock;
        }

        public boolean dumpThreadsOnWarn() {
            return this.dumpThreadsOnWarn;
        }

        public void setDumpThreadsOnWarn(boolean dumpThreadsOnWarn) {
            this.dumpThreadsOnWarn = dumpThreadsOnWarn;
        }
    }

    @ConfigSerializable
    public static class GeneralCategory extends Category {

        @Setting(value = GENERAL_DISABLE_WARNINGS, comment = "Disable warning messages to server admins")
        private boolean disableWarnings = false;
        @Setting(value = GENERAL_CHUNK_LOAD_OVERRIDE,
                comment = "Forces Chunk Loading on provide requests (speedup for mods that don't check if a chunk is loaded)")
        private boolean chunkLoadOverride = false;

        public boolean disableWarnings() {
            return this.disableWarnings;
        }

        public void setDisableWarnings(boolean disableWarnings) {
            this.disableWarnings = disableWarnings;
        }

        public boolean chunkLoadOverride() {
            return this.chunkLoadOverride;
        }

        public void setChunkLoadOverride(boolean chunkLoadOverride) {
            this.chunkLoadOverride = chunkLoadOverride;
        }
    }

    @ConfigSerializable
    public static class EntityCategory extends Category {

        @Setting(value = ENTITY_MAX_BOUNDING_BOX_SIZE, comment = "Max size of an entity's bounding box before removing it. Set to 0 to disable")
        private int maxBoundingBoxSize = 1000;
        @Setting(value = SpongeConfig.ENTITY_MAX_SPEED, comment = "Square of the max speed of an entity before removing it. Set to 0 to disable")
        private int maxSpeed = 100;
        @Setting(value = ENTITY_COLLISION_WARN_SIZE,
                comment = "Number of colliding entities in one spot before logging a warning. Set to 0 to disable")
        private int maxCollisionSize = 200;
        @Setting(value = ENTITY_COUNT_WARN_SIZE,
                comment = "Number of entities in one dimension before logging a warning. Set to 0 to disable")
        private int maxCountWarnSize = 0;
        @Setting(value = ENTITY_ITEM_DESPAWN_RATE, comment = "Controls the time in ticks for when an item despawns.")
        private int itemDespawnRate = 6000;
        @Setting(value = ENTITY_HUMAN_PLAYER_LIST_REMOVE_DELAY,
                comment = "Number of ticks before the fake player entry of a human is removed from the tab list (range of 0 to 100 ticks).")
        private int humanPlayerListRemoveDelay = 10;
        @Setting(value = ENTITY_PAINTING_RESPAWN_DELAY,
                comment = "Number of ticks before a painting is respawned on clients when their art is changed")
        private int paintingRespawnDelaly = 2;

        public int getMaxBoundingBoxSize() {
            return this.maxBoundingBoxSize;
        }

        public void setMaxBoundingBoxSize(int maxBoundingBoxSize) {
            this.maxBoundingBoxSize = maxBoundingBoxSize;
        }

        public int getMaxSpeed() {
            return this.maxSpeed;
        }

        public void setMaxSpeed(int maxSpeed) {
            this.maxSpeed = maxSpeed;
        }

        public int getMaxCollisionSize() {
            return this.maxCollisionSize;
        }

        public void setMaxCollisionSize(int maxCollisionSize) {
            this.maxCollisionSize = maxCollisionSize;
        }

        public int getMaxCountWarnSize() {
            return this.maxCountWarnSize;
        }

        public void setMaxCountWarnSize(int maxCountWarnSize) {
            this.maxCountWarnSize = maxCountWarnSize;
        }

        public int getItemDespawnRate() {
            return this.itemDespawnRate;
        }

        public void setItemDespawnRate(int itemDespawnRate) {
            this.itemDespawnRate = itemDespawnRate;
        }

        public int getHumanPlayerListRemoveDelay() {
            return this.humanPlayerListRemoveDelay;
        }

        public void setHumanPlayerListRemoveDelay(int delay) {
            this.humanPlayerListRemoveDelay = Math.max(0, Math.min(delay, 100));
        }

        public int getPaintingRespawnDelaly() {
            return this.paintingRespawnDelaly;
        }

        public void setPaintingRespawnDelaly(int paintingRespawnDelaly) {
            this.paintingRespawnDelaly = Math.min(paintingRespawnDelaly, 1);
        }
    }

    @ConfigSerializable
    public static class BungeeCordCategory extends Category {

        @Setting(value = BUNGEECORD_IP_FORWARDING,
                comment = "If enabled, allows BungeeCord to forward IP address, UUID, and Game Profile to this server")
        private boolean ipForwarding = false;

        public boolean getIpForwarding() {
            return this.ipForwarding;
        }
    }

    @ConfigSerializable
    public static class ShutdownOnEulaCategory extends Category {

        @Setting(value = EULA_SHUTDOWN_SERVER, comment = "If enabled, shut down the server if the EULA has not been accepted")
        private boolean shutdownServer = true;

        public boolean shouldShutdownServer() {
            return this.shutdownServer;
        }
    }

    @ConfigSerializable
    public static class EntityActivationRangeCategory extends Category {

        @Setting(value = ENTITY_ACTIVATION_RANGE_CREATURE)
        private int creatureActivationRange = 32;
        @Setting(value = ENTITY_ACTIVATION_RANGE_MONSTER)
        private int monsterActivationRange = 32;
        @Setting(value = ENTITY_ACTIVATION_RANGE_AQUATIC)
        private int aquaticActivationRange = 32;
        @Setting(value = ENTITY_ACTIVATION_RANGE_AMBIENT)
        private int ambientActivationRange = 32;
        @Setting(value = ENTITY_ACTIVATION_RANGE_MISC)
        private int miscActivationRange = 16;

        public int getCreatureActivationRange() {
            return this.creatureActivationRange;
        }

        public void setCreatureActivationRange(int creatureActivationRange) {
            this.creatureActivationRange = creatureActivationRange;
        }

        public int getMonsterActivationRange() {
            return this.monsterActivationRange;
        }

        public void setMonsterActivationRange(int monsterActivationRange) {
            this.monsterActivationRange = monsterActivationRange;
        }

        public int getAquaticActivationRange() {
            return this.aquaticActivationRange;
        }

        public void setAquaticActivationRange(int aquaticActivationRange) {
            this.aquaticActivationRange = aquaticActivationRange;
        }

        public int getAmbientActivationRange() {
            return this.ambientActivationRange;
        }

        public void setAmbientActivationRange(int ambientActivationRange) {
            this.ambientActivationRange = ambientActivationRange;
        }

        public int getMiscActivationRange() {
            return this.miscActivationRange;
        }

        public void setMiscActivationRange(int miscActivationRange) {
            this.miscActivationRange = miscActivationRange;
        }
    }

    @ConfigSerializable
    public static class LoggingCategory extends Category {

        @Setting(value = LOGGING_BLOCK_BREAK, comment = "Log when blocks are broken")
        private boolean blockBreakLogging = false;
        @Setting(value = LOGGING_BLOCK_MODIFY, comment = "Log when blocks are modified")
        private boolean blockModifyLogging = false;
        @Setting(value = LOGGING_BLOCK_PLACE, comment = "Log when blocks are placed")
        private boolean blockPlaceLogging = false;
        @Setting(value = LOGGING_BLOCK_POPULATE, comment = "Log when blocks are populated in a chunk")
        private boolean blockPopulateLogging = false;
        @Setting(value = LOGGING_BLOCK_TRACKING, comment = "Log when blocks are placed by players and tracked")
        private boolean blockTrackLogging = false;
        @Setting(value = LOGGING_CHUNK_LOAD, comment = "Log when chunks are loaded")
        private boolean chunkLoadLogging = false;
        @Setting(value = LOGGING_CHUNK_UNLOAD, comment = "Log when chunks are unloaded")
        private boolean chunkUnloadLogging = false;
        @Setting(value = LOGGING_ENTITY_SPAWN, comment = "Log when living entities are spawned")
        private boolean entitySpawnLogging = false;
        @Setting(value = LOGGING_ENTITY_DESPAWN, comment = "Log when living entities are despawned")
        private boolean entityDespawnLogging = false;
        @Setting(value = LOGGING_ENTITY_DEATH, comment = "Log when living entities are destroyed")
        private boolean entityDeathLogging = false;
        @Setting(value = LOGGING_EXPLOIT_SIGN_COMMAND_UPDATES, comment = "Log when server receives exploited packet to update a sign containing commands from player with no permission.")
        public boolean logExploitSignCommandUpdates = false;
        @Setting(value = LOGGING_EXPLOIT_ITEMSTACK_NAME_OVERFLOW, comment = "Log when server receives exploited packet with itemstack name exceeding string limit.")
        public boolean logExploitItemStackNameOverflow = false;
        @Setting(value = LOGGING_EXPLOIT_RESPAWN_INVISIBILITY, comment = "Log when player attempts to respawn invisible to surrounding players.")
        public boolean logExploitRespawnInvisibility = false;
        @Setting(value = LOGGING_STACKTRACES, comment = "Add stack traces to dev logging")
        private boolean logWithStackTraces = false;
        @Setting(value = LOGGING_ENTITY_COLLISION_CHECKS, comment = "Whether to log entity collision/count checks")
        private boolean logEntityCollisionChecks = false;
        @Setting(value = LOGGING_ENTITY_SPEED_REMOVAL, comment = "Whether to log entity removals due to speed")
        private boolean logEntitySpeedRemoval = false;

        public boolean blockBreakLogging() {
            return this.blockBreakLogging;
        }

        public void setBlockBreakLogging(boolean flag) {
            this.blockBreakLogging = flag;
        }

        public boolean blockModifyLogging() {
            return this.blockModifyLogging;
        }

        public void setBlockModifyLogging(boolean flag) {
            this.blockModifyLogging = flag;
        }

        public boolean blockPlaceLogging() {
            return this.blockPlaceLogging;
        }

        public void setBlockPlaceLogging(boolean flag) {
            this.blockPlaceLogging = flag;
        }

        public boolean blockPopulateLogging() {
            return this.blockPopulateLogging;
        }

        public void setBlockPopulateLogging(boolean flag) {
            this.blockPopulateLogging = flag;
        }

        public boolean blockTrackLogging() {
            return this.blockTrackLogging;
        }

        public void setBlockTrackLogging(boolean flag) {
            this.blockTrackLogging = flag;
        }

        public boolean chunkLoadLogging() {
            return this.chunkLoadLogging;
        }

        public void setChunkLoadLogging(boolean flag) {
            this.chunkLoadLogging = flag;
        }

        public boolean chunkUnloadLogging() {
            return this.chunkUnloadLogging;
        }

        public void setChunkUnloadLogging(boolean flag) {
            this.chunkUnloadLogging = flag;
        }

        public boolean entitySpawnLogging() {
            return this.entitySpawnLogging;
        }

        public void setEntitySpawnLogging(boolean flag) {
            this.entitySpawnLogging = flag;
        }

        public boolean entityDespawnLogging() {
            return this.entityDespawnLogging;
        }

        public void setEntityDespawnLogging(boolean flag) {
            this.entityDespawnLogging = flag;
        }

        public boolean entityDeathLogging() {
            return this.entityDeathLogging;
        }

        public void setEntityDeathLogging(boolean flag) {
            this.entityDeathLogging = flag;
        }

        public boolean logWithStackTraces() {
            return this.logWithStackTraces;
        }

        public void setLogWithStackTraces(boolean flag) {
            this.logWithStackTraces = flag;
        }

        public boolean logEntityCollisionChecks() {
            return this.logEntityCollisionChecks;
        }

        public void setLogEntityCollisionChecks(boolean flag) {
            this.logEntityCollisionChecks = flag;
        }

        public boolean logEntitySpeedRemoval() {
            return this.logEntitySpeedRemoval;
        }

        public void setLogEntitySpeedRemoval(boolean flag) {
            this.logEntitySpeedRemoval = flag;
        }
    }

    @ConfigSerializable
    public static class ModuleCategory extends Category {

        @Setting(value = MODULE_BUNGEECORD)
        private boolean pluginBungeeCord = false;

        @Setting(value = MODULE_ENTITY_ACTIVATION_RANGE)
        private boolean pluginEntityActivation = true;

        @Setting("timings")
        private boolean pluginTimings = true;

        public boolean usePluginBungeeCord() {
            return this.pluginBungeeCord;
        }

        public void setPluginBungeeCord(boolean state) {
            this.pluginBungeeCord = state;
        }

        public boolean usePluginEntityActivation() {
            return this.pluginEntityActivation;
        }

        public void setPluginEntityActivation(boolean state) {
            this.pluginEntityActivation = state;
        }

        public boolean usePluginTimings() {
            return this.pluginTimings;
        }

        public void setPluginTimings(boolean state) {
            this.pluginTimings = state;
        }
    }

    @ConfigSerializable
    public static class BlockTrackingCategory extends Category {

        @Setting(value = BLOCK_TRACKING_ENABLED, comment = "If enabled, adds player tracking support for block positions. Note: This should only be disabled if you do not care who caused a block to change.")
        private boolean enabled = true;

        @Setting(value = BLOCK_TRACKING_BLACKLIST, comment = "Add block ids you wish to blacklist for player block placement tracking.")
        private List<String> blockBlacklist = new ArrayList<>();

        public boolean isEnabled() {
            return this.enabled;
        }

        public void setEnabled(boolean flag) {
            this.enabled = flag;
        }

        public List<String> getBlockBlacklist() {
            return this.blockBlacklist;
        }
    }

    @ConfigSerializable
    public static class WorldCategory extends Category {

        @Setting(value = WORLD_INFINITE_WATER_SOURCE, comment = "Vanilla water source behavior - is infinite")
        private boolean infiniteWaterSource = false;

        @Setting(value = WORLD_FLOWING_LAVA_DECAY, comment = "Lava behaves like vanilla water when source block is removed")
        private boolean flowingLavaDecay = false;

        @Setting(value = WORLD_ENABLED, comment = "Enable if this world should be allowed to load.")
        protected boolean worldEnabled = true;

        @Setting(value = WORLD_LOAD_ON_STARTUP, comment = "Enable if this world should load on startup.")
        protected boolean loadOnStartup = true;

        @Setting(value = WORLD_KEEP_SPAWN_LOADED, comment = "Enable if this world's spawn should remain loaded with no players.")
        protected boolean keepSpawnLoaded = true;

        @Setting(value = WORLD_PVP_ENABLED, comment = "Enable if this world allows PVP combat.")
        protected boolean pvpEnabled = true;

        @Setting(value = WORLD_GEN_MODIFIERS, comment = "WorldGenerationModifiers to apply to the world")
        private List<String> worldModifiers = new ArrayList<>();

        public boolean hasInfiniteWaterSource() {
            return this.infiniteWaterSource;
        }

        public void setInfiniteWaterSource(boolean infiniteWaterSource) {
            this.infiniteWaterSource = infiniteWaterSource;
        }

        public boolean hasFlowingLavaDecay() {
            return this.flowingLavaDecay;
        }

        public void setFlowingLavaDecay(boolean flowingLavaDecay) {
            this.flowingLavaDecay = flowingLavaDecay;
        }

        public boolean isWorldEnabled() {
            return this.worldEnabled;
        }

        public void setWorldEnabled(boolean enabled) {
            this.worldEnabled = enabled;
        }

        public boolean loadOnStartup() {
            return this.loadOnStartup;
        }

        public void setLoadOnStartup(boolean state) {
            this.loadOnStartup = state;
        }

        public boolean getKeepSpawnLoaded() {
            return this.keepSpawnLoaded;
        }

        public void setKeepSpawnLoaded(boolean loaded) {
            this.keepSpawnLoaded = loaded;
        }

        public boolean getPVPEnabled() {
            return this.pvpEnabled;
        }

        public void setPVPEnabled(boolean allow) {
            this.pvpEnabled = allow;
        }

        public List<String> getWorldGenModifiers() {
            return this.worldModifiers;
        }
    }

    @ConfigSerializable
    public static class TimingsCategory extends Category {

        @Setting
        private boolean verbose = false;

        @Setting
        private boolean enabled = true;

        @Setting("server-name-privacy")
        private boolean serverNamePrivacy = false;

        @Setting("hidden-config-entries")
        private List<String> hiddenConfigEntries = Lists.newArrayList("sponge.sql");

        @Setting("history-interval")
        private int historyInterval = 300;

        @Setting("history-length")
        private int historyLength = 3600;

        public boolean isVerbose() {
            return this.verbose;
        }

        public void setVerbose(boolean verbose) {
            this.verbose = verbose;
        }

        public boolean isEnabled() {
            return this.enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getHistoryInterval() {
            return this.historyInterval;
        }

        public void setHistoryInterval(int historyInterval) {
            this.historyInterval = historyInterval;
        }

        public int getHistoryLength() {
            return this.historyLength;
        }

        public void setHistoryLength(int historyLength) {
            this.historyLength = historyLength;
        }

    }

    @ConfigSerializable
    private static class Category {
    }
}
