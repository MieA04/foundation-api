# foundation-api

`foundation-api` 是 Minecraft 模组 `foundation-core` 的前置核心 API 模块，用于提供一套轻量、显式、需要手动注册的服务容器能力，并提供基础的服务发现与 Capability 注册机制。

这个项目的目标不是提供完整的依赖注入框架，而是为多个模组之间共享服务、声明能力、发现可用功能提供一个简单稳定的约定层。

## 功能特性

- 简单服务容器：通过 `Class<?>` 作为 key 手动注册服务实例。
- 服务发现：调用方可以按服务类型查询可用服务。
- 状态感知：服务和 Capability 都带有状态，便于表达初始化、可用、降级、失败、禁用等阶段。
- 重复注册保护：同一个服务类型或 Capability key 重复注册时会抛出异常。
- 并发友好：默认实现基于 `ConcurrentHashMap`。
- Capability 注册表：用于声明某个模组提供的能力、版本、特性集合和可用状态。

## 适用场景

- `foundation-core` 作为多个模组的公共前置核心。
- 一个模组希望向其他模组暴露服务实例。
- 模组之间需要判断某项能力是否存在、是否可用。
- 希望避免硬编码依赖具体实现类，只依赖一组轻量 API。

## 环境要求

- Java 21
- Maven 3.x

## 核心概念

### Foundation

`Foundation` 是聚合入口，持有：

- `FoundationContainer`：服务容器。
- `CapabilityRegistry`：Capability 注册表。

### FoundationContainer

服务容器接口，提供：

- `registry(Class<?> type, ServiceEntry<T> entry)`：注册服务。
- `get(Class<T> key)`：按类型获取可用服务实例。
- `getEntry(Class<?> key)`：获取完整服务条目。
- `contains(Class<?> type)`：判断服务是否已注册。

只有状态为 `AVAILABLE` 或 `DEGRADED` 的服务会通过 `get` 返回实例。

### CapabilityRegistry

Capability 注册表接口，提供：

- `register(CapabilityKey key, CapabilityInfo info)`：注册 Capability。
- `update(CapabilityKey key, CapabilityInfo info)`：更新已存在的 Capability。
- `get(CapabilityKey key)`：获取 Capability 信息。
- `contains(CapabilityKey key)`：判断 Capability 是否存在。
- `isAvailable(CapabilityKey key)`：判断 Capability 是否为 `AVAILABLE`。
- `list()`：列出全部 Capability。

## 快速开始

### 创建 Foundation 实例

```java
import org.miea04.foundation.Foundation;
import org.miea04.foundation.api.container.FoundationContainer;
import org.miea04.foundation.runtime.container.DefaultContainer;

public final class FoundationBootstrap {

    public static Foundation createFoundation() {
        FoundationContainer container = new DefaultContainer();
        org.miea04.foundation.api.capability.CapabilityRegistry capabilityRegistry =
                new org.miea04.foundation.runtime.capability.CapabilityRegistry();

        return new Foundation(container, capabilityRegistry);
    }
}
```

### 注册服务

```java
import org.miea04.foundation.api.container.FoundationContainer;
import org.miea04.foundation.api.container.service.ServiceEntry;
import org.miea04.foundation.api.container.service.ServiceInfo;
import org.miea04.foundation.api.container.service.ServiceStatus;

import java.util.Map;
import java.util.Set;

public final class ServiceRegistrationExample {

    public static void registerConfigService(FoundationContainer container, ConfigService service) {
        ServiceInfo info = new ServiceInfo(
                "example_mod",
                "1.0.0",
                ServiceStatus.AVAILABLE,
                "Config service is ready",
                Set.of("config"),
                Map.of("side", "common")
        );

        ServiceEntry<ConfigService> entry = new ServiceEntry<>(
                ConfigService.class,
                service,
                info
        );

        container.registry(ConfigService.class, entry);
    }
}
```

### 发现服务

```java
import org.miea04.foundation.api.container.FoundationContainer;

public final class ServiceDiscoveryExample {

    public static void useConfigService(FoundationContainer container) {
        container.get(ConfigService.class).ifPresent(configService -> {
            configService.reload();
        });
    }
}
```

如果服务不存在，或服务状态不是 `AVAILABLE` / `DEGRADED`，`get` 会返回 `Optional.empty()`。

### 注册 Capability

```java
import org.miea04.foundation.api.capability.CapabilityInfo;
import org.miea04.foundation.api.capability.CapabilityKey;
import org.miea04.foundation.api.capability.CapabilityRegistry;
import org.miea04.foundation.api.capability.CapabilityStatus;

import java.util.Set;

public final class CapabilityRegistrationExample {

    public static void registerCapability(CapabilityRegistry registry) {
        CapabilityKey key = CapabilityKey.of("example_mod", "config");
        CapabilityInfo info = new CapabilityInfo(
                CapabilityStatus.AVAILABLE,
                "example_mod",
                "1.0.0",
                "Config capability is ready",
                Set.of("read", "write", "reload")
        );

        registry.register(key, info);
    }
}
```

### 查询 Capability

```java
import org.miea04.foundation.api.capability.CapabilityKey;
import org.miea04.foundation.api.capability.CapabilityRegistry;

public final class CapabilityDiscoveryExample {

    public static boolean canReloadConfig(CapabilityRegistry registry) {
        CapabilityKey key = CapabilityKey.of("example_mod", "config");
        return registry.isAvailable(key)
                && registry.get(key)
                .map(info -> info.features().contains("reload"))
                .orElse(false);
    }
}
```

## 状态说明

服务和 Capability 均使用类似的状态集合：

| 状态 | 含义 |
| --- | --- |
| `REGISTERED` | 已注册，但尚未初始化完成 |
| `INITIALIZING` | 初始化中 |
| `AVAILABLE` | 可用 |
| `DEGRADED` | 降级可用 |
| `UNAVAILABLE` | 当前不可用 |
| `FAILED` | 初始化或运行失败 |
| `DISABLED` | 被禁用 |

服务容器中，`AVAILABLE` 和 `DEGRADED` 会被视为可通过 `get` 发现。

Capability 注册表中，只有 `AVAILABLE` 会被 `isAvailable` 视为可用。

## 异常行为

- 重复注册服务会抛出 `ServiceAlreadyRegisteredException`。
- 重复注册 Capability 会抛出 `CapabilityRegisterRepeatException`。
- 注册服务或 Capability 时传入 `null` key / entry / info 会抛出 `NullPointerException`。

## 测试

运行全部测试：

```powershell
$env:JAVA_HOME='C:\Java21'
mvn '-Dmaven.repo.local=.m2/repository' test
```

生成 Surefire HTML 测试报告：

```powershell
$env:JAVA_HOME='C:\Java21'
mvn '-U' '-Dmaven.repo.local=.m2/repository' surefire-report:report
```

报告输出位置：

- `target/surefire-reports/`
- `target/reports/surefire.html`

当前测试覆盖：

- 服务注册、查询、重复注册、不可用状态过滤。
- Capability 注册、更新、查询、重复注册、缺失 key 边界。
- 并发重复注册。
- 大批量 Capability 注册与高频服务查询。

## 项目结构

```text
src/main/java/org/miea04/foundation
├── Foundation.java
├── api
│   ├── capability
│   ├── container
│   └── exception
└── runtime
    ├── capability
    └── container
```

## 设计约定

- 注册操作由调用方显式触发，项目不会自动扫描服务。
- 服务 key 默认使用服务接口或服务类的 `Class<?>`。
- Capability key 使用 `namespace + path` 组合，建议 `namespace` 使用提供方模组 ID。
- 查询方应始终处理 `Optional.empty()`，不要假设服务或 Capability 一定存在。
- 跨模组暴露服务时，优先暴露稳定接口，而不是具体实现类。

## License

本项目使用MIT协议
