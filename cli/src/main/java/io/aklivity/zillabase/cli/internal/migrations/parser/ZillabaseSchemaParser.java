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
package io.aklivity.zillabase.cli.internal.migrations.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.aklivity.zillabase.cli.internal.migrations.model.ZillabaseDatabaseSchema;
import io.aklivity.zillabase.cli.internal.migrations.model.ZillabaseMaterializedView;
import io.aklivity.zillabase.cli.internal.migrations.model.ZillabaseTable;
import io.aklivity.zillabase.cli.internal.migrations.model.ZillabaseZfunction;
import io.aklivity.zillabase.cli.internal.migrations.model.ZillabaseZtable;
import io.aklivity.zillabase.cli.internal.migrations.model.ZillabaseZview;

public final class ZillabaseSchemaParser
{
    private static final Pattern CREATE_QUERY_PATTERN = Pattern.compile(
        "(?i)^\\s*CREATE\\s+" +
        "(?:(?:OR|REPLACE|TEMP|UNLOGGED|GLOBAL|LOCAL)\\s+)*" +
        "((?:MATERIALIZED\\s+VIEW)|ZTABLE|TABLE|VIEW|FUNCTION|ZVIEW|ZFUNCTION)" +
        "\\s+" +
        "(?:IF\\s+NOT\\s+EXISTS\\s+)?" +
        "([^\\s(]+)");

    public void parseCreateStatements(
        ZillabaseDatabaseSchema schema,
        String sql)
    {
        List<String> statements = splitSQL(sql);
        for (String statement : statements)
        {
            Matcher matcher = CREATE_QUERY_PATTERN.matcher(statement);
            if (matcher.find())
            {
                String type = matcher.group(1);
                String name = matcher.group(2);
                addToSchema(schema, type, name, statement);
            }
        }
    }

    private void addToSchema(
        ZillabaseDatabaseSchema schema,
        String type,
        String name,
        String sql)
    {
        switch (type.toUpperCase())
        {
        case "ZFUNCTION":
            schema.addZfunction(new ZillabaseZfunction(name, sql));
            break;
        case "ZTABLE":
            schema.addZtable(new ZillabaseZtable(name, sql));
            break;
        case "ZVIEW":
            schema.addZview(new ZillabaseZview(name, sql));
            break;
        case "TABLE":
            schema.addTable(new ZillabaseTable(name, sql));
            break;
        case "MATERIALIZED VIEW":
            schema.addMaterializedView(new ZillabaseMaterializedView(name, sql));
            break;
        case "VIEW":
            schema.addView(new ZillabaseZview(name, sql));
            break;
        default:
            // log or handle unknown type
            break;
        }
    }

    public static List<String> splitSQL(
        String sql)
    {
        sql = removeSqlComments(sql);
        List<String> result = new ArrayList<>();
        StringBuilder command = new StringBuilder();
        boolean insideDollarBlock = false;

        String[] lines = sql.split("\\r?\\n");
        for (String line : lines)
        {
            if (line.contains("$$"))
            {
                insideDollarBlock = !insideDollarBlock;
            }
            command.append(line).append("\n");
            if (!insideDollarBlock && line.trim().endsWith(";"))
            {
                result.add(command.toString().trim());
                command.setLength(0);
            }
        }

        if (!command.isEmpty())
        {
            result.add(command.toString().trim());
        }

        return result;
    }

    public static String removeSqlComments(
        String sql)
    {
        return sql.replaceAll("(?s)/\\*.*?\\*/", "")
                  .replaceAll("--.*?(?=\\r?\\n|$)", "")
                  .trim();
    }
}
