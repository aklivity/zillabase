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
          <div class="text-subtitle2">Note:</div>
          <p>{{ req.notes }}</p>
        </q-card-section>
        <q-card-section :class="riskBackground(req?.risk)">
          <div class="text-h7">Risk: {{ req.risk }}</div>
          <q-inner-loading :showing="req.risk == 'PENDING'">
          </q-inner-loading>
          <p>{{ req.summary }}</p>
        </q-card-section>

        <q-separator />

        <q-card-actions>
          <q-form @submit="pay(req)" @reset="reject(req)" class="q-pa-sm q-gutter-sm">
            <q-btn label="Approve" type="submit" color="primary" unelevated />
            <q-btn label="Reject" type="reset" flat />
          </q-form>
        </q-card-actions>
      </q-card>

    </div>
  </q-page>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';
import { api } from 'boot/axios';
import { keycloak } from 'boot/main';
import { useQuasar } from 'quasar';
import { useRouter } from 'vue-router';
import { v4 } from 'uuid';

interface PaymentRequest {
    from_user_id: string;
    from_username: string;
    to_username: string;
    to_user_id_identity: string;
    status: string;
    id: string;
    amount: number;
    notes: string;
}

const $q = useQuasar()
const router = useRouter();

const requests = ref([] as any);

function riskBackground(status = 'PENDING') {
  switch (status) {
    case 'PENDING':
      return 'bg-grey';
    case 'LOW':
      return 'bg-green';
    case 'MEDIUM':
      return 'bg-yellow';
    case 'HIGH':
      return 'bg-red';
    default:
      return 'bg-white';
  }
}

async function readRequests() {
  const accessToken = keycloak.token;
  const authorization = { Authorization: `Bearer ${accessToken}` };

  await api.get('/streampay_payment_requests', {
    headers: {
      ...authorization
    }
  })
    .then((response) => {
      const data = response.data;

      const groupedById: Record<string, PaymentRequest[]> = data.reduce((acc: any, req: PaymentRequest) => {
          if (!acc[req.id]) {
              acc[req.id] = [];
          }
          acc[req.id].push(req);
          return acc;
      }, {});

      const filteredGroups: PaymentRequest[][] = Object.values(groupedById).filter((group) =>
          group.every((item) => item.status === 'pending')
      );

      const filteredRequest: PaymentRequest[] = filteredGroups.reduce(
          (acc: PaymentRequest[], group: PaymentRequest[]) => acc.concat(group),
          []
      );

      requests.value = filteredRequest;
    });
  api.get('/streampay_payment_risk_assessment', {
    headers: {
      ...authorization
    }
  })
    .then((response) => {
      let riskById = response.data?.reduce((acc: any, risk: any) => { acc[risk.id] = risk; return acc; }, {});
      requests.value = requests.value.map((r: any) => ({ ...r, risk: riskById[r.id]?.risk || 'PENDING', summary: riskById[r.id]?.summary }));;
    });
}

function pay(request: any) {
  api.post('/streampay_send_payment_commands', {
    user_id: request.from_user_id,
    request_id: request?.id || '',
    amount: request.amount,
    notes: request.notes
  }, {
    headers: {
      'Idempotency-Key': v4(),
      Authorization: `Bearer ${keycloak.token}`
    }
  }).then(function () {
    router.push({ path: '/main' });
  })
  .catch(function ({message}) {
    $q.notify({
      position: 'top',
      color: 'red-5',
      textColor: 'white',
      icon: 'error',
      message
    });
  });
}

function reject(request: any) {
  api.post('/streampay_reject_payment_commands', {
    user_id: request.from_user_id,
    request_id: request?.id || '',
    amount: request.amount,
    notes: request.notes
  }, {
    headers: {
      'Idempotency-Key': v4(),
      Authorization: `Bearer ${keycloak.token}`
    }
  }).then(function () {
    router.push({ path: '/main' });
  })
  .catch(function (error) {
    $q.notify({
      position: 'top',
      color: 'red-5',
      textColor: 'white',
      icon: 'error',
      message: error
    });
  });
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
