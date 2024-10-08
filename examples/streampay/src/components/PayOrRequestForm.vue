<template>
  <div style="margin-left: 12%; margin-right: 12%; margin-top: 40px;">
    <div class="text-center text-primary text-h4" style="margin: 20px 35% 40px 30%;">
      ${{ balance }}
    </div>
    <q-form
      @submit="onPay"
      @reset="onRequest"
      class="q-gutter-md"
    >
      <q-select
        use-chips
        stack-label
        label="To"
        use-input
        outlined
        v-model="userOption"
        :options="userOptions"
        :rules="[
           (val) =>
              (val && val.value.length > 0) ||
              'Please select user',
        ]"
      />

      <q-input
        label="Amount"
        type="number"
        v-model="amount"
        step="any"
        lazy-rules
        outlined
        :rules="[
          (val) =>
            (val && val > 0) || 'Required field and should be more than $0.',
        ]"
      />

      <q-input
        v-model="notes"
        label="Notes"
        type="textarea"
        outlined
      />

      <div style="margin-left: 15%; margin-bottom: 20px;  margin-top: 20px;">
        <q-btn label="Pay" style="width: 200px" type="submit" color="primary" rounded />
        <q-btn label="Request" style="width: 200px" type="reset" color="primary" class="q-ml-sm" rounded />
      </div>
    </q-form>

  </div>
</template>

<script lang="ts">
import {defineComponent, ref, toRefs, watch} from 'vue'
import {api, streamingUrl} from 'boot/axios';
import {keycloak, user, SecureEventSource} from 'boot/main';
import {useQuasar} from 'quasar';
import {useRouter} from 'vue-router';
import {v4} from 'uuid';

interface UserOption {
  label: string;
  value: string;
}

export default defineComponent({
  name: 'PayOrRequestForm',
  props: {
    requestId: {
      type: String
    }
  },
  setup (props) {
    const $q = useQuasar()
    const { requestId } = toRefs(props);
    const balance = ref(0 as number);
    const userOption = ref(null as UserOption | null);
    const userOptions = ref([] as UserOption[]);
    const amount = ref(0 as number);
    const notes = ref('' as string);
    const router = useRouter();
    const balanceStream = null as SecureEventSource | null;

    return {
      keycloak,
      user,
      formRequestId: requestId,
      balance,
      userOption,
      userOptions,
      amount,
      notes,
      balanceStream,
      async onPay () {
        if (balance.value - amount.value > 0) {
          const accessToken = keycloak.token;
          const authorization = { Authorization: `Bearer ${accessToken}` };
          api.post('/streampay_commands', {
            type: 'SendPayment',
            user_id: userOption.value?.value,
            requestid: '',
            amount: +amount.value,
            notes: notes.value
          },{
            headers: {
              'Idempotency-Key': v4(),
              ...authorization
            }}).then(function () {
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
      },
      async onRequest () {
        const accessToken = keycloak.token;
        const authorization = { Authorization: `Bearer ${accessToken}` };
        api.post('/streampay_commands', {
          type: 'RequestPayment',
          user_id: userOption.value?.value,
          requestid: '',
          amount: +amount.value,
          notes: notes.value
        },{
          headers: {
            'Idempotency-Key': v4(),
            ...authorization
        }}).then(function () {
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
    }
  },
  async mounted() {
    const updateBalance = this.updateBalance;
    const updateAmount = this.updateAmount;
    const formRequestId = this.formRequestId;
    const fetchAndSetUsers = this.fetchAndSetUsers;
    let balanceStream = this.balanceStream;

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

      if (formRequestId) {
        api.get('/streampay_payment_requests/' + formRequestId,{
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

    if (keycloak.authenticated) {
      await readBalance();
    } else {
      watch(() => keycloak.authenticated ?? false, (newValue) => {
        if (newValue) {
          readBalance();
        }
      });
    }

  },
  methods: {
    updateBalance(newBalance: number) {
      this.balance = +newBalance.toFixed(2);
    },
    updateAmount(amount: number) {
      this.amount = amount;
    },
    async fetchAndSetUsers(userId = null) {
      const accessToken = keycloak.token;
      const authorization = { Authorization: `Bearer ${accessToken}` };

        const url = userId == null ? '/streampay_users' : '/streampay_users/' + userId;
        await api.get(url, {
          headers: {
            ...authorization,
          }
        })
        .then((response) => {
          const users = response.data;

          if (typeof users[Symbol.iterator] === 'function')
          {
              for(let user of users) {
              if (user.id != this.user?.username) {
                const newUserOption = {
                  label: user.name,
                  value: user.id
                };
                this.userOptions.push(newUserOption as any);
              }

              if (userId && userId == user.id) {
                this.userOption = { label: user.name, value: user.id };
              }
            }
          }
          else
          {
              if (users.id != this.user?.id) {
                const newUserOption = {
                  label: users.name,
                  value: users.id
                };
                this.userOptions.push(newUserOption as any);
              }

              if (userId && userId == users.id) {
                this.userOption = { label: users.name, value: users.id };
              }
          }
        });
    }
  },
  unmounted() {
    this.balanceStream?.close();
  }
})
</script>
