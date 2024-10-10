<template>
  <q-layout view="lHh Lpr lFf" style="min-width: 400px">
    <q-drawer
      show-if-above
      :width="300"
      :breakpoint="500"
      bordered
    >
      <div class="absolute-top"  style="height: 150px; margin-top: 15px;">
        <div class="absolute-top" style="margin-bottom: 50px;">
          <q-btn
            class="text-weight-bold text-h4"
            flat color="primary"
            label="StreamPay"
            to="/main"
          />
        </div>
        <div style="margin-top: 100px">
          <div class="text-weight-bold float-right text-h6" style="padding-right: 10px; width: 222px; margin-top: 10px;">
            Hi, {{ name }}
          </div>
        </div>
      </div>

      <div style="margin-top: 240px; padding-left: 20px; padding-right: 20px;">
        <q-btn
          unelevated
          size="lg"
          color="primary"
          class="full-width text-white"
          label="Send or Request"
          rounded
          to="/payorrequest"
        />
      </div>

      <div style="margin-top: 40px; padding-left: 20px; padding-right: 20px;">
        <div class="text-h6">
          <b>${{ balance }}</b> in StreamPay
        </div>
      </div>

      <q-list class="text-h6" style="margin-top: 20px;">
        <q-item
          clickable
          v-ripple
          to="/request"
        >
          <q-item-section avatar>
            <q-icon size="36px" color="primary" name="request_quote" />
          </q-item-section>

          <q-item-section>
              <div>Requests <q-badge v-if="request > 0" rounded color="red" :label="request" /></div>
          </q-item-section>
        </q-item>

        <q-item
          clickable
          v-ripple
          to="/statement"
        >
          <q-item-section avatar>
            <q-icon size="36px" color="primary" name="insights" />
          </q-item-section>

          <q-item-section>Statement</q-item-section>
        </q-item>
      </q-list>

      <div class="absolute-bottom text-weight-bold" style="padding-left: 80px; padding-right: 80px; margin-bottom: 30px;">
        <q-btn
          size="10px"
          color="negative"
          class="full-width text-white"
          label="Logout"
          rounded
          @click="logout"
        />
      </div>
    </q-drawer>

    <q-page-container>
      <router-view />
    </q-page-container>
  </q-layout>
</template>

<script setup lang="ts">
import {onBeforeMount, onUnmounted, ref, unref, watch, computed} from 'vue';
import {streamingUrl} from 'boot/axios';
import {keycloak, user, SecureEventSource} from 'boot/main';

const request = ref(0);
const balance = ref(0);
const name = computed(() => (user?.firstName || 'User'))

let requestStream: SecureEventSource;
let balanceStream: SecureEventSource;

onBeforeMount(async () => {
  const isAuthenticated = async () => {
    if (unref(keycloak.authenticated)) {
      return true;
    }

    await keycloak.login({
        redirectUri: window.location.href,
    });

    return false;
  };

  if (!unref(keycloak.authenticated)) {
      await keycloak.init({ onLoad: 'login-required' });
  }

  return isAuthenticated();
});

function logout() {
  keycloak.logout({
    redirectUri: `${window.location.origin}/`
  });
}
function incRequest() {
  request.value++
}
function decRequests() {
  let currentRequests = request.value;
  currentRequests--;
  request.value = currentRequests < 0 ? 0 : currentRequests;
}
function updateBalance(newBalance: number) {
  balance.value = +newBalance.toFixed(2);
}

async function authenticatePage() {
  requestStream  = new SecureEventSource(`${streamingUrl}/streampay_payment_requests-stream`, {
    credentials: () => keycloak.token || ''
  });

  requestStream.addEventListener('delete', () => {
    decRequests();
  }, false);

  requestStream.onmessage = function () {
    incRequest();
  };

  balanceStream  = new SecureEventSource(`${streamingUrl}/streampay_balances-stream-identity`, {
    credentials: () => keycloak.token || ''
  });

  balanceStream.onmessage = function (event: MessageEvent) {
    const balance = JSON.parse(event.data);
    updateBalance(balance.balance);
  };

}

if (keycloak.authenticated)
{
  authenticatePage();
} else {
  watch(() => keycloak.authenticated ?? false, (newValue) => {
    if (newValue) {
      authenticatePage();
    }
  });
}

onUnmounted(() => {
  requestStream?.close();
  balanceStream?.close();
})

defineExpose({
  logout,
});
</script>
