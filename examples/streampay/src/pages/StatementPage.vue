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
import {useAuth0} from '@auth0/auth0-vue';
import {streamingUrl} from 'boot/axios';

export default defineComponent({
  name: 'MainPage',
  setup () {
    const auth0 = useAuth0();

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
      auth0,
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
    const auth0 = this.auth0;
    const updateBalance = this.updateBalance;
    const updateTotalTransactionBalance = this.updateTotalTransactionBalance;
    const updateAverageTransactionBalance = this.updateAverageTransactionBalance;
    let totalTransactionStream = this.totalTransactionStream;
    let balanceStream = this.balanceStream;
    let averageTransactionStream = this.averageTransactionStream;

    async function readStatement() {
      const accessToken = await auth0.getAccessTokenSilently();

      balanceStream = new EventSource(`${streamingUrl}/balance-histories?access_token=${accessToken}`);

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

    if (auth0.isAuthenticated.value) {
      await readStatement();
    } else {
      watch(auth0.isAuthenticated, readStatement);
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
