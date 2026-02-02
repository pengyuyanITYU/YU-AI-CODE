# MonitorContext 空指针异常问题

## 问题描述

```java
java.lang.NullPointerException: Cannot invoke MonitorContext.getUserId() because context is null
at com.yu.yuaicodemother.monitor.AiModelMonitorListener.onRequest(AiModelMonitorListener.java:74)
```

## 根因分析

在 `AiModelMonitorListener.onRequest()` 方法中，直接调用 `MonitorContextHolder.getContext().getUserId()`，未对返回值进行空值检查。

当请求未经过 `MonitorContextHolder.setContext()` 设置上下文时，`getContext()` 返回 `null`，导致 NPE。

## 发生场景

- 异步线程中触发 AI 模型调用
- 主线程未正确设置 MonitorContext
- 流式响应在回调时线程切换

## 解决方案

### 1. onRequest 方法

```java
MonitorContext context = MonitorContextHolder.getContext();

String userId = (context != null) ? context.getUserId() : "unknown";
String appId = (context != null) ? context.getAppId() : "unknown";
```

### 2. onError 方法优化

```java
// 改从 attributes 获取，而非 MonitorContextHolder（异步场景下 ThreadLocal 可能失效）
MonitorContext context = (MonitorContext) errorContext.attributes().get(MONITOR_CONTEXT_KEY);

String userId = (context != null) ? context.getUserId() : "unknown";
String appId = (context != null) ? context.getAppId() : "unknown";
```

## 经验总结

1. **任何从 ThreadLocal/Holder 获取的值都必须做空值检查**
2. **异步回调场景应从 Context Attributes 获取传递的值，而非重新从 Holder 获取**
3. 防御性编程原则：假设任何外部输入都可能是 null
