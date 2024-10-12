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
package io.aklivity.zillabase.cli.internal.commands.sso;

import java.net.URI;

import com.github.rvesse.airline.annotations.Command;

import io.aklivity.zillabase.cli.internal.commands.ZillabaseCommand;

public abstract class ZillabaseSsoCommand extends ZillabaseCommand
{
    protected static final String SSO_PATH = "sso";
    protected static final String SSO_ALIAS_PATH = "sso/%s";
    protected static final URI ADMIN_SERVER_DEFAULT = URI.create("http://localhost:7184/v1/");

    @Command(
        name = "help",
        hidden = true)
    public static final class Help<T> extends com.github.rvesse.airline.help.Help<T>
    {
        public Help()
        {
            this.command.add("sso");
        }
    }
}
