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

import com.github.rvesse.airline.annotations.Cli;
import com.github.rvesse.airline.annotations.Group;
import com.github.rvesse.airline.help.Help;

import io.aklivity.zillabase.cli.internal.commands.asyncapi.add.ZillabaseAsyncapiAddCommand;
import io.aklivity.zillabase.cli.internal.commands.asyncapi.list.ZillabaseAsyncapiListCommand;
import io.aklivity.zillabase.cli.internal.commands.asyncapi.remove.ZillabaseAsyncapiRemoveCommand;
import io.aklivity.zillabase.cli.internal.commands.config.add.ZillabaseConfigAddCommand;
import io.aklivity.zillabase.cli.internal.commands.config.list.ZillabaseConfigListCommand;
import io.aklivity.zillabase.cli.internal.commands.config.remove.ZillabaseConfigRemoveCommand;
import io.aklivity.zillabase.cli.internal.commands.init.ZillabaseInitCommand;
import io.aklivity.zillabase.cli.internal.commands.sso.add.ZillabaseSsoAddCommand;
import io.aklivity.zillabase.cli.internal.commands.sso.list.ZillabaseSsoListCommand;
import io.aklivity.zillabase.cli.internal.commands.sso.remove.ZillabaseSsoRemoveCommand;
import io.aklivity.zillabase.cli.internal.commands.start.ZillabaseStartCommand;
import io.aklivity.zillabase.cli.internal.commands.stop.ZillabaseStopCommand;

@Cli(name = "zillabase",
    description = "Zillabase CLI",
    defaultCommand = Help.class,
    groups =
    {
        @Group(
            name = "asyncapi",
            description = "AsyncAPI specification",
            defaultCommand = Help.class,
            commands =
            {
                ZillabaseAsyncapiAddCommand.class,
                ZillabaseAsyncapiListCommand.class,
                ZillabaseAsyncapiRemoveCommand.class
            }),
        @Group(
            name = "config",
            description = "Zilla Config",
            defaultCommand = Help.class,
            commands =
            {
                ZillabaseConfigAddCommand.class,
                ZillabaseConfigListCommand.class,
                ZillabaseConfigRemoveCommand.class
            }),
        @Group(
            name = "sso",
            description = "Identity Provider",
            defaultCommand = Help.class,
            commands =
            {
                ZillabaseSsoAddCommand.class,
                ZillabaseSsoListCommand.class,
                ZillabaseSsoRemoveCommand.class
            })
    },
    commands =
    {
        Help.class,
        ZillabaseInitCommand.class,
        ZillabaseStartCommand.class,
        ZillabaseStopCommand.class
    })
public final class ZillabaseCli
{
    private ZillabaseCli()
    {
        // utility class
    }
}
