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
package io.aklivity.zillabase.cli.internal.commands.migration.diff;

import java.util.ArrayList;
import java.util.List;

import com.github.rvesse.airline.annotations.Command;

import io.aklivity.zillabase.cli.internal.commands.migration.ZillabaseMigrationCommand;
import io.aklivity.zillabase.cli.internal.migrations.ZillabaseMigrationFile;
import io.aklivity.zillabase.cli.internal.migrations.ZillabaseMigrationMetadata;

@Command(
    name = "diff",
    description = "Shows delta of SQL commands between explicit migrations and current state.")
public final class ZillabaseMigrationDiffCommand extends ZillabaseMigrationCommand
{
    @Override
    protected void invoke()
    {
        try
        {
            List<ZillabaseMigrationMetadata> appliedMigrations = helper.allAppliedMigrations();
            List<ZillabaseMigrationFile> localMigrations = helper.allMigrationFiles();

            List<ZillabaseMigrationFile> pending = findPendingMigrations(appliedMigrations, localMigrations);

            System.out.println("Pending migrations:");

            for (ZillabaseMigrationFile mf : pending)
            {
                System.out.println(" - " + mf.version() + " : " + mf.scriptName());
            }


        }
        catch (Exception ex)
        {
            ex.printStackTrace(System.err);
        }
    }

    private  List<ZillabaseMigrationFile> findPendingMigrations(
        List<ZillabaseMigrationMetadata> appliedMigrations,
        List<ZillabaseMigrationFile> localMigrations)
    {
        List<ZillabaseMigrationFile> pending = new ArrayList<>();

        List<String> appliedVersions = new ArrayList<>();
        for (ZillabaseMigrationMetadata mm : appliedMigrations)
        {
            appliedVersions.add(mm.version());
        }

        for (ZillabaseMigrationFile mf : localMigrations)
        {
            if (!appliedVersions.contains(mf.version()))
            {
                pending.add(mf);
            }
        }

        return pending;
    }
}
