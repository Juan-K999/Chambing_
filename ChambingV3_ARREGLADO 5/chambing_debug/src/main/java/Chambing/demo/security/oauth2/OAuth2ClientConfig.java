package Chambing.demo.security.oauth2;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;

/**
 * Registro EXPLICITO del cliente OAuth2 de Google.
 *
 * Por que por codigo y no solo por application.properties:
 *   El registro automatico de Spring Boot lee TODAS las propiedades
 *   "spring.security.oauth2.client.registration.*". Si quedaba un proveedor
 *   a medias (p.ej. github con client-id placeholder), la construccion del
 *   ClientRegistrationRepository entero podia fallar y dejar a Google tambien
 *   sin registrar — sintoma: /oauth2/authorization/google rebota a /login.
 *
 *   Definiendo el bean nosotros mismos:
 *     - Solo existe Google. GitHub no puede romper nada.
 *     - No depende de que el IDE recompile el .properties.
 *     - El flujo /oauth2/authorization/google queda garantizado.
 *
 * Las credenciales se siguen leyendo del application.properties para no
 * hardcodearlas aqui. Si quieres añadir GitHub mas adelante, agrega otro
 * ClientRegistration a este repositorio.
 */
@Configuration
public class OAuth2ClientConfig {

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String googleClientSecret;

    @Bean
    public ClientRegistrationRepository clientRegistrationRepository() {
        return new InMemoryClientRegistrationRepository(googleClientRegistration());
    }

    /**
     * Cliente Google. CommonOAuth2Provider.GOOGLE ya trae configuradas las URLs
     * de autorizacion, token, userinfo y el scope base; solo añadimos credenciales.
     */
    private ClientRegistration googleClientRegistration() {
        return CommonOAuth2Provider.GOOGLE
                .getBuilder("google")
                .clientId(googleClientId)
                .clientSecret(googleClientSecret)
                // scope explicito: openid + perfil + email (email es imprescindible
                // para crear el Usuario en Mongo)
                .scope("openid", "profile", "email")
                .build();
    }
}
