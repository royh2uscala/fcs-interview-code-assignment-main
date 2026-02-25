package com.fulfilment.application.monolith.warehouses.domain.usecases;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

@ApplicationScoped
public class WarehouseMutationLockManager {

  private final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

  public void withLocks(List<String> keys, Runnable action) {
    withLocks(
        keys,
        () -> {
          action.run();
          return null;
        });
  }

  public <T> T withLocks(List<String> keys, Supplier<T> action) {
    List<String> normalized =
        keys.stream()
            .filter(Objects::nonNull)
            .map(String::trim)
            .filter(value -> !value.isBlank())
            .distinct()
            .sorted(Comparator.naturalOrder())
            .toList();

    List<ReentrantLock> acquired = new ArrayList<>(normalized.size());
    for (String key : normalized) {
      ReentrantLock lock = locks.computeIfAbsent(key, ignored -> new ReentrantLock());
      lock.lock();
      acquired.add(lock);
    }

    try {
      return action.get();
    } finally {
      for (int i = acquired.size() - 1; i >= 0; i--) {
        ReentrantLock lock = acquired.get(i);
        lock.unlock();
      }
      for (String key : normalized) {
        ReentrantLock lock = locks.get(key);
        if (lock != null && !lock.isLocked() && !lock.hasQueuedThreads()) {
          locks.remove(key, lock);
        }
      }
    }
  }
}
