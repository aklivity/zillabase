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
package io.aklivity.zillabase.service.api.gen.internal.component;

import java.util.Collections;
import java.util.List;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.stereotype.Component;

import io.aklivity.zillabase.service.api.gen.internal.config.KeycloakConfig;

@Component
public class KeycloakHelper
{
    private final KeycloakConfig config;
    private final Keycloak keycloak;

    public KeycloakHelper(
        KeycloakConfig config,
        Keycloak keycloak)
    {
        this.config = config;
        this.keycloak = keycloak;
    }

    public void createAndAssignScope(String scopeName)
    {
        createRoleIfNotExists(scopeName);
        addRoleToAllUsers(scopeName);
    }

    private void createRoleIfNotExists(
        String scopeName)
    {
        RealmResource realmResource = keycloak.realm(config.realm());

        List<RoleRepresentation> existingRoles = realmResource.roles().list();
        boolean roleExists = existingRoles.stream()
                .anyMatch(r -> r.getName().equals(scopeName));

        System.out.println("Existing roles:");
        realmResource.roles().list().forEach(r -> System.out.println(r.getName()));

        if (!roleExists)
        {
            RoleRepresentation newRole = new RoleRepresentation();
            newRole.setName(scopeName);

            realmResource.roles().create(newRole);
            System.out.println("Created new realm role: " + scopeName);
        }
        else
        {
            System.out.println("Role already exists: " + scopeName);
        }
    }

    private void addRoleToAllUsers(
        String scopeName)
    {
        RealmResource realmResource = keycloak.realm(config.realm());
        RoleRepresentation roleToAdd = realmResource.roles()
                .get(scopeName)
                .toRepresentation();

        List<UserRepresentation> allUsers = realmResource.users().list();

        for (UserRepresentation user : allUsers)
        {
            try
            {
                realmResource.users()
                        .get(user.getId())
                        .roles()
                        .realmLevel()
                        .add(Collections.singletonList(roleToAdd));

                System.out.printf("Added role '%s' to user '%s' (%s)%n",
                                  scopeName, user.getUsername(), user.getId());
            }
            catch (Exception e)
            {
                System.err.printf("Failed to add role '%s' to user '%s' (%s): %s%n",
                                  scopeName, user.getUsername(), user.getId(), e.getMessage());
            }
        }
    }
}
