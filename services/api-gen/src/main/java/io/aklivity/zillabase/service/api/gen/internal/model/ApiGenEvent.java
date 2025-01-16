package io.aklivity.zillabase.service.api.gen.internal.model;

public record ApiGenEvent(
    ApiGenEventName name,
    String kafkaVersion,
    String httpVersion)
{
}
