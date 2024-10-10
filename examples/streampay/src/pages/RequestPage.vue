<template>
  <q-page class="items-center" style="margin-left: 12%; margin-right: 12%; margin-top: 70px;">
    <div class="items-center text-primary text-h4" style="margin-left: 40%; margin-bottom: 60px;">
      Requests
    </div>
    <div class="q-pa-md row items-start q-gutter-md">
      <q-card v-for="req in requests" :key="req.id" class="full-width">
        <q-card-section class="bg-white">

          <div class="text-h6"><b>{{ req.from_username }}</b> requested <b> ${{ req.amount?.toFixed(2) || `0` }}</b>
          </div>
          <div class="text-subtitle2">Risk:</div>
          <p>{{ req.risk }} | {{ req.summary }}</p>
          <div class="text-subtitle2">Note:</div>
          <p>{{ req.notes }}</p>
        </q-card-section>

        <q-separator />

        <q-card-actions>
          <q-btn @click="pay(req.id)" class="bg-primary text-white">Pay</q-btn>
          <q-btn @click="reject(req.id)" flat>Reject</q-btn>
        </q-card-actions>
      </q-card>

    </div>
  </q-page>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';
import { api } from 'boot/axios';
import { keycloak } from 'boot/main';

const requests = ref([] as any);

async function readRequests() {
  const accessToken = keycloak.token;
  const authorization = { Authorization: `Bearer ${accessToken}` };

  await api.get('/streampay_payment_requests', {
    headers: {
      ...authorization
    }
  })
    .then((response) => {
      requests.value = response.data
    });
  api.get('/streampay_payment_risk_assessment', {
    headers: {
      ...authorization
    }
  })
    .then((response) => {
      let riskById = response.data?.reduce((acc: any, risk: any) => {acc[risk.id] = risk; return acc;}, {});
      let withRisk = requests.value.map((r: any) => ({ ...riskById[r.id], ...r }));
      requests.value = withRisk;
    });
}

function pay(id: string) {
  console.log(id);
}

function reject(id: string) {
  console.log(id);
}

if (keycloak.authenticated) {
  readRequests();
} else {
  watch(() => keycloak.authenticated ?? false, (newValue) => {
    if (newValue) {
      readRequests();
    }
  });
}

defineExpose({
  pay,
  reject,
});
</script>
