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
package io.aklivity.zillabase.service.internal.model;

public final class ZillabaseAuthSsoRequestView
{
    public String alias;
    public String providerId;
    public boolean enabled;
    public Config config;

    public static final class Config
    {
        public String clientId;
        public String clientSecret;

        public Config(
            String clientId,
            String clientSecret)
        {
            this.clientId = clientId;
            this.clientSecret = clientSecret;
        }
    }

    public static ZillabaseAuthSsoRequestView of(
        ZillabaseAuthSsoRequest sso)
    {
        ZillabaseAuthSsoRequestView view = new ZillabaseAuthSsoRequestView();
        view.alias = sso.alias;
        view.providerId = sso.providerId;
        view.enabled = true;
        view.config = new Config(sso.clientId, sso.secret);
        return view;
    }
}
