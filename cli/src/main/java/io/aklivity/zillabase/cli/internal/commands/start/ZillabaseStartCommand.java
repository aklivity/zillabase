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
package io.aklivity.zillabase.cli.internal.commands.start;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.github.rvesse.airline.annotations.Command;

import io.aklivity.zillabase.cli.internal.commands.ZillabaseCommand;

@Command(
    name = "start",
    description = "Start containers for local development")
public final class ZillabaseStartCommand extends ZillabaseCommand
{
    public String kafkaSeedFilePath = "zillabase/seed-kafka.yaml";

    @Override
    protected void invoke()
    {
        try
        {
            unpackResourcesDocker();
            copyMigrations();
            copyJavaFunctions();
            runDockerCompose();
            printExposedEndpoints();
        }
        catch (Exception e)
        {
            System.out.println("Failed to start containers: " + e.getMessage());
        }
    }

    private void unpackResourcesDocker() throws IOException
    {
        Path target = Paths.get("/tmp/zillabase-docker");

        if (Files.exists(target))
        {
            Files.walk(target)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
        }

        try (ZipFile zipFile = new ZipFile(getClass().getProtectionDomain().getCodeSource().getLocation().getPath()))
        {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements())
            {
                ZipEntry entry = entries.nextElement();
                if (entry.getName().startsWith("docker/"))
                {
                    Path entryDestination = target.resolve(entry.getName().substring("docker/".length()));
                    if (entry.isDirectory())
                    {
                        Files.createDirectories(entryDestination);
                    }
                    else
                    {
                        try (InputStream in = zipFile.getInputStream(entry))
                        {
                            Files.copy(in, entryDestination, REPLACE_EXISTING);
                        }
                    }
                }
            }
        }
    }

    private void copyMigrations() throws IOException
    {
        Path source = Paths.get("zillabase/migrations");
        Path target = Paths.get("/tmp/zillabase-docker/volumes/db/migrations");

        Files.createDirectories(target);

        Files.walk(source)
            .filter(Files::isRegularFile)
            .forEach(file ->
            {
                try
                {
                    Files.copy(file, target.resolve(source.relativize(file)), REPLACE_EXISTING);
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            });
    }

    private void copyJavaFunctions() throws IOException
    {
        Path source = Paths.get("zillabase/functions/java/target");
        Path target = Paths.get("/tmp/zillabase-docker/volumes/functions/java");

        if (Files.exists(source))
        {
            Files.walk(source)
                .filter(f -> f.toString().endsWith(".jar"))
                .forEach(file ->
                {
                    try
                    {
                        Files.copy(file, target.resolve(source.relativize(file)), REPLACE_EXISTING);
                    }
                    catch (IOException e)
                    {
                        System.out.println("Failed to copy: " + file);
                    }
                });
        }
    }

    private void runDockerCompose() throws IOException, InterruptedException
    {
        ProcessBuilder builder = new ProcessBuilder("docker", "compose", "up", "-d");
        builder.directory(new File("/tmp/zillabase-docker"));
        builder.inheritIO();
        Process process = builder.start();
        int exitCode = process.waitFor();
        if (exitCode != 0)
        {
            throw new RuntimeException("Failed to run docker-compose");
        }
    }

    private void printExposedEndpoints()
    {
        int studioPort = 7194;

        String studioUrl = "Studio UI: http://localhost:%d".formatted(studioPort);

        int maxLength = studioUrl.length();
        String border = "#".repeat(maxLength + 4);

        System.out.println(border);
        System.out.printf("# %-" + maxLength + "s #\n", studioUrl);
        System.out.println(border);
    }
}
