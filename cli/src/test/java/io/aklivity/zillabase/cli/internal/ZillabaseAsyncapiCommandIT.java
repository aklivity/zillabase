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

import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;

import com.github.rvesse.airline.HelpOption;

import io.aklivity.k3po.runtime.junit.annotation.Specification;
import io.aklivity.k3po.runtime.junit.rules.K3poRule;
import io.aklivity.zillabase.cli.config.ZillabaseConfig;
import io.aklivity.zillabase.cli.internal.asyncapi.KafkaTopicSchemaRecord;
import io.aklivity.zillabase.cli.internal.commands.asyncapi.add.ZillabaseAsyncapiAddCommand;
import io.aklivity.zillabase.cli.internal.commands.asyncapi.list.ZillabaseAsyncapiListCommand;
import io.aklivity.zillabase.cli.internal.commands.asyncapi.remove.ZillabaseAsyncapiRemoveCommand;
import io.aklivity.zillabase.cli.internal.commands.start.ZillabaseStartCommand;

public class ZillabaseAsyncapiCommandIT
{
    private final K3poRule k3po = new K3poRule()
        .addScriptRoot("server", "io/aklivity/zillabase/cli/internal/streams");

    private final TestRule timeout = new DisableOnDebug(new Timeout(10, SECONDS));

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
    public void shouldGenerateKafkaAsyncapiSpec() throws Exception
    {
        String expectedSpec =
            "---\n" +
            "asyncapi: \"3.0.0\"\n" +
            "info:\n" +
            "  title: \"API Document for Kafka Cluster\"\n" +
            "  version: \"1.0.0\"\n" +
            "  license:\n" +
            "    name: \"Aklivity Community License\"\n" +
            "    url: \"https://github.com/aklivity/zillabase/blob/develop/LICENSE\"\n" +
            "servers:\n" +
            "  plain:\n" +
            "    host: \"localhost:9092\"\n" +
            "    protocol: \"kafka\"\n" +
            "    bindings:\n" +
            "      kafka:\n" +
            "        bindingVersion: \"0.5.0\"\n" +
            "        schemaRegistryUrl: \"http://localhost:8080\"\n" +
            "        schemaRegistryVendor: \"apicurio\"\n" +
            "channels:\n" +
            "  events:\n" +
            "    address: \"events\"\n" +
            "    messages:\n" +
            "      EventsMessage:\n" +
            "        $ref: \"#/components/messages/EventsMessage\"\n" +
            "operations:\n" +
            "  doEvents:\n" +
            "    action: \"send\"\n" +
            "    channel:\n" +
            "      $ref: \"#/channels/events\"\n" +
            "    messages:\n" +
            "    - $ref: \"#/channels/events/messages/EventsMessage\"\n" +
            "  onEvents:\n" +
            "    action: \"receive\"\n" +
            "    channel:\n" +
            "      $ref: \"#/channels/events\"\n" +
            "    messages:\n" +
            "    - $ref: \"#/channels/events/messages/EventsMessage\"\n" +
            "components:\n" +
            "  schemas:\n" +
            "    events-value:\n" +
            "      type: \"object\"\n" +
            "      properties:\n" +
            "        id:\n" +
            "          type: \"string\"\n" +
            "        status:\n" +
            "          type: \"string\"\n" +
            "      required:\n" +
            "      - \"id\"\n" +
            "      - \"status\"\n" +
            "  messages:\n" +
            "    EventsMessage:\n" +
            "      payload:\n" +
            "        $ref: \"#/components/schemas/events-value\"\n" +
            "      contentType: \"application/json\"\n" +
            "      name: \"EventsMessage\"\n";

        String schema = "{" +
            "\"type\": \"object\"," +
            "\"properties\": " +
            "{" +
                "\"id\": {" +
                "\"type\": \"string\"" +
                "}," +
                "\"status\": {" +
                "\"type\": \"string\"" +
                "}" +
            "}," +
            "\"required\": [" +
                "\"id\"," +
                "\"status\"" +
            "]" +
            "}";

        ZillabaseStartCommand command = new ZillabaseStartCommand();
        KafkaTopicSchemaRecord record = new KafkaTopicSchemaRecord("events", "Events", "events-value", "json", schema);

        String spec = command.generateKafkaAsyncApiSpecs(new ZillabaseConfig(), List.of(record));
        assertEquals(expectedSpec, spec);
    }
}
