<template>
  <div style="margin-left: 12%; margin-right: 12%; margin-top: 40px;">
    <div class="text-center text-primary text-h4" style="margin: 20px 35% 40px 30%;">
      ${{ balance }}
    </div>
    <q-form @submit="onPay" @reset="onRequest" class="q-gutter-md">
      <q-select use-chips stack-label label="To" use-input outlined v-model="userOption" :options="userOptions" :rules="[
        (val) =>
          (val && val.value.length > 0) ||
          'Please select user',
      ]" />

      <q-input label="Amount" type="number" v-model="amount" step="any" lazy-rules outlined :rules="[
        (val) =>
          (val && val > 0) || 'Required field and should be more than $0.',
      ]" />

      <q-input v-model="notes" label="Notes" type="textarea" outlined />

      <div style="margin-left: 15%; margin-bottom: 20px;  margin-top: 20px;">
        <q-btn label="Pay" style="width: 200px" type="submit" color="primary" rounded />
        <q-btn label="Request" style="width: 200px" type="reset" color="primary" class="q-ml-sm" rounded />
      </div>
    </q-form>

  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, ref, watch } from 'vue'
import { api, streamingUrl } from 'boot/axios';
import { keycloak, user, SecureEventSource } from 'boot/main';
import { useQuasar } from 'quasar';
import { useRouter } from 'vue-router';
import { v4 } from 'uuid';

const $q = useQuasar()
const router = useRouter();
interface UserOption {
  label: string;
  value: string;
}

const props = defineProps({
  requestId: {
    type: String
  }
})

const balance = ref();
const userOption = ref(null as UserOption | null);
const userOptions = ref([] as UserOption[]);
const amount = ref();
const notes = ref('' as string);

let balanceStream: SecureEventSource;

async function onPay() {
  if (balance.value - amount.value > 0) {
    const accessToken = keycloak.token;
    const authorization = { Authorization: `Bearer ${accessToken}` };
    api.post('/streampay_commands', {
      type: 'SendPayment',
      user_id: userOption.value?.value,
      requestid: '',
      amount: +amount.value,
      notes: notes.value
    }, {
      headers: {
        'Idempotency-Key': v4(),
        ...authorization
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
  } else {
    $q.notify({
      position: 'top',
      color: 'red-5',
      textColor: 'white',
      icon: 'error',
      message: "You don't have enough balance."
    });
  }
}
async function onRequest() {
  const accessToken = keycloak.token;
  const authorization = { Authorization: `Bearer ${accessToken}` };
  api.post('/streampay_commands', {
    type: 'RequestPayment',
    user_id: userOption.value?.value,
    requestid: '',
    amount: +amount.value,
    notes: notes.value
  }, {
    headers: {
      'Idempotency-Key': v4(),
      ...authorization
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

async function readBalance() {
  const accessToken = keycloak.token;
  const authorization = { Authorization: `Bearer ${accessToken}` };
  balanceStream = new SecureEventSource(`${streamingUrl}/streampay_balances-stream-identity`, {
    credentials: () => keycloak.token || ''
  });

  balanceStream.onmessage = function (event: MessageEvent) {
    const balance = JSON.parse(event.data);
    updateBalance(balance.balance);
  };

  if (props.requestId) {
    api.get('/streampay_payment_requests/' + props.requestId, {
      headers: {
        ...authorization
      }
    })
      .then((response) => {
        const request = response.data;
        updateAmount(request.amount);

        fetchAndSetUsers(request.from_user_id);
      })
  } else {
    await fetchAndSetUsers();
  }
}


function updateBalance(newBalance: number) {
  balance.value = +newBalance.toFixed(2);
}
function updateAmount(newAmount: number) {
  amount.value = newAmount;
}
async function fetchAndSetUsers(userId = null) {
  const accessToken = keycloak.token;
  const authorization = { Authorization: `Bearer ${accessToken}` };

  const url = userId == null ? '/streampay_users' : '/streampay_users/' + userId;
  await api.get(url, {
    headers: {
      ...authorization,
    }
  })
    .then((response) => {
      userOptions.value = response.data?.filter((u: any) => (u.id != user?.username))
        .map((u: any) => ({
          label: u.name,
          value: u.id
        }))
    });
}

onMounted(async () => {
  if (keycloak.authenticated) {
    await readBalance();
  } else {
    watch(() => keycloak.authenticated ?? false, (newValue) => {
      if (newValue) {
        readBalance();
      }
    });
  }
})

onUnmounted(() => {
  balanceStream?.close();
})
</script>
