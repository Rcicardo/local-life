package com.hmdp.agent;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.utility.JavaModule;

import java.lang.instrument.Instrumentation;

public class MyAgent {

    public static void premain(String agentArgs, Instrumentation inst) {
        System.out.println("========================================");
        System.out.println("  [Agent] hmdp-agent 已加载");
        System.out.println("  [Agent] 分层监控: 本地缓存 / Redis / 数据库");
        System.out.println("========================================");

        new AgentBuilder.Default()
                .with(new AgentBuilder.Listener() {
                    @Override
                    public void onDiscovery(String typeName, ClassLoader classLoader, JavaModule module, boolean loaded) {}
                    @Override
                    public void onTransformation(TypeDescription typeDescription, ClassLoader classLoader, JavaModule module, boolean loaded, DynamicType dynamicType) {
                        System.out.println("[Agent] ✅ 转换成功: " + typeDescription.getName());
                    }
                    @Override
                    public void onIgnored(TypeDescription typeDescription, ClassLoader classLoader, JavaModule module, boolean loaded) {}
                    @Override
                    public void onError(String typeName, ClassLoader classLoader, JavaModule module, boolean loaded, Throwable throwable) {
                        if (typeName.contains("CacheClient") || typeName.contains("caffeine") || typeName.contains("redis") || typeName.contains("Executor")) {
                            System.err.println("[Agent] ❌ 转换失败: " + typeName);
                            throwable.printStackTrace();
                        }
                    }
                    @Override
                    public void onComplete(String typeName, ClassLoader classLoader, JavaModule module, boolean loaded) {}
                })
                // 1. 拦截 CacheClient 总耗时
                .type(ElementMatchers.named("com.hmdp.utils.CacheClient"))
                .transform((builder, type, classLoader, module, protectionDomain) -> {
                    return builder.method(
                            ElementMatchers.named("queryWithPassThrough")
                                    .or(ElementMatchers.named("queryWithLogicalExpire"))
                    ).intercept(MethodDelegation.to(CacheMethodInterceptor.class));
                })
                // 2. 拦截 Caffeine 本地缓存（精确匹配 LocalManualCache，只拦截这一个类）
                .type(ElementMatchers.named("com.github.benmanes.caffeine.cache.LocalManualCache"))
                .transform((builder, type, classLoader, module, protectionDomain) -> {
                    return builder.method(
                            ElementMatchers.named("getIfPresent")
                    ).intercept(MethodDelegation.to(LocalCacheInterceptor.class));
                })
                // 3. 拦截 Redis（精确匹配 DefaultValueOperations）
                .type(ElementMatchers.named("org.springframework.data.redis.core.DefaultValueOperations"))
                .transform((builder, type, classLoader, module, protectionDomain) -> {
                    return builder.method(
                            ElementMatchers.named("get")
                                    .and(ElementMatchers.takesArguments(1))
                    ).intercept(MethodDelegation.to(RedisInterceptor.class));
                })
                // 4. 拦截数据库（精确匹配 CachingExecutor，最外层，只打印一条）
                .type(ElementMatchers.named("org.apache.ibatis.executor.CachingExecutor"))
                .transform((builder, type, classLoader, module, protectionDomain) -> {
                    return builder.method(
                            ElementMatchers.named("query")
                                    .or(ElementMatchers.named("update"))
                    ).intercept(MethodDelegation.to(DatabaseInterceptor.class));
                })
                .installOn(inst);
    }
}
