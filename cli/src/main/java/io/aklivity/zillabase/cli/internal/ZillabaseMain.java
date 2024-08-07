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

import com.github.rvesse.airline.Cli;

public final class ZillabaseMain
{
    public static void main(
        String[] args)
    {
        System.setProperty("jdk.httpclient.allowRestrictedHeaders", "Content-Length");
        System.setProperty("org.slf4j.simpleLogger.log.org.apache.kafka.clients.admin.AdminClientConfig", "error");
        System.setProperty("org.slf4j.simpleLogger.log.org.apache.kafka.clients.admin.internals.AdminMetadataManager", "error");
        System.setProperty("org.slf4j.simpleLogger.log.org.apache.kafka.common.utils.AppInfoParser", "error");

        Cli<Runnable> parser = new Cli<>(ZillabaseCli.class);
        parser.parse(args).run();
    }

    private ZillabaseMain()
    {
        // utility class
    }
}
