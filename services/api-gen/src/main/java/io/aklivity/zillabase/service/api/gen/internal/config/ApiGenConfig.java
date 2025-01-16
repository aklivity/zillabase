package io.aklivity.zillabase.service.api.gen.internal.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.gen")
public class ApiGenConfig
{
    @Value("${admin.http.port:7184}")
    private int adminHttpPort;

    @Value("${risingwave.db:dev}")
    private String risingwaveDb;

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String kafkaBootstrapServers;

    @Value("${KARAPACE_URL:http://karapace.zillabase.dev:8081}")
    private String karapaceUrl;

    @Value("${apicurio.registry.url}")
    private String apicurioUrl;

    @Value("${apicurio.group.id}")
    private String apicurioGroupId;

    @Value("${keycloak.url:http://localhost:8180}")
    private String keycloakUrl;

    @Value("${keycloak.realm}")
    private String keycloakRealm;

    @Value("${keycloak.audience:account}")
    private String keycloakAudience;

    @Value("${keycloak.jwks.url:http://keycloak.zillabase.dev:8180/realms/%s/protocol/openid-connect/certs}")
    private String keycloakJwksUrl;

    public int adminHttpPort()
    {
        return adminHttpPort;
    }

    public String risingwaveDb()
    {
        return risingwaveDb;
    }

    public String kafkaBootstrapServers()
    {
        return kafkaBootstrapServers;
    }

    public String karapaceUrl()
    {
        return karapaceUrl;
    }

    public String apicurioUrl()
    {
        return apicurioUrl;
    }

    public String apicurioGroupId()
    {
        return apicurioGroupId;
    }

    public String keycloakUrl()
    {
        return keycloakUrl;
    }

    public String keycloakRealm()
    {
        return keycloakRealm;
    }

    public String keycloakAudience()
    {
        return keycloakAudience;
    }

    public String keycloakJwksUrl()
    {
        return keycloakJwksUrl;
    }
}
