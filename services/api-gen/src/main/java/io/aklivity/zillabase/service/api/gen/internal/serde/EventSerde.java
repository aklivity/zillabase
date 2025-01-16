package io.aklivity.zillabase.service.api.gen.internal.serde;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.Serde;

import io.aklivity.zillabase.service.api.gen.internal.model.ApiGenEvent;

public class EventSerde implements Serde<ApiGenEvent>
{

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Serializer<ApiGenEvent> serializer()
    {
        return (topic, data) ->
        {
            try
            {
                return objectMapper.writeValueAsBytes(data);
            }
            catch (Exception e)
            {
                throw new RuntimeException("Error serializing Event", e);
            }
        };
    }

    @Override
    public Deserializer<ApiGenEvent> deserializer()
    {
        return (topic, data) ->
        {
            try
            {
                return objectMapper.readValue(data, ApiGenEvent.class);
            }
            catch (Exception e)
            {
                throw new RuntimeException("Error deserializing Event", e);
            }
        };
    }
}
