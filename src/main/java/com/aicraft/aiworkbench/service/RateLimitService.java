package com.aicraft.aiworkbench.service;

import com.aicraft.aiworkbench.config.PluginConfig;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

public final class RateLimitService {
    private final PluginConfig config;
    private final Semaphore semaphore;
    private final Map<UUID, Deque<Instant>> requests = new ConcurrentHashMap<>();

    public RateLimitService(PluginConfig config) {
        this.config = config;
        this.semaphore = new Semaphore(config.maxConcurrentGenerations());
    }

    public boolean tryAcquire(UUID playerId) {
        Deque<Instant> deque = requests.computeIfAbsent(playerId, ignored -> new ArrayDeque<>());
        Instant cutoff = Instant.now().minusSeconds(60);
        synchronized (deque) {
            while (!deque.isEmpty() && deque.peekFirst().isBefore(cutoff)) {
                deque.removeFirst();
            }
            if (deque.size() >= config.generationsPerMinute()) {
                return false;
            }
            if (!semaphore.tryAcquire()) {
                return false;
            }
            deque.addLast(Instant.now());
            return true;
        }
    }

    public void release() {
        semaphore.release();
    }
}
