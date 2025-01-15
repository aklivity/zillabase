package io.aklivity.zillabase.service.api.gen.internal;

import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Produced;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.aklivity.zillabase.service.api.gen.internal.service.HttpAsyncApiService;
import io.aklivity.zillabase.service.api.gen.internal.service.KafkaAsyncApiService;
import io.aklivity.zillabase.service.api.gen.internal.service.PublishConfigService;
import io.aklivity.zillabase.service.api.gen.internal.serde.ApiGenEvent;
import io.aklivity.zillabase.service.api.gen.internal.serde.EventSerde;

@Component
public class ApiGenTopology
{
    private final Serde<String> stringSerde = Serdes.String();
    private final Serde<ApiGenEvent> eventSerde = new EventSerde();

    @Value("${zcatalogs.topic:public.ztatalogs}")
    String zcatalogsTopic;

    @Value("${api.gen.events.topic:public.api-gen-events}")
    String eventsTopic;

    private final KafkaAsyncApiService kafkaAsyncApiHandler;
    private final HttpAsyncApiService httpAsyncApiHandler;
    private final PublishConfigService publishConfigHandler;

    public ApiGenTopology(
        KafkaAsyncApiService kafkaAsyncApiService,
        HttpAsyncApiService httpAsyncApiHandler,
        PublishConfigService publishConfigService)
    {
        this.kafkaAsyncApiHandler = kafkaAsyncApiService;
        this.httpAsyncApiHandler = httpAsyncApiHandler;
        this.publishConfigHandler = publishConfigService;
    }

    @Autowired
    public void buildPipeline(
        StreamsBuilder streamsBuilder)
    {
        streamsBuilder.stream(zcatalogsTopic, Consumed.with(stringSerde, stringSerde))
            .mapValues(e -> new ApiGenEvent("catalog_updated", 0, 0))
            .to(eventsTopic, Produced.with(stringSerde, eventSerde));

        KStream<String, ApiGenEvent> eventsStream = streamsBuilder.stream(eventsTopic,
            Consumed.with(stringSerde, eventSerde));
        KStream<String, ApiGenEvent>[] branches = eventsStream.branch(
            (key, value) -> value.name().equals("catalog_updated"),
            (key, value) -> value.name().equals("kafka_asyncapi_generated"),
            (key, value) -> value.name().equals("http_asyncapi_generated"));

        branches[0]
            .mapValues(kafkaAsyncApiHandler::generate)
            .to(eventsTopic, Produced.with(stringSerde, eventSerde));

        branches[1].mapValues(httpAsyncApiHandler::generate)
            .to(eventsTopic, Produced.with(stringSerde, eventSerde));

        branches[2].mapValues(publishConfigHandler::publish)
            .to(eventsTopic, Produced.with(stringSerde, eventSerde));

    }
}
