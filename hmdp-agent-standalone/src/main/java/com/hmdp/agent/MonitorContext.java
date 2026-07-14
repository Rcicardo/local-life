package com.hmdp.agent;

public class MonitorContext {
    private static final ThreadLocal<Boolean> inCacheClientFlow = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<Boolean> dbAccessed = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<Boolean> redisAccessed = ThreadLocal.withInitial(() -> false);

    public static void enterCacheClientFlow() {
        inCacheClientFlow.set(true);
    }

    public static boolean isInCacheClientFlow() {
        return inCacheClientFlow.get();
    }

    public static void markDbAccessed() {
        dbAccessed.set(true);
    }

    public static boolean isDbAccessed() {
        return dbAccessed.get();
    }

    public static void markRedisAccessed() {
        redisAccessed.set(true);
    }

    public static boolean isRedisAccessed() {
        return redisAccessed.get();
    }

    public static void clear() {
        inCacheClientFlow.remove();
        dbAccessed.remove();
        redisAccessed.remove();
    }
}
