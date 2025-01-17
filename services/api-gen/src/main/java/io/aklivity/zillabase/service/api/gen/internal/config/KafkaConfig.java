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
package io.aklivity.zillabase.service.api.gen.internal.config;

import static org.apache.kafka.streams.StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG;
import static org.apache.kafka.streams.StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG;

import java.util.HashMap;
import java.util.Map;

import jakarta.annotation.PostConstruct;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.errors.LogAndContinueExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.KafkaStreamsConfiguration;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;
import org.springframework.kafka.support.serializer.JsonSerde;

@Configuration
@EnableKafka
public class KafkaConfig
{
    public static final String API_GEN_STREAMS_BUILDER_BEAN_NAME = "apiGenKafkaStreamsBuilder";

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Value("${spring.kafka.streams.state.dir:#{null}}")
    private String stateDir;

    @Autowired
    private ApplicationContext context;

    public KafkaConfig()
    {
    }

    @Bean
    public AdminClient adminClient()
    {
        Map<String, Object> config = new HashMap<>();
        config.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        return AdminClient.create(config);
    }

    @Bean(name = API_GEN_STREAMS_BUILDER_BEAN_NAME)
    public StreamsBuilderFactoryBean apiGenKafkaStreamsBuilder()
    {
        Map<String, Object> paymentStreamsConfigProperties = commonStreamsConfigProperties();
        paymentStreamsConfigProperties.put(StreamsConfig.APPLICATION_ID_CONFIG, API_GEN_STREAMS_BUILDER_BEAN_NAME);
        return new StreamsBuilderFactoryBean(new KafkaStreamsConfiguration(paymentStreamsConfigProperties));
    }

    private Map<String, Object> commonStreamsConfigProperties()
    {
        final Map<String, Object> props = new HashMap<>();
        props.put("bootstrap.servers", this.bootstrapServers);
        props.put(DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        props.put(DEFAULT_VALUE_SERDE_CLASS_CONFIG, JsonSerde.class.getName());
        props.put("default.deserialization.exception.handler", LogAndContinueExceptionHandler.class);
        if (this.stateDir != null)
        {
            props.put("state.dir", this.stateDir);
        }

        props.put("commit.interval.ms", 0);
        return props;
    }

    @PostConstruct
    public void checkConnection()
    {
        Map<String, Object> conf = commonStreamsConfigProperties();
        conf.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, 15000);
        conf.put(AdminClientConfig.RETRIES_CONFIG, 1);

        DescribeClusterResult describeClusterResult = null;
        try (AdminClient adminClient = AdminClient.create(conf))
        {
            describeClusterResult = adminClient.describeCluster();
        }
        catch (Exception e)
        {
            // ignore
        }

        if (describeClusterResult == null ||
            describeClusterResult.clusterId().isCompletedExceptionally())
        {
            ((ConfigurableApplicationContext) context).close();
        }
    }
}
