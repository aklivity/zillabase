package io.aklivity.zillabase.service.api.gen.internal.serde;

public record Event(
    String name,
    int kafkaVersion,
    int httpVersion)
{
}
