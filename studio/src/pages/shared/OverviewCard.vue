<template>
  <div class="q-pa-md row q-gutter-md">
    <q-card
      bordered
      v-for="(card, index) in cards"
      :key="index"
      class="col no-shadow rounded-15"
      :class="index === 0 ? 'q-ml-none q-mr-sm' : 'q-mx-sm '"
    >
      <!-- Icon and Title Section -->
      <q-card-section class="flex items-center">
        <q-icon
          :name="`img:${card.icon}`"
          class="filter-light-green"
          size="sm"
        />
        <p
          class="text-subtitle1 text-default-light-green q-mt-xs text-weight-medium q-ml-md"
        >
          {{ card.title }}
        </p>
      </q-card-section>

      <q-card-section class="q-pt-none">
        <q-input
          outlined
          readonly
          disable
          class="non-editable-input"
          style="pointer-events: none; user-select: none; border-radius: 10px"
        >
          <!-- Left text -->
          <template v-slot:prepend>
            <span class="text-subtitle2 text-custom-gray-dark">
              {{ card.request }}</span
            >
          </template>
          <!-- Right text -->
          <template v-slot:append>
            <span class="text-h6">{{ card.requestCount }}</span>
          </template>
        </q-input>
      </q-card-section>

      <!-- chart Section -->
      <q-card-section> </q-card-section>

      <!-- Stats and Button Section -->
      <q-card-section>
        <div class="text-subtitle2 text-custom-gray-dark q-py-sm">
          <span>{{ card.progressLabel }}</span>
        </div>
        <div class="row q-pb-sm" style="justify-content: space-between">
          <span>{{ card.totalCount }} {{ card.unit }}</span>
          <span>{{ card.maxtotalCount }} {{ card.unit }}</span>
        </div>
        <q-linear-progress
          :value="calculateProgress(card.totalCount, card.maxtotalCount) / 100"
          track-color="'rgba(30, 213, 167, 0.1)'"
          size="8px"
          rounded
        />
      </q-card-section>
    </q-card>
  </div>
</template>

<script>
import { defineComponent } from "vue";

export default defineComponent({
  name: "OverviewCard",
  data() {
    return {
      cards: [
        {
          icon: "/icons/function.svg",
          title: "Functions",
          request: "Functional Requests",
          requestCount: "5809",
          description:
            "Interact with your database through the ZillBase client libraries with your API keys.",
          statLabel: "Total API's",
          statValue: "32",
          totalCount: 203,
          maxtotalCount: 1000,
          unit: "MB",
          progressLabel: "Functions Space",
        },
        {
          icon: "/icons/auth.svg",
          title: "Auth",
          request: "Auth Requests",
          requestCount: "54",
          description:
            "Interact with your database through the ZillBase client libraries with our Programming Docs.",
          statLabel: "Programming Docs",
          statValue: "79",
          totalCount: 500,
          maxtotalCount: 10000,
          unit: "",
          progressLabel: "Users",
        },
        {
          icon: "/icons/storage.svg",
          title: "Storage",
          request: "Storage Requests",
          requestCount: "290",
          description:
            "Interact with your database through the ZillBase client libraries with our Other Docs.",
          statLabel: "Other Docs",
          statValue: "79",
          totalCount: 11,
          maxtotalCount: 1024,
          unit: "MB",
          progressLabel: "Users",
        },
      ],
      inputData: [
        { name: "Apples", data: [5, 3] },
        { name: "Bananas", data: [2, 8] },
        { name: "Oranges", data: [4, 7] },
      ],
      chartOptions: {
        chart: {
          type: "column",
        },
        title: {
          text: "Dynamic Column Chart Example",
        },
        xAxis: {
          categories: ["2023", "2024"],
          title: {
            text: "Year",
          },
        },
        yAxis: {
          min: 0,
          title: {
            text: "Units",
          },
        },
        series: [],
      },
    };
  },
  methods: {
    calculateProgress(current, max) {
      return (current / max) * 100;
    },
  },
});
</script>
<style scoped lang="scss">
.card-width {
  width: 300px;
  max-width: 100%;
}
.text-primary {
  color: #00b894; /* Customize as needed */
}
.rounded-borders {
  border-radius: 8px;
}
.non-editable-input {
  border: 1px solid #e9e8e8;
  cursor: default;
  box-shadow: none;
  .q-field--disabled .q-field__inner {
    cursor: auto;
  }
}
.q-linear-progress {
  color: #1ed5a7;
}
</style>
