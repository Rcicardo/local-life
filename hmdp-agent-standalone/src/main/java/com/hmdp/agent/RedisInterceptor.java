package com.hmdp.agent;

import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.SuperCall;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;

public class RedisInterceptor {
    public static Object intercept(
            @Origin Method method,
            @AllArguments Object[] args,
            @SuperCall Callable<?> callable
    ) throws Exception {
        MonitorContext.markRedisAccessed();

        long startNanos = System.nanoTime();
        try {
            return callable.call();
        } finally {
            long elapsedNanos = System.nanoTime() - startNanos;
            double elapsedMs = elapsedNanos / 1_000_000.0;
            if (!MonitorContext.isDbAccessed()) {
                System.out.printf("[Redis] ValueOperations.get() | Key=%s | 耗时: %.3f ms%n",
                        args.length > 0 ? String.valueOf(args[0]) : "unknown", elapsedMs);
            }
        }
    }
}
