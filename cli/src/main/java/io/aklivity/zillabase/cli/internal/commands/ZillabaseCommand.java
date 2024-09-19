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

import javax.inject.Inject;

import com.github.rvesse.airline.HelpOption;
import com.github.rvesse.airline.annotations.Option;

public abstract class ZillabaseCommand implements Runnable
{
    public static final String VERSION = ZillabaseCommand.class.getPackage().getImplementationVersion();

    @Inject
    public HelpOption<ZillabaseCommand> helpOption;

    @Option(name = { "--debug" })
    public Boolean debug = false;

    @Override
    public void run()
    {
        if (!helpOption.showHelpIfRequested())
        {
            invoke();
        }
    }

    protected abstract void invoke();
}
