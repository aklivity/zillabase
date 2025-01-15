package io.aklivity.zillabase.service.api.gen.internal.serde;

import io.aklivity.zillabase.service.api.gen.internal.service.ApiGenEventName;

public record ApiGenEvent(
    ApiGenEventName name,
    String kafkaVersion,
    String httpVersion)
{
}
