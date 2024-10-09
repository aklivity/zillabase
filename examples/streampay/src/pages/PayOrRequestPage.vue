<template>
  <q-page class="items-center" style="margin-left: 12%; margin-right: 12%; margin-top: 70px;">
    <div class="text-center text-primary text-h5" style="margin-left: 35%; margin-right: 35%; margin-top: 20px;">
      Pay & Request
    </div>
    <PayOrRequestForm :request-id="requestId" />
  </q-page>
</template>

<script lang="ts">
import {defineComponent} from 'vue';
import PayOrRequestForm from 'components/PayOrRequestForm.vue';
import {useRoute} from 'vue-router';
import {keycloak} from 'boot/main';

export default defineComponent({
  name: 'PayOrRequestPage',
  components: { PayOrRequestForm },
  setup() {
    const route = useRoute();
    const requestId = route.params.requestId?.toString();
    return {
      keycloak,
      requestId
    };
  }
})
</script>
