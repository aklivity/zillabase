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
package io.aklivity.zillabase.cli.internal.commands.migration.apply;

import com.github.rvesse.airline.annotations.Command;

import io.aklivity.zillabase.cli.internal.commands.migration.ZillabaseMigrationCommand;
import io.aklivity.zillabase.cli.internal.migrations.model.ZillabaseMigrationFile;

@Command(
    name = "apply",
    description = "Applies new migrations files")
public final class ZillabaseMigrationApplyCommand extends ZillabaseMigrationCommand
{
    @Override
    protected void invoke()
    {
        try
        {
            migrationApply.apply();

            String patchScript =  migrationDiff.databaseDiff();
            if (!patchScript.isEmpty())
            {
                saveNewMigrationFile("manual_changes_catchup", patchScript);

                ZillabaseMigrationFile newMigration = migrationDiff.unappliedFiles().get(0);

                migrationDiff.record(newMigration);
            }

        }
        catch (Exception ex)
        {
            ex.printStackTrace(System.err);
        }
    }
}
