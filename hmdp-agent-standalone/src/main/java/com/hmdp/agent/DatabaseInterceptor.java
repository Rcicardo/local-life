package com.hmdp.agent;

import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;

public class DatabaseInterceptor {

    @RuntimeType
    public static Object intercept(
            @Origin Method method,
            @AllArguments Object[] args,
            @SuperCall Callable<?> callable
    ) throws Exception {
        boolean isInFlow = MonitorContext.isInCacheClientFlow();
        if (isInFlow) {
            MonitorContext.markDbAccessed();
        }

        long startNanos = System.nanoTime();
        try {
            return callable.call();
        } finally {
            long elapsedNanos = System.nanoTime() - startNanos;
            double elapsedMs = elapsedNanos / 1_000_000.0;
            if (isInFlow) {
                System.out.printf("[数据库] %s.%s() | 耗时: %.3f ms%n",
                        method.getDeclaringClass().getSimpleName(), method.getName(), elapsedMs);
            }
        }
    }
}
