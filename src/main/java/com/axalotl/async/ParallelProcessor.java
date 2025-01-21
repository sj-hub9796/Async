package com.axalotl.async;

import com.axalotl.async.config.AsyncConfig;
import com.axalotl.async.parallelised.ConcurrentCollections;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class ParallelProcessor {
    private static final Logger LOGGER = LogManager.getLogger();

    @Getter
    @Setter
    private static MinecraftServer server;

    public static final AtomicInteger currentEntities = new AtomicInteger();
    private static final AtomicInteger threadPoolID = new AtomicInteger();
    private static ExecutorService tickPool;
    private static final Queue<CompletableFuture<Void>> taskQueue = new ConcurrentLinkedQueue<>();
    private static final Set<UUID> blacklistedEntity = ConcurrentHashMap.newKeySet();
    private static final Map<String, Set<Thread>> mcThreadTracker = ConcurrentCollections.newHashMap();
    private static final Set<Class<?>> specialEntities = Set.of(
            FallingBlockEntity.class,
            Player.class,
            ServerPlayer.class
    );

    public static void setupThreadPool(int parallelism) {
        ForkJoinPool.ForkJoinWorkerThreadFactory threadFactory = pool -> {
            ForkJoinWorkerThread worker = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool);
            worker.setName("Async-Tick-Pool-Thread-" + threadPoolID.getAndIncrement());
            registerThread("Async-Tick", worker);
            worker.setDaemon(true);
            worker.setContextClassLoader(Async.class.getClassLoader());
            return worker;
        };

        tickPool = new ForkJoinPool(parallelism, threadFactory, (t, e) ->
                LOGGER.error("Uncaught exception in thread {}: {}", t.getName(), e), true);
        LOGGER.info("Initialized Pool with {} threads", parallelism);
    }

    public static void registerThread(String poolName, Thread thread) {
        mcThreadTracker.computeIfAbsent(poolName, key -> ConcurrentHashMap.newKeySet()).add(thread);
    }

    private static boolean isThreadInPool(Thread thread) {
        return mcThreadTracker.getOrDefault("Async-Tick", Set.of()).contains(thread);
    }

    public static boolean isServerExecutionThread() {
        return isThreadInPool(Thread.currentThread());
    }

    public static void callEntityTick(Consumer<Entity> tickConsumer, Entity entity) {
        if (shouldTickSynchronously(entity)) {
            tickSynchronously(tickConsumer, entity);
        } else {
            if (!tickPool.isShutdown() && !tickPool.isTerminated()) {
                CompletableFuture<Void> future = CompletableFuture.runAsync(() ->
                        performAsyncEntityTick(tickConsumer, entity), tickPool
                ).exceptionally(e -> {
                    logEntityError("Error in async tick, switching to synchronous", entity, e);
                    tickSynchronously(tickConsumer, entity);
                    blacklistedEntity.add(entity.getUUID());
                    return null;
                });
                taskQueue.add(future);
            } else {
                logEntityError("Rejected task due to ExecutorService shutdown", entity, null);
                tickSynchronously(tickConsumer, entity);
            }
        }
    }

    public static boolean shouldTickSynchronously(Entity entity) {
        return AsyncConfig.disabled ||
                blacklistedEntity.contains(entity.getUUID()) ||
                specialEntities.contains(entity.getClass()) ||
                AsyncConfig.synchronizedEntities.contains(EntityType.getKey(entity.getType())) ||
                isPortalTickRequired(entity) ||
                entity.hasExactlyOnePlayerPassenger() ||
                entity instanceof Projectile ||
                entity instanceof AbstractMinecart;
    }

    private static boolean isPortalTickRequired(Entity entity) {
        return entity.portalProcess != null && entity.portalProcess.isInsidePortalThisTick();
    }

    private static void tickSynchronously(Consumer<Entity> tickConsumer, Entity entity) {
        try {
            tickConsumer.accept(entity);
        } catch (Exception e) {
            logEntityError("Error during synchronous tick", entity, e);
        }
    }

    private static void performAsyncEntityTick(Consumer<Entity> tickConsumer, Entity entity) {
        currentEntities.incrementAndGet();
        try {
            tickConsumer.accept(entity);
        } finally {
            currentEntities.decrementAndGet();
        }
    }

    public static void postEntityTick() {
        if (!AsyncConfig.disabled) {
            try {
                List<CompletableFuture<Void>> futuresList = new ArrayList<>(taskQueue);
                taskQueue.clear();

                if (futuresList.isEmpty()) {
                    return;
                }

                CompletableFuture<Void> allTasks = CompletableFuture.allOf(
                        futuresList.toArray(new CompletableFuture[0])
                );

                allTasks.orTimeout(60, TimeUnit.SECONDS).exceptionally(ex -> {
                    LOGGER.error("Timeout during entity tick processing", ex);
                    server.stopServer();
                    return null;
                });

                server.getAllLevels().forEach(world -> {
                    world.getChunkSource().pollTask();
                    world.getChunkSource().mainThreadProcessor.managedBlock(allTasks::isDone);
                });

            } catch (CompletionException e) {
                LOGGER.error("Critical error during entity tick processing", e);
                server.stopServer();
            }
        }
    }

    public static void stop() {
        if (tickPool != null && !tickPool.isShutdown()) {
            tickPool.shutdown();
            try {
                if (!tickPool.awaitTermination(10, TimeUnit.SECONDS)) {
                    tickPool.shutdownNow();
                }
            } catch (InterruptedException e) {
                tickPool.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    private static void logEntityError(String message, Entity entity, Throwable e) {
        LOGGER.error("{} Entity Type: {}, UUID: {}", message, entity.getName(), entity.getUUID(), e);
    }
}