package io.aklivity.zillabase.service.api.gen.internal.service;

import java.io.StringReader;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonValue;

import org.springframework.stereotype.Service;

import io.aklivity.zillabase.service.api.gen.internal.serde.Event;

@Service
public class PublishConfigService
{
    public Event publish(
        Event event)
    {
        JsonValue jsonValue = Json.createReader(new StringReader(httpSpec)).readValue();
        JsonObject operations = jsonValue.asJsonObject().getJsonObject("operations");
        for (Map.Entry<String, JsonValue> operation : operations.entrySet())
        {
            this.operations.add(operation.getKey());
        }

        return new Event();
    }
}
