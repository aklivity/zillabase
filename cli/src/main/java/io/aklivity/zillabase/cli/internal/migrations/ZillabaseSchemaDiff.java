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
package io.aklivity.zillabase.cli.internal.migrations;

import java.util.ArrayList;
import java.util.List;

public class ZillabaseSchemaDiff
{
    private final List<String> differences = new ArrayList<>();

    public void addDifference(
        String diff)
    {
        differences.add(diff);
    }

    public boolean isEmpty()
    {
        return differences.isEmpty();
    }

    public String generatePatchScript()
    {
        StringBuilder sb = new StringBuilder();

        if (!differences.isEmpty())
        {
            for (String diff : differences)
            {
                sb.append(diff).append("\n\n");
            }
        }


        return sb.toString();
    }
}
