package io.aklivity.zillabase.service.api.gen.internal.model;

public record ApiGenEvent(
    ApiGenEventType type,
    String kafkaVersion,
    String httpVersion)
{
}
