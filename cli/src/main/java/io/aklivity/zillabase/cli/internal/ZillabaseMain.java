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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.github.rvesse.airline.Cli;
import com.github.rvesse.airline.parser.errors.ParseException;

import io.aklivity.zillabase.cli.internal.util.ZillabaseSystemPropertyUtil;

public final class ZillabaseMain
{
    public static void main(
        String[] args)
    {
        ZillabaseSystemPropertyUtil.initialize();
        Cli<Runnable> parser = new Cli<>(ZillabaseCli.class);

        try
        {
            parser.parse(args).run();
        }
        catch (ParseException ex)
        {
            System.out.format("%s\n\n", ex.getMessage());

            List<String> helpArgs = new ArrayList<>();
            helpArgs.add("help");
            helpArgs.addAll(Arrays.asList(args));
            parser.parse(helpArgs.toArray(String[]::new)).run();
        }
    }

    private ZillabaseMain()
    {
        // utility class
    }
}
