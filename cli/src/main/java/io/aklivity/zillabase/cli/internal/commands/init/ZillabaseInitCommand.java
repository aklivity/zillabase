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
package io.aklivity.zillabase.cli.internal.commands.init;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.github.rvesse.airline.annotations.Command;

import io.aklivity.zillabase.cli.internal.commands.ZillabaseCommand;

@Command(
    name = "init",
    description = "Initialize a local project")
public final class ZillabaseInitCommand extends ZillabaseCommand
{
    @Override
    protected void invoke()
    {
        Path configPath = Paths.get("zillabase/config.yaml");
        Path seedPath = Paths.get("zillabase/seed.sql");

        init:
        try
        {
            if (Files.exists(configPath))
            {
                System.err.format("Failed to create config file: %s: file exists\n", configPath);
                break init;
            }

            Files.createDirectories(configPath.getParent());
            Files.writeString(configPath, "# config\n");

            if (!Files.exists(seedPath))
            {
                Files.writeString(seedPath, "# seed\n");
            }

            System.out.format("Finished zillabase init\n");
        }
        catch (IOException ex)
        {
            ex.printStackTrace(System.err);
        }
    }
}
