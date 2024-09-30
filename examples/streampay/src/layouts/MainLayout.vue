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
            @click="this.$router.push({ path: '/main' })"
          />
        </div>
        <div style="margin-top: 100px">
          <div class="text-weight-bold float-right text-h6" style="padding-right: 10px; width: 222px; margin-top: 10px;">
            Hi, {{ user.firstName }}
          </div>
        </div>
      </div>

      <div style="margin-top: 240px; padding-left: 20px; padding-right: 20px;">
        <q-btn
          unelevated
          size="lg"
          color="primary"
          class="full-width text-white"
          label="Pay or Request"
          rounded
          @click="this.$router.push({ path: '/payorrequest' })"
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
          @click="this.$router.push({ path: '/request' })"
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
          @click="this.$router.push({ path: '/statement' })"
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

<script lang="ts">
import {defineComponent, unref, watch} from 'vue';
import {api, streamingUrl} from "boot/axios";
import {keycloak, user} from 'boot/main';
import {v4} from "uuid";

export default defineComponent({
  name: 'MainLayout',
  data() {
    return {
      request: 0,
      balance: 0
    }
  },
  setup () {
    return {
      keycloak,
      user,
    }
  },
  async mounted() {
    const userId = this.user?.id;
    const firstname = this.user?.firstName;
    const username = this.user?.username;
    const incRequests =  this.incRequest;
    const decRequests =  this.decRequests;
    const updateBalance =  this.updateBalance;
    async function authenticatePage() {
      const accessToken = keycloak.token;
      const requestStream = new EventSource(`${streamingUrl}/streampay_payment_requests-stream?access_token=${accessToken}`);

      requestStream.addEventListener('delete', () => {
        decRequests();
      }, false);

      requestStream.onmessage = function () {
        incRequests();
      };

      const balanceStream = new EventSource(`${streamingUrl}/streampay_balances-stream-identity?access_token=${accessToken}`);

      balanceStream.onmessage = function (event: MessageEvent) {
        const balance = JSON.parse(event.data);
        updateBalance(balance.balance);
      };

      const authorization = { Authorization: `Bearer ${accessToken}` };

      await api.put(`${streamingUrl}/streampay_users/${userId}`, {
        'id': userId,
        'name': firstname,
        'username': username
      }, {
        headers: {
          'Idempotency-Key': v4(),
          ...authorization
        }
      })
    }

    if (keycloak.authenticated)
    {
      await authenticatePage();
    } else {
      watch(() => keycloak.authenticated ?? false, (newValue) => {
        if (newValue) {
          authenticatePage();
        }
      });
    }
  },
  methods: {
     logout() {
      keycloak.logout({
        redirectUri: `${window.location.origin}/`
      });
    },
    incRequest() {
      this.request++
    },
    decRequests() {
      let currentRequests = this.request;
      currentRequests--;
      this.request = currentRequests < 0 ? 0 : currentRequests;
    },
    updateBalance(newBalance: number) {
      this.balance = +newBalance.toFixed(2);
    },
  },
  async beforeCreate() {
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
  }

});
</script>
