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

import java.util.List;

public final class ZillabaseAuthUserRequestView
{
    public String username;
    public String email;
    public String firstName;
    public String lastName;
    public boolean enabled;
    public List<Credential> credentials;

    public static class Credential
    {
        public String type;
        public String value;
        public boolean temporary;

        public Credential(
            String value)
        {
            this.type = "password";
            this.value = value;
            this.temporary = false;
        }
    }

    public static ZillabaseAuthUserRequestView of(
        ZillabaseAuthUserRequest user)
    {
        ZillabaseAuthUserRequestView view = new ZillabaseAuthUserRequestView();
        view.username = user.username;
        view.email = user.email;
        view.firstName = user.firstName;
        view.lastName = user.lastName;
        view.enabled = true;
        view.credentials = List.of(new Credential(user.password));
        return view;
    }
}
