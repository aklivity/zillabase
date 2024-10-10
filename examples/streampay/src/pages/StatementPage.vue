<template>
  <q-page class="items-center" style="margin-left: 12%; margin-right: 12%; margin-top: 70px;">
    <div class="items-center text-primary text-h4" style="margin-left: 40%; margin-bottom: 60px;">
      Statement
    </div>
    <div class="q-pa-md">
      <div class="row">
        <div class="col">
          <div class="text-h6">Total Transaction: {{ totalTransaction }}</div>
        </div>
        <div class="col">
          <div class="text-h6">Average Transaction Amount: {{ averageTransaction }}</div>
        </div>
      </div>
      <div style="margin-top: 20px;" class="row items-center text-primary text-h6">
        Balance History
      </div>
      <div class="row">
        <div class="col">
          <apexchart type="line" :options="options" :series="balanceSeries"></apexchart>
        </div>
      </div>
    </div>
  </q-page>
</template>

<script setup lang="ts">
import { ref, watch, onUnmounted, onMounted } from 'vue';
import { streamingUrl } from 'boot/axios';
import { keycloak } from 'boot/main';

const balanceSeries = ref([{
  name: 'Balance',
  data: [] as any
}]);
let balanceStream: EventSource;
let totalTransactionStream: EventSource;
let averageTransactionStream: EventSource;
const totalTransaction = ref(0);
const averageTransaction = ref(0);


function updateBalance(newBalance: number, timestamp: number) {
  let balance = { x: new Date(timestamp).toLocaleString(), y: newBalance };
  if (balanceSeries.value[0].data.length > 20) {
    balanceSeries.value[0].data.pop();
    balanceSeries.value[0].data.unshift(balance);
  } else {
    balanceSeries.value[0].data.push(balance);
  }
}
function updateTotalTransactionBalance(total: number) {
  totalTransaction.value = total;
}
function updateAverageTransactionBalance(average: number) {
  averageTransaction.value = +Math.abs(average).toFixed(2);
}


async function readStatement() {
  const accessToken = keycloak.token;

  balanceStream = new EventSource(`${streamingUrl}/streampay_balance_histories-stream?access_token=${accessToken}`);

  balanceStream.onmessage = function (event: MessageEvent) {
    const balance = JSON.parse(event.data);
    updateBalance(balance.balance, balance.timestamp);
  };

  totalTransactionStream = new EventSource(`${streamingUrl}/total-transactions?access_token=${accessToken}`);

  totalTransactionStream.onmessage = function (event: MessageEvent) {
    const totalTransaction = JSON.parse(event.data);
    updateTotalTransactionBalance(totalTransaction.total);
  };

  averageTransactionStream = new EventSource(`${streamingUrl}/average-transactions?access_token=${accessToken}`);

  averageTransactionStream.onmessage = function (event: MessageEvent) {
    updateAverageTransactionBalance(event.data);
  };
}

onMounted(async () => {
  if (keycloak.authenticated) {
    await readStatement();
  } else {
    watch(() => keycloak.authenticated ?? false, (newValue) => {
      if (newValue) {
        readStatement();
      }
    });
  }
});

onUnmounted(() => {
  balanceStream?.close();
  totalTransactionStream?.close();
  averageTransactionStream?.close();
})
</script>
