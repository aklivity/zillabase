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
package io.aklivity.zillabase.service.internal;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.rules.RuleChain.outerRule;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;

import io.aklivity.k3po.runtime.junit.annotation.Specification;
import io.aklivity.k3po.runtime.junit.rules.K3poRule;
import io.aklivity.zillabase.service.internal.server.ZillabaseServer;

public class ZillabaseServerIT
{
    private final K3poRule k3po = new K3poRule()
        .addScriptRoot("app", "io/aklivity/zillabase/service/internal/streams");

    private final TestRule timeout = new DisableOnDebug(new Timeout(10, SECONDS));

    @Rule
    public final TestRule chain = outerRule(k3po).around(timeout);

    private static ZillabaseServer server;

    @BeforeClass
    public static void init()
    {
        server = new ZillabaseServer();
        server.run();
    }

    @Test
    @Specification({
        "${app}/asyncapi.register/client",
        "${app}/asyncapi.register/server"})
    public void shouldRegisterAsyncapiSpec() throws Exception
    {
        k3po.finish();
    }

    @Test
    @Specification({
        "${app}/resolve.artifact.via.global.id/client",
        "${app}/resolve.artifact.via.global.id/server"})
    public void shouldResolveAsyncapiSpecById() throws Exception
    {
        k3po.finish();
    }

    @Test
    @Specification({
        "${app}/remove.artifact/client",
        "${app}/remove.artifact/server"})
    public void shouldRemoveAsyncapiSpec() throws Exception
    {
        k3po.finish();
    }

    @Test
    @Specification({
        "${app}/sso.add/client",
        "${app}/sso.add/server"})
    public void shouldAddIdentityProvider() throws Exception
    {
        k3po.finish();
    }
}
