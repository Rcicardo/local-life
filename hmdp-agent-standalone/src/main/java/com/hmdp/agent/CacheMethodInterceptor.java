package com.hmdp.agent;

import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.SuperCall;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;

public class CacheMethodInterceptor {

    public static Object intercept(
            @Origin Method method,
            @AllArguments Object[] args,
            @SuperCall Callable<?> callable
    ) throws Exception {
        String className = method.getDeclaringClass().getSimpleName();
        String methodName = method.getName();
        String id = args.length > 1 ? String.valueOf(args[1]) : "unknown";

        MonitorContext.enterCacheClientFlow();

        long startNanos = System.nanoTime();
        try {
            return callable.call();
        } finally {
            long elapsedNanos = System.nanoTime() - startNanos;
            double elapsedMs = elapsedNanos / 1_000_000.0;
            System.out.printf("[Monitor] %s.%s() | ID=%s | 总耗时: %.3f ms%n",
                    className, methodName, id, elapsedMs);
            MonitorContext.clear();
        }
    }
}
