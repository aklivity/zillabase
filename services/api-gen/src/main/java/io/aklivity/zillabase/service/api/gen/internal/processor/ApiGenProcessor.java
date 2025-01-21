/*
 * Copyright 2024 Aklivity Inc
 *
 * Licensed under the Aklivity Community License (the "License"); you may not use
 * this file except in compliance with the License.  You may obtain a copy of the
 * License at
 *
 *   https://www.aklivity.io/aklivity-community-license/
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 */
package io.aklivity.zillabase.service.api.gen.internal.processor;

import java.io.UnsupportedEncodingException;
import java.util.Map;

import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Branched;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Named;
import org.apache.kafka.streams.kstream.Produced;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.aklivity.zillabase.service.api.gen.internal.config.ApiGenConfig;
import io.aklivity.zillabase.service.api.gen.internal.model.ApiGenEvent;
import io.aklivity.zillabase.service.api.gen.internal.model.ApiGenEventType;
import io.aklivity.zillabase.service.api.gen.internal.serde.ApiGenEventSerde;
import io.aklivity.zillabase.service.api.gen.internal.service.HttpAsyncApiService;
import io.aklivity.zillabase.service.api.gen.internal.service.KafkaAsyncApiService;
import io.aklivity.zillabase.service.api.gen.internal.service.PublishConfigService;

@Component
public class ApiGenProcessor
{
    private final Serde<String> stringSerde = Serdes.String();
    private final Serde<byte[]> byteSerde = Serdes.ByteArray();
    private final Serde<ApiGenEvent> eventSerde = new ApiGenEventSerde();

    private final ApiGenConfig config;
    private final KafkaAsyncApiService kafkaAsyncApiHandler;
    private final HttpAsyncApiService httpAsyncApiHandler;
    private final PublishConfigService publishConfigHandler;

    public ApiGenProcessor(
        ApiGenConfig config,
        KafkaAsyncApiService kafkaAsyncApiService,
        HttpAsyncApiService httpAsyncApiHandler,
        PublishConfigService publishConfigService)
    {
        this.config = config;
        this.kafkaAsyncApiHandler = kafkaAsyncApiService;
        this.httpAsyncApiHandler = httpAsyncApiHandler;
        this.publishConfigHandler = publishConfigService;
    }

    @Autowired
    public void buildPipeline(
        StreamsBuilder streamsBuilder)
    {
        String zcatalogsTopic = config.zcatalogsTopic();
        String eventsTopic = config.eventsTopic();

        streamsBuilder.stream(zcatalogsTopic, Consumed.with(stringSerde, byteSerde))
            .mapValues(e ->
            {
                try
                {
                    System.out.println("Catalog updated: " + new String(e, "UTF-8"));
                }
                catch (UnsupportedEncodingException ex)
                {
                    throw new RuntimeException(ex);
                }
                return new ApiGenEvent(ApiGenEventType.CATALOG_UPDATED, null, null, null);
            })
            .to(eventsTopic, Produced.with(stringSerde, eventSerde));

        KStream<String, ApiGenEvent> eventsStream = streamsBuilder.stream(eventsTopic,
            Consumed.with(stringSerde, eventSerde));
        Map<String, KStream<String, ApiGenEvent>> branches = eventsStream.split(Named.as("branch-"))
            .branch((key, value) ->
                value.type() == ApiGenEventType.CATALOG_UPDATED, Branched.as("catalog-updated"))
            .branch((key, value) ->
                value.type() == ApiGenEventType.KAFKA_ASYNC_API_PUBLISHED, Branched.as("kafka-async-api-published"))
            .branch((key, value) ->
                value.type() == ApiGenEventType.HTTP_ASYNC_API_PUBLISHED, Branched.as("http-async-api-published"))
            .defaultBranch(Branched.as("branch-catalog-updated"));

        branches.get("branch-catalog-updated")
            .mapValues(kafkaAsyncApiHandler::generate)
            .to(eventsTopic, Produced.with(stringSerde, eventSerde));

        branches.get("branch-kafka-async-api-published")
            .mapValues(httpAsyncApiHandler::generate)
            .to(eventsTopic, Produced.with(stringSerde, eventSerde));
        branches.get("branch-http-async-api-published")
            .mapValues(publishConfigHandler::publish)
            .to(eventsTopic, Produced.with(stringSerde, eventSerde));
    }
}
