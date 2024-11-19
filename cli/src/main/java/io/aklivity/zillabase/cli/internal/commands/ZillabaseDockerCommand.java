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
package io.aklivity.zillabase.cli.internal.commands;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbException;
import jakarta.json.spi.JsonProvider;
import jakarta.json.stream.JsonParser;

import org.leadpony.justify.api.JsonSchema;
import org.leadpony.justify.api.JsonSchemaReader;
import org.leadpony.justify.api.JsonValidationService;
import org.leadpony.justify.api.ProblemHandler;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;

import io.aklivity.zillabase.cli.config.ZillabaseConfig;

public abstract class ZillabaseDockerCommand extends ZillabaseCommand
{
    @Override
    protected final void invoke()
    {
        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
            .withDockerHost("unix:///var/run/docker.sock")
            .build();

        DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
            .dockerHost(config.getDockerHost())
            .sslConfig(config.getSSLConfig())
            .maxConnections(100)
            .connectionTimeout(Duration.ofSeconds(30))
            .responseTimeout(Duration.ofSeconds(45))
            .build();

        DockerClient client = DockerClientImpl.getInstance(config, httpClient);
        final ZillabaseConfig zillabaseConfig = readZillabaseConfig();

        invoke(client, zillabaseConfig);
    }

    protected abstract void invoke(
        DockerClient client,
        ZillabaseConfig config);

    private ZillabaseConfig readZillabaseConfig()
    {
        ZillabaseConfig config;

        Path configPath = Paths.get("zillabase/config.yaml");
        try
        {
            if (Files.size(configPath) == 0 || Files.readAllLines(configPath)
                .stream().allMatch(line -> line.trim().isEmpty() || line.trim().startsWith("#")))
            {
                config = new ZillabaseConfig();
            }
            else
            {
                try (InputStream inputStream = Files.newInputStream(configPath);
                     InputStream schemaStream = getClass().getResourceAsStream("/internal/schema/zillabase.schema.json"))
                {
                    JsonProvider schemaProvider = JsonProvider.provider();
                    JsonReader schemaReader = schemaProvider.createReader(schemaStream);
                    JsonObject schemaObject = schemaReader.readObject();

                    JsonParser schemaParser = schemaProvider.createParserFactory(null)
                        .createParser(new StringReader(schemaObject.toString()));

                    JsonValidationService service = JsonValidationService.newInstance();
                    JsonSchemaReader reader = service.createSchemaReader(schemaParser);
                    JsonSchema schema = reader.read();

                    JsonProvider provider = service.createJsonProvider(schema, parser -> ProblemHandler.throwing());

                    Jsonb jsonb = JsonbBuilder.newBuilder()
                        .withProvider(provider)
                        .build();
                    config = jsonb.fromJson(inputStream, ZillabaseConfig.class);
                }
            }
        }
        catch (IOException | JsonbException ex)
        {
            System.err.println("Error resolving config, reverting to default.");
            ex.printStackTrace(System.err);
            config = new ZillabaseConfig();
        }

        return config;
    }
}
