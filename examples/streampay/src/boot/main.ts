import { boot } from 'quasar/wrappers';
import Keycloak, { KeycloakInstance, KeycloakProfile } from 'keycloak-js';

const keycloak = new Keycloak({
  url: 'http://localhost:8180/',
  realm: 'zillabase',
  clientId: 'streampay',
});

let user: KeycloakProfile | null = null;

async function createRefreshTokenTimer(keycloak: KeycloakInstance) {
  setInterval(() => {
    keycloak.updateToken(60).then((refreshed: boolean) => {
      if (refreshed) {
        console.log('Token refreshed', refreshed);
      } else {
        console.log('Token not refreshed, it is still valid');
      }
    }).catch(() => {
      console.log('Failed to refresh token');
    });
  }, 6000);
}

export default boot(({ app }) => {
  return new Promise<void>(resolve => {
    keycloak.init({
      onLoad: 'login-required',
      checkLoginIframe: false,
      enableLogging: true,
    }).then(async (authenticated: boolean) => {
      if (authenticated) {
        console.log('Authenticated');
        await keycloak.loadUserProfile().then(userProfile => {
          user = userProfile;
        }).catch(err => {
          console.error('Failed to load user profile:', err);
        });
        await createRefreshTokenTimer(keycloak);
        resolve();
      } else {
        console.log('Not authenticated');
      }
    }).catch((error: Error) => {
      console.log('Authentication failure', error);
    });

    app.config.globalProperties.$keycloak = keycloak as Keycloak;
    app.config.globalProperties.$user = user as KeycloakProfile;
  });
});

export { keycloak, user };
