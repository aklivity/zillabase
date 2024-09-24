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

<script lang="ts">
import {defineComponent, ref, watch} from 'vue';
import {streamingUrl} from 'boot/axios';
import {keycloak} from 'boot/main';

export default defineComponent({
  name: 'MainPage',
  setup () {
    const balanceSeries = ref([{
      name: 'Balance',
      data: [] as any
    }]);
    const balanceStream = null as EventSource | null;
    const totalTransaction = ref(0);
    const averageTransaction = ref(0);
    const totalTransactionStream = null as EventSource | null;
    const averageTransactionStream = null as EventSource | null;

    return {
      keycloak,
      options: {},
      balanceSeries,
      balanceStream,
      totalTransaction,
      totalTransactionStream,
      averageTransaction,
      averageTransactionStream
    }
  },
  async mounted() {
    const updateBalance = this.updateBalance;
    const updateTotalTransactionBalance = this.updateTotalTransactionBalance;
    const updateAverageTransactionBalance = this.updateAverageTransactionBalance;
    let totalTransactionStream = this.totalTransactionStream;
    let balanceStream = this.balanceStream;
    let averageTransactionStream = this.averageTransactionStream;

    async function readStatement() {
      const accessToken = keycloak.token;

      balanceStream = new EventSource(`${streamingUrl}/streampay_balance_histories?access_token=${accessToken}`);

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

    if (keycloak.authenticated) {
      await readStatement();
    } else {
      watch(() => keycloak.authenticated ?? false, (newValue) => {
        if (newValue) {
          readStatement();
        }
      });
    }
  },
  methods: {
    updateBalance(newBalance: number, timestamp: number) {
      let balance = { x: new Date(timestamp).toLocaleString(), y: newBalance };
      if (this.balanceSeries[0].data.length > 20) {
        this.balanceSeries[0].data.pop();
        this.balanceSeries[0].data.unshift(balance);
      } else {
        this.balanceSeries[0].data.push(balance);
      }
    },
    updateTotalTransactionBalance(total: number) {
      this.totalTransaction = total;
    },
    updateAverageTransactionBalance(average: number) {
      this.averageTransaction = +Math.abs(average).toFixed(2);
    },
  },
  unmounted() {
    this.balanceStream?.close();
    this.totalTransactionStream?.close();
  }
});
</script>
