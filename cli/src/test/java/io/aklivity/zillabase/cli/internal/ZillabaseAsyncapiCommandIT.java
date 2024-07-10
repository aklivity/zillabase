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
import static org.junit.rules.RuleChain.outerRule;

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
}
