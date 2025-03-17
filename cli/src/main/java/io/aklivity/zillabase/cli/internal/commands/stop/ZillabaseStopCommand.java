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
package io.aklivity.zillabase.cli.internal.commands.stop;

import java.io.File;
import java.io.IOException;

import com.github.rvesse.airline.annotations.Command;
import com.github.rvesse.airline.annotations.Option;

import io.aklivity.zillabase.cli.internal.commands.ZillabaseCommand;

@Command(
    name = "stop",
    description = "Stop containers for local development")
public final class ZillabaseStopCommand extends ZillabaseCommand
{
    @Option(name = {"--no-backup"},
        description = "Deletes all data volumes after stopping.",
        hidden = true)
    public boolean noBackup = false;

    @Override
    protected void invoke()
    {
        try
        {
            stopDockerCompose(noBackup);
        }
        catch (Exception e)
        {
            System.out.println("Failed to stop containers: " + e.getMessage());
        }
    }

    private void stopDockerCompose(
        boolean noBackup) throws IOException, InterruptedException
    {
        String[] commands = noBackup
            ? new String[] { "docker", "compose", "down", "--volumes" }
            : new String[] { "docker", "compose", "down" };

        ProcessBuilder builder = new ProcessBuilder(commands);
        builder.directory(new File(".docker"));
        builder.inheritIO();

        Process process = builder.start();
        int exitCode = process.waitFor();

        if (exitCode != 0)
        {
            throw new RuntimeException("Failed to stop docker compose");
        }
    }
}
