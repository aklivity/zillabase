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
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
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

    public void createAndAssignScope(
        String scopeName)
    {
        createClientScopeIfNotExists(scopeName);
        addScopeToAllUsers(scopeName);
    }

    public void createClientScopeIfNotExists(
        String scopeName)
    {
        RealmResource realmResource = keycloak.realm(config.realm());

        List<ClientScopeRepresentation> allScopes = realmResource.clientScopes().findAll();

        boolean scopeExists = allScopes.stream()
            .anyMatch(s -> s.getName().equals(scopeName));

        if (!scopeExists)
        {
            ClientScopeRepresentation newScope = new ClientScopeRepresentation();
            newScope.setName(scopeName);

            realmResource.clientScopes().create(newScope);
            System.out.println("Created new client scope: " + scopeName);
        }
        else
        {
            System.out.println("Client scope already exists: " + scopeName);
        }
    }

    private void addScopeToAllUsers(
        String scopeName)
    {
        final String realm = config.realm();
        final String clientId = "streampay";
        RealmResource realmResource = keycloak.realm(realm);

        List<ClientRepresentation> clients = realmResource.clients().findByClientId(clientId);
        if (clients.isEmpty())
        {
            System.err.printf("Client '%s' not found in realm '%s'%n", clientId, realm);
            return;
        }
        ClientRepresentation foundClient = clients.get(0);
        ClientResource clientResource = realmResource.clients().get(foundClient.getId());

        List<ClientScopeRepresentation> allScopes = realmResource.clientScopes().findAll();

        ClientScopeRepresentation match = allScopes.stream()
            .filter(s -> scopeName.equals(s.getName()))
            .findFirst()
            .orElse(null);

        if (match == null)
        {
            System.err.printf("Client scope '%s' not found in realm '%s'%n", scopeName, realm);
            return;
        }

        clientResource.addDefaultClientScope(match.getId());

        System.out.printf("Attached client scope '%s' (%s) to client '%s' (%s)%n",
                          scopeName, match.getId(), clientId, foundClient.getId());
    }
}
