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
  }, 18000);
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

interface SecureEventSourceInit extends EventSourceInit {
  credentials?: () => string;
}

class SecureEventSource extends EventTarget {
  private eventSource: EventSource | null = null;
  private lastEventId = "";
  private url: string;
  private eventSourceInit: SecureEventSourceInit | undefined;

  constructor(url: string, eventSourceInit?: SecureEventSourceInit) {
    super();
    this.url = url;
    this.eventSourceInit = eventSourceInit;

    this.initEventSource();
  }

  private initEventSource() {
    const accessToken = this.eventSourceInit?.credentials ? this.eventSourceInit?.credentials() : '';
    const lastEventIdQuery = this.lastEventId ? `&lastEventId=${this.lastEventId}` : '';
    const secureUrl = `${this.url}?access_token=${accessToken}${lastEventIdQuery}`;


    this.eventSource = new EventSource(secureUrl, this.eventSourceInit);

    this.eventSource.onopen = (event: Event) => this.dispatchEvent(new Event("open"));
    this.eventSource.onmessage = (event: MessageEvent) => {
      this.lastEventId = event.lastEventId;
      this.dispatchEvent(new MessageEvent("message", { data: event.data, lastEventId: event.lastEventId }));
    };
    this.eventSource.onerror = (event: Event) => {
      this.dispatchEvent(new ErrorEvent("error"));
      this.eventSource?.close();
      setTimeout(() => this.initEventSource(), 1000);
    };
  }

  close() {
    this.eventSource?.close();
  }

  addEventListener(type: string, listener: EventListenerOrEventListenerObject, options?: boolean | AddEventListenerOptions) {
    super.addEventListener(type, listener, options);
  }


  removeEventListener(type: string, listener: EventListenerOrEventListenerObject, options?: boolean | EventListenerOptions) {
    super.removeEventListener(type, listener, options);
  }
}

export { keycloak, user, SecureEventSource};
