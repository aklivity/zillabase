package io.aklivity.zillabase.service.api.gen.internal.processor;

import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Produced;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.aklivity.zillabase.service.api.gen.internal.model.ApiGenEventType;
import io.aklivity.zillabase.service.api.gen.internal.service.HttpAsyncApiService;
import io.aklivity.zillabase.service.api.gen.internal.service.KafkaAsyncApiService;
import io.aklivity.zillabase.service.api.gen.internal.service.PublishConfigService;
import io.aklivity.zillabase.service.api.gen.internal.model.ApiGenEvent;
import io.aklivity.zillabase.service.api.gen.internal.serde.ApiGenEventSerde;

@Component
public class ApiGenProcessor
{
    private final Serde<String> stringSerde = Serdes.String();
    private final Serde<ApiGenEvent> eventSerde = new ApiGenEventSerde();

    @Value("${zcatalogs.topic:public.ztatalogs}")
    String zcatalogsTopic;

    @Value("${api.gen.events.topic:public.api-gen-events}")
    String eventsTopic;

    private final KafkaAsyncApiService kafkaAsyncApiHandler;
    private final HttpAsyncApiService httpAsyncApiHandler;
    private final PublishConfigService publishConfigHandler;

    public ApiGenProcessor(
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
            .mapValues(e -> new ApiGenEvent(ApiGenEventType.CATALOG_UPDATED, "", "0"))
            .to(eventsTopic, Produced.with(stringSerde, eventSerde));

        KStream<String, ApiGenEvent> eventsStream = streamsBuilder.stream(eventsTopic,
            Consumed.with(stringSerde, eventSerde));
        KStream<String, ApiGenEvent>[] branches = eventsStream.branch(
            (key, value) -> value.type() == ApiGenEventType.CATALOG_UPDATED,
            (key, value) -> value.type() == ApiGenEventType.KAFKA_ASYNC_API_PUBLISHED,
            (key, value) -> value.type() == ApiGenEventType.HTTP_ASYNC_API_PUBLISHED);

        branches[0]
            .mapValues(kafkaAsyncApiHandler::generate)
            .to(eventsTopic, Produced.with(stringSerde, eventSerde));

        branches[1].mapValues(httpAsyncApiHandler::generate)
            .to(eventsTopic, Produced.with(stringSerde, eventSerde));

        branches[2].mapValues(publishConfigHandler::publish)
            .to(eventsTopic, Produced.with(stringSerde, eventSerde));

    }
}
