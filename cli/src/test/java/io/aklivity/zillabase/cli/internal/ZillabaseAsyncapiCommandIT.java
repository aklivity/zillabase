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
package io.aklivity.zillabase.cli.internal;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.rules.RuleChain.outerRule;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;

import com.github.rvesse.airline.HelpOption;

import io.aklivity.k3po.runtime.junit.annotation.Specification;
import io.aklivity.k3po.runtime.junit.rules.K3poRule;
import io.aklivity.zillabase.cli.internal.commands.asyncapi.add.ZillabaseAsyncapiAddCommand;
import io.aklivity.zillabase.cli.internal.commands.asyncapi.list.ZillabaseAsyncapiListCommand;
import io.aklivity.zillabase.cli.internal.commands.asyncapi.remove.ZillabaseAsyncapiRemoveCommand;
import io.aklivity.zillabase.cli.internal.commands.start.ZillabaseStartCommand;
import io.aklivity.zillabase.cli.internal.commands.stop.ZillabaseStopCommand;

public class ZillabaseAsyncapiCommandIT
{
    private final K3poRule k3po = new K3poRule()
        .addScriptRoot("server", "io/aklivity/zillabase/cli/internal/streams");

    private final TestRule timeout = new DisableOnDebug(new Timeout(300, SECONDS));

    @Rule
    public final TestRule chain = outerRule(k3po).around(timeout);

    @Test
    @Specification({
        "${server}/asyncapi.add"})
    public void shouldAddAsyncapiSpec() throws Exception
    {
        ZillabaseAsyncapiAddCommand command = new ZillabaseAsyncapiAddCommand();
        command.helpOption = new HelpOption<>();
        command.verbose = true;
        command.spec = "src/test/scripts/io/aklivity/zillabase/cli/internal/specs/asyncapi.yaml";

        command.run();

        k3po.finish();
    }

    @Test
    @Specification({
        "${server}/asyncapi.list.via.id"})
    public void shouldListAsyncapiSpec() throws Exception
    {
        ZillabaseAsyncapiListCommand command = new ZillabaseAsyncapiListCommand();
        command.helpOption = new HelpOption<>();
        command.verbose = true;
        command.id = "1";

        command.run();

        k3po.finish();
    }

    @Test
    @Specification({
        "${server}/asyncapi.remove.via.id"})
    public void shouldRemoveAsyncapiSpec() throws Exception
    {
        ZillabaseAsyncapiRemoveCommand command = new ZillabaseAsyncapiRemoveCommand();
        command.helpOption = new HelpOption<>();
        command.verbose = true;
        command.id = "1";

        command.run();

        k3po.finish();
    }

    @Test
    public void shouldValidateAsyncapiSpec() throws Exception
    {
        ZillabaseStartCommand startCommand = new ZillabaseStartCommand();
        startCommand.helpOption = new HelpOption<>();
        startCommand.run();

        String kafkaSpec = resolveAsyncApiSpec(4);
        String httpSpec = resolveAsyncApiSpec(5);

        ZillabaseStopCommand stopCommand = new ZillabaseStopCommand();
        stopCommand.helpOption = new HelpOption<>();
        stopCommand.run();

        validateKafkaAsyncApiSpec(kafkaSpec);
        validateHttpAsyncApiSpec(httpSpec);
    }

    private void validateKafkaAsyncApiSpec(
        String kafkaSpec)
    {
        String expectedKafkaSpec = """
            ---
            asyncapi: "3.0.0"
            info:
              title: "API Document for Kafka Cluster"
              version: "1.0.0"
              license:
                name: "Aklivity Community License"
                url: "https://github.com/aklivity/zillabase/blob/develop/LICENSE"
            servers:
              plain:
                host: "localhost:9092"
                protocol: "kafka"
                bindings:
                  kafka:
                    bindingVersion: "0.5.0"
                    schemaRegistryUrl: "http://localhost:8080"
                    schemaRegistryVendor: "apicurio"
            channels:
              streampay-replies:
                address: "streampay-replies"
                messages:
                  StreampayRepliesMessage:
                    $ref: "#/components/messages/StreampayRepliesMessage"
                bindings:
                  kafka:
                    bindingVersion: "0.5.0"
                    topicConfiguration:
                      cleanup.policy:
                      - "delete"
              streampay-commands:
                address: "streampay-commands"
                messages:
                  StreampayCommandsMessage:
                    $ref: "#/components/messages/StreampayCommandsMessage"
                bindings:
                  kafka:
                    bindingVersion: "0.5.0"
                    topicConfiguration:
                      cleanup.policy:
                      - "compact"
              events:
                address: "events"
                messages:
                  EventsMessage:
                    $ref: "#/components/messages/EventsMessage"
                bindings:
                  kafka:
                    bindingVersion: "0.5.0"
                    topicConfiguration:
                      cleanup.policy:
                      - "delete"
            operations:
              doStreampayCommands:
                action: "send"
                channel:
                  $ref: "#/channels/streampay-commands"
                messages:
                - $ref: "#/channels/streampay-commands/messages/StreampayCommandsMessage"
                reply:
                  channel:
                    $ref: "#/channels/streampay-replies"
              onStreampayReplies:
                action: "receive"
                channel:
                  $ref: "#/channels/streampay-replies"
                messages:
                - $ref: "#/channels/streampay-replies/messages/StreampayRepliesMessage"
              doEvents:
                action: "send"
                channel:
                  $ref: "#/channels/events"
                messages:
                - $ref: "#/channels/events/messages/EventsMessage"
              onEvents:
                action: "receive"
                channel:
                  $ref: "#/channels/events"
                messages:
                - $ref: "#/channels/events/messages/EventsMessage"
              onStreampayCommands:
                action: "receive"
                channel:
                  $ref: "#/channels/streampay-commands"
                messages:
                - $ref: "#/channels/streampay-commands/messages/StreampayCommandsMessage"
              doStreampayReplies:
                action: "send"
                channel:
                  $ref: "#/channels/streampay-replies"
                messages:
                - $ref: "#/channels/streampay-replies/messages/StreampayRepliesMessage"
            components:
              schemas:
                events-value:
                  fields:
                  - name: "id"
                    type: "string"
                  - name: "status"
                    type: "string"
                  name: "Event"
                  namespace: "io.aklivity.example"
                  type: "record"
                streampay-commands-value:
                  fields:
                  - name: "id"
                    type: "string"
                  - name: "status"
                    type: "string"
                  name: "Event"
                  namespace: "io.aklivity.example"
                  type: "record"
                streampay-replies-value:
                  fields:
                  - name: "price"
                    type: "string"
                  - name: "unit"
                    type: "string"
                  name: "Product"
                  namespace: "io.aklivity.example"
                  type: "record"
              messages:
                StreampayRepliesMessage:
                  payload:
                    $ref: "#/components/schemas/streampay-replies-value"
                  contentType: "application/avro"
                  name: "StreampayRepliesMessage"
                StreampayCommandsMessage:
                  payload:
                    $ref: "#/components/schemas/streampay-commands-value"
                  contentType: "application/avro"
                  name: "StreampayCommandsMessage"
                EventsMessage:
                  payload:
                    $ref: "#/components/schemas/events-value"
                  contentType: "application/avro"
                  name: "EventsMessage"
            """;

        assertEquals(expectedKafkaSpec, kafkaSpec);
    }

    private void validateHttpAsyncApiSpec(
        String httpSpec)
    {
        String expectedHttpSpec = """
            ---
            asyncapi: "3.0.0"
            info:
              title: "API Document for REST APIs"
              version: "1.0.0"
              license:
                name: "Aklivity Community License"
                url: "https://github.com/aklivity/zillabase/blob/develop/LICENSE"
            servers:
              sse:
                host: "localhost:9090"
                protocol: "secure"
              http:
                host: "localhost:9090"
                protocol: "http"
            channels:
              streampay-commands:
                address: "/streampay-commands"
                messages:
                  StreampayCommandsMessage:
                    $ref: "#/components/messages/StreampayCommandsMessage"
              events-item:
                address: "/events/{id}"
                parameters:
                  id:
                    description: "Id of the item."
                messages:
                  EventsMessage:
                    $ref: "#/components/messages/EventsMessage"
              streampay-commands-item:
                address: "/streampay-commands/{id}"
                parameters:
                  id:
                    description: "Id of the item."
                messages:
                  StreampayCommandsMessage:
                    $ref: "#/components/messages/StreampayCommandsMessage"
              events:
                address: "/events"
                messages:
                  EventsMessage:
                    $ref: "#/components/messages/EventsMessage"
            operations:
              doEvents:
                action: "send"
                channel:
                  $ref: "#/channels/events"
                bindings:
                  x-zilla-http-kafka:
                    method: "POST"
                    overrides:
                      zilla:identity: "{identity}"
                  http:
                    bindingVersion: "0.3.0"
                    method: "POST"
                messages:
                - $ref: "#/channels/events/messages/EventsMessage"
              onEventsReadItem:
                action: "receive"
                channel:
                  $ref: "#/channels/events-item"
                messages:
                - $ref: "#/channels/events/messages/EventsMessage"
              doStreampayCommandsUpdate:
                action: "send"
                channel:
                  $ref: "#/channels/streampay-commands-item"
                bindings:
                  x-zilla-http-kafka:
                    method: "PUT"
                    overrides:
                      zilla:identity: "{identity}"
                  http:
                    bindingVersion: "0.3.0"
                    method: "PUT"
                messages:
                - $ref: "#/channels/streampay-commands/messages/StreampayCommandsMessage"
              onStreampayCommandsRead:
                action: "receive"
                channel:
                  $ref: "#/channels/streampay-commands"
                messages:
                - $ref: "#/channels/streampay-commands/messages/StreampayCommandsMessage"
              onStreampayCommandsReadItem:
                action: "receive"
                channel:
                  $ref: "#/channels/streampay-commands-item"
                messages:
                - $ref: "#/channels/streampay-commands/messages/StreampayCommandsMessage"
              onEventsRead:
                action: "receive"
                channel:
                  $ref: "#/channels/events"
                messages:
                - $ref: "#/channels/events/messages/EventsMessage"
              doStreampayCommandsCreate:
                action: "send"
                channel:
                  $ref: "#/channels/streampay-commands"
                bindings:
                  x-zilla-http-kafka:
                    method: "POST"
                    overrides:
                      zilla:identity: "{identity}"
                  http:
                    bindingVersion: "0.3.0"
                    method: "POST"
                messages:
                - $ref: "#/channels/streampay-commands/messages/StreampayCommandsMessage"
            components:
              schemas:
                events-value:
                  fields:
                  - name: "id"
                    type: "string"
                  - name: "status"
                    type: "string"
                  name: "Event"
                  namespace: "io.aklivity.example"
                  type: "record"
                streampay-commands-value:
                  fields:
                  - name: "id"
                    type: "string"
                  - name: "status"
                    type: "string"
                  name: "Event"
                  namespace: "io.aklivity.example"
                  type: "record"
                streampay-replies-value:
                  fields:
                  - name: "price"
                    type: "string"
                  - name: "unit"
                    type: "string"
                  name: "Product"
                  namespace: "io.aklivity.example"
                  type: "record"
              messages:
                StreampayRepliesMessage:
                  payload:
                    $ref: "#/components/schemas/streampay-replies-value"
                  contentType: "application/avro"
                  name: "StreampayRepliesMessage"
                StreampayCommandsMessage:
                  payload:
                    $ref: "#/components/schemas/streampay-commands-value"
                  contentType: "application/avro"
                  name: "StreampayCommandsMessage"
                EventsMessage:
                  payload:
                    $ref: "#/components/schemas/events-value"
                  contentType: "application/avro"
                  name: "EventsMessage"
            """;

        assertEquals(expectedHttpSpec, httpSpec);
    }

    private String resolveAsyncApiSpec(
        int globalId)
    {
        String responseBody;
        HttpClient client = HttpClient.newHttpClient();
        try
        {
            HttpRequest httpRequest = HttpRequest
                .newBuilder(URI.create("http://localhost:8080")
                    .resolve("/apis/registry/v2/ids/globalIds/%d".formatted(globalId)))
                .GET()
                .build();

            HttpResponse<String> httpResponse = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            responseBody = httpResponse.statusCode() == 200 ? httpResponse.body() : null;
        }
        catch (Exception ex)
        {
            responseBody = null;
        }
        return responseBody;
    }
}
