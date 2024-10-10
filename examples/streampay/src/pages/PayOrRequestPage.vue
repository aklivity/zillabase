<template>
  <q-page class="items-center" style="margin-left: 12%; margin-right: 12%; margin-top: 70px;">
    <div class="text-center text-primary text-h5" style="margin-left: 35%; margin-right: 35%; margin-top: 20px;">
      Pay & Request
    </div>
    <PayOrRequestForm :request-id="requestId" />
  </q-page>
</template>

<script setup lang="ts">
import PayOrRequestForm from 'components/PayOrRequestForm.vue';
import {useRoute} from 'vue-router';

const route = useRoute();
const requestId = route.params.requestId?.toString();
</script>
