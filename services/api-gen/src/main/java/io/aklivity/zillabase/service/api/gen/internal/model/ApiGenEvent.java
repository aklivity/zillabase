package io.aklivity.zillabase.service.api.gen.internal.model;

public record ApiGenEvent(
    ApiGenEventState name,
    String kafkaVersion,
    String httpVersion)
{
}
