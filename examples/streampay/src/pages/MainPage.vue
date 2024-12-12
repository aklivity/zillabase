<template>
  <q-page class="items-center" style="margin-left: 12%; margin-right: 12%; margin-top: 70px;">
    <div class="items-center text-primary text-h4" style="margin-left: 40%; margin-bottom: 20px;">
      Activities
    </div>
    <q-table
      ref="tableRef"
      title-class="feed-title"
      hide-bottom
      hide-header
      card-style="box-shadow: none;"
      :rows="activities"
      :columns="columns"
      :table-colspan="9"
      row-key="index"
      virtual-scroll
      :virtual-scroll-item-size="48"
      :rows-per-page-options="[0]"
    >
       <template v-slot:body="props">
         <q-tr :props="props" no-hover>
           <q-td  key="avatar" :props="props" style="width: 50px">
             <q-avatar color="primary" text-color="white">{{ props.row.avatar }}</q-avatar>
           </q-td>
           <q-td  key="activities" :props="props">
             <div class="text-h6">
               <b>{{ props.row.from}}</b> {{ props.row.state }} <b>{{ props.row.to}}</b>
             </div>
             <div class="text-subtitle2">
               {{ props.row.date }}
             </div>
           </q-td>

           <q-td
             key="amount"
             :props="props"
           >
             <div v-if="props.row.eventName === 'PaymentSent'" class="text-subtitle1" style="color:red">
               -${{ props.row.amount }}
             </div>
             <div v-else class="text-subtitle1" style="color:green" >
               ${{ props.row.amount }}
             </div>
           </q-td>
         </q-tr>
       </template>
    </q-table>
  </q-page>
</template>


<script setup lang="ts">
import {onMounted, onUnmounted, ref, watch} from 'vue';
import {streamingUrl} from 'boot/axios';
import {keycloak, user, SecureEventSource} from 'boot/main';

const tableRef = ref(null);

const columns = ref([
  { name: 'avatar', label: 'avatar', align: '"left"', field: 'avatar'},
  {
    name: 'activities',
    label: 'activities',
    required: true,
    align: 'left',
    field: 'activities',
    format: (val: any) => `${val}`
  },
  { name: 'amount', label: 'amount', align: 'right', field: 'amount'},
])

const activities = ref([] as any);
let activitiesStream: SecureEventSource;

onMounted(async () => {
  const userId = user?.username;

  async function readActivities() {
    activitiesStream = new SecureEventSource(`${streamingUrl}/streampay_activities-stream`, {
      credentials: () => keycloak.token || ''
    });

    activitiesStream.onmessage = function (event: MessageEvent) {
      const activity = JSON.parse(event.data);

      if ((activity.eventname === 'PaymentReceived' && activity.from_user_id === userId) ||
          (activity.eventname === 'PaymentSent' && activity.to_user_id === userId)) {
      } else {
        let state = '';

        var from = activity.from_user_id === userId ? 'You' : activity.from_username;
        var to = activity.to_user_id === userId ? 'you' : activity.to_username;

        if (activity.eventname === 'PaymentSent') {
          state = 'paid';
        } else if (activity.eventname === 'PaymentReceived') {
          from = activity.to_user_id;
          to = activity.from_user_id;
          state = 'received from';
        } else if (activity.eventname === 'PaymentRequested') {
          state = 'requested';
        }

        const avatar = from.charAt(0).toUpperCase();
        const eventName = activity.eventname;

        const newActivity = {
          eventName,
          avatar,
          from,
          to,
          state,
          amount: Math.abs(activity.amount).toFixed(2),
          date: new Date(activity.timestamp)
        };

        if (activities.value.length > 20) {
          activities.value.pop();
          activities.value.unshift(newActivity);
        } else {
          activities.value.push(newActivity);
        }
      }
    };
  }

  if (keycloak.authenticated)
  {
    await readActivities();
  } else {
    watch(() => keycloak.authenticated ?? false, (newValue) => {
      if (newValue) {
        readActivities();
      }
    });
  }
})

onUnmounted(() => {
  activitiesStream?.close();
})


</script>
