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
        if (differences.isEmpty())
        {
            return "-- No differences";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("-- Auto-generated patch script\n");
        for (String diff : differences)
        {
            sb.append("-- ").append(diff).append("\n");
        }

        return sb.toString();
    }
}
