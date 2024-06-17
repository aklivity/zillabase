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
import com.github.rvesse.airline.help.Help;

@Cli(name = "zillabase",
    description = "Zillabase CLI",
    defaultCommand = Help.class,
    commands =
    {
        Help.class
    })
public final class ZillabaseCli
{
    private ZillabaseCli()
    {
        // utility class
    }
}
