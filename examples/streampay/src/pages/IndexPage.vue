<template>
  <q-page class="row items-center justify-evenly">
    <div class="q-pa-md row items-start q-gutter-md">
      <q-card square class="shadow-24" style="text-align: center; width:400px;height:340px;">
        <q-card-section>
          <div class="text-weight-bold text-h3 text-primary">StreamPay</div>
        </q-card-section>

        <q-separator/>

        <q-card-section>
          “StreamPay” is a web-based payment app where users make and request payments.
        </q-card-section>

        <q-separator/>

        <q-card-actions class="q-px-lg">
          <q-btn
            unelevated
            size="lg"
            color="primary"
            class="full-width text-white"
            label="Login"
            @click="login"
          />
        </q-card-actions>

        <q-card-actions class="q-px-lg" align="center">
          Powered by
          <q-avatar style="margin-left: 10px" size="md">
            <img src="../assets/zilla.png">
          </q-avatar>
          +
          <q-avatar size="md" style="margin-left: 10px">
            <img src="../assets/redpanda.svg">
          </q-avatar>
        </q-card-actions>
      </q-card>
    </div>
  </q-page>
</template>

<script lang="ts">
import {defineComponent, unref} from 'vue';
import { useAuth0 } from "@auth0/auth0-vue";
import {watchEffectOnceAsync} from "@auth0/auth0-vue/src/utils";

export default defineComponent({
  name: 'IndexPage',
  setup () {
    const auth0 = useAuth0();
    return {
      auth0: auth0
    }
  },
  async beforeCreate() {
    const fn = async () => {
      if (!unref(this.auth0.isAuthenticated)) {
        return true;
      }

      this.$router.push({ path: '/main' });

      return true;
    };

    if (!unref(this.auth0.isLoading)) {
      return fn();
    }

    await watchEffectOnceAsync(() => !unref(this.auth0.isLoading));

    return fn();
  },
  methods: {
    login() {
      this.auth0.loginWithRedirect();
    }
  }
});
</script>
