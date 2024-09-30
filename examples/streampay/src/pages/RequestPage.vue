<template>
  <q-page class="items-center" style="margin-left: 12%; margin-right: 12%; margin-top: 70px;">
    <div class="items-center text-primary text-h4" style="margin-left: 40%; margin-bottom: 60px;">
      Requests
    </div>
    <q-table
      ref="tableRef"
      title-class="feed-title"
      hide-bottom
      hide-header
      card-style="box-shadow: none;"
      :rows="requests"
      :columns="columns"
      :table-colspan="9"
      row-key="index"
      virtual-scroll
      :virtual-scroll-item-size="48"
      :rows-per-page-options="[0]"
    >
       <template v-slot:body="props">
         <q-tr :props="props" no-hover>
           <q-td  key="requester" :props="props">
             <div style="margin-bottom: 20px; margin-top: 20px;">
               <div class="text-h6">
                 <b>{{ props.row.request.from_username }}</b> requested <b> ${{ props.row.request.amount.toFixed(2) }}</b>
               </div>
               <div class="text-subtitle2">
                 {{ props.row.request.notes }}
               </div>
             </div>
           </q-td>

           <q-td
             key="action"
             :props="props"
           >
             <div class="text-negative">
               <q-btn
                 label="Pay"
                 color="primary"
                 rounded
                 @click="this.$router.push({ path: '/payorrequest/' + props.row.request.id })" />
             </div>
           </q-td>
         </q-tr>
       </template>
    </q-table>
  </q-page>
</template>

<script lang="ts">
import {defineComponent, ref, watch} from 'vue';
import {api} from 'boot/axios';
import {keycloak} from 'boot/main';

export default defineComponent({
  name: 'MainPage',
  setup () {
    const tableRef = ref(null);

    const columns = [
      {
        name: 'requester',
        required: true,
        align: 'left',
        field: 'requester',
        format: (val: any) => `${val}`
      },
      { name: 'action', align: 'right', field: 'amount', sortable: true },
    ]

    const requests = ref([] as any);

    return {
      keycloak,
      tableRef,
      columns,
      requests
    }
  },
  async mounted() {
    const requests = this.requests;

    async function readRequests() {
      const accessToken = keycloak.token;
      const authorization = {Authorization: `Bearer ${accessToken}`};

      await api.get('/streampay_payment_requests', {
        headers: {
          ...authorization
        }
      })
      .then((response) => {
        const paymentRequests = response.data;
        for (let paymentRequest of paymentRequests) {
          requests.push({request: paymentRequest})
        }
      });
    }

    if (keycloak.authenticated) {
      await readRequests();
    } else {
      watch(() => keycloak.authenticated ?? false, (newValue) => {
        if (newValue) {
          readRequests();
        }
      });
    }
  }
});
</script>
