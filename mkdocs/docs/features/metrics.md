# Метрики

Kora использует micrometer для записи метрик приложения

## Подключение

Для подключения метрик необходимо в Application примешать `ru.tinkoff.kora.micrometer.module.MetricsModule` из `micrometer-module` 

```groovy
    implementation "ru.tinkoff.kora:micrometer-module"
```

После этого в Metrics.globalRegistry будет зарегистрирован наш `PrometheusMeterRegistry`, который будет использоваться во всех компонентах, собирающих метрики

## Соглашения об именовании и составе метрик

Мы следуем и вам рекомендуем использовать нотацию, описанную в opentelemetry спецификации

## Модификация конфигурации

Для внесения изменений в конфигурацию `PrometheusMeterRegistry` нужно добавить в контейнер `PrometheusMeterRegistryInitializer`

**Важно**, `PrometheusMeterRegistryInitializer` применяется только один раз при инициализации приложения

Например, мы хотим добавить общий тег для всех метрик

```java
@Module
interface MetricsConfigModule {
    default PrometheusMeterRegistryInitializer commonTagsInit() {
        return registry -> {
            registry.config().commonTags("tag", "value");
            return registry;
        };
    }
}
```

