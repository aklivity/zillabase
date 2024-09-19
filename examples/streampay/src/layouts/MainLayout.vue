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
          <q-avatar size="60px" class="q-mb-sm" style="margin-left: 10px">
            <img :src="user.picture" referrerpolicy="no-referrer">
          </q-avatar>
          <div class="text-weight-bold float-right text-h6" style="padding-right: 10px; width: 222px; margin-top: 10px;">
            Hi, {{ user.name }}
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
import {defineComponent, ref, unref, watch} from 'vue';
import {useAuth0} from '@auth0/auth0-vue';
import {api, streamingUrl} from "boot/axios";
import {watchEffectOnceAsync} from "@auth0/auth0-vue/src/utils";
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
    const auth0 = useAuth0();

    return {
      auth0,
      user: auth0.user
    }
  },
  async mounted() {
    const incRequests =  this.incRequest;
    const decRequests =  this.decRequests;
    const updateBalance =  this.updateBalance;
    const auth0 = this.auth0;
    async function authenticatePage() {
      const accessToken = await auth0.getAccessTokenSilently();

      const requestStream = new EventSource(`${streamingUrl}/streampay-request-payment?access_token=${accessToken}`);

      requestStream.addEventListener('delete', (event: MessageEvent) => {
        decRequests();
      }, false);

      requestStream.onmessage = function () {
        incRequests();
      };

      const balanceStream = new EventSource(`${streamingUrl}/streampay-balances?access_token=${accessToken}`);

      balanceStream.onmessage = function (event: MessageEvent) {
        const balance = JSON.parse(event.data);
        updateBalance(balance.balance);
      };

      const authorization = { Authorization: `Bearer ${accessToken}` };
      const userId = auth0.user.value.sub;
      await api.put(`${streamingUrl}/streampay-users/${userId}`, {
        'id': userId,
        'name': auth0.user.value.name,
        'username': auth0.user.value.nickname
      }, {
        headers: {
          'Idempotency-Key': v4(),
          ...authorization
        }
      })
    }

    if (auth0.isAuthenticated.value)
    {
      await authenticatePage();
    } else {
      watch(this.auth0.isAuthenticated, authenticatePage);
    }
  },
  methods: {
     logout() {
      this.auth0.logout({
        returnTo: `${window.location.origin}/`
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
      if (unref(this.auth0.isAuthenticated)) {
        return true;
      }

      await this.auth0.loginWithRedirect({
        appState: { target: '/' }
      });

      return false;
    };

    if (!unref(this.auth0.isLoading)) {
      return isAuthenticated();
    }

    await watchEffectOnceAsync(() => !unref(this.auth0.isLoading));

    return isAuthenticated();
  }

});
</script>
