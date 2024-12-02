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

      <!-- Description Section -->
      <q-card-section
        class="text-subtitle2 text-custom-gray-dark q-pt-none text-weight-light"
      >
        {{ card.description }}
      </q-card-section>
      <q-separator />
      <!-- Stats and Button Section -->
      <q-card-section class="q-pa-none overflow-hidden">
        <div
          class="row justify-between items-center q-pt-md bg-custom-primary q-pa-md"
        >
          <div
            class="text-caption text-custom-gray-dark q-mb-xs text-weight-medium"
          >
            {{ card.statLabel }}
            <p class="text-subtitle2 text-bold text-custom-text-secondary">
              {{ card.statValue }}
            </p>
          </div>
          <q-btn
            label="View Docs"
            color="light-green"
            class="text-weight-medium text-capitalize rounded-10"
            unelevated
            :ripple="false"
          />
        </div>
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
          icon: "/icons/document-text.svg",
          title: "API Docs",
          description:
            "Interact with your database through the ZillBase client libraries with your API keys.",
          statLabel: "Total API's",
          statValue: "32",
        },
        {
          icon: "/icons/document-text.svg",
          title: "Programming Docs",
          description:
            "Interact with your database through the ZillBase client libraries with our Programming Docs.",
          statLabel: "Programming Docs",
          statValue: "79",
        },
        {
          icon: "/icons/document-text.svg",
          title: "Other Docs",
          description:
            "Interact with your database through the ZillBase client libraries with our Other Docs.",
          statLabel: "Other Docs",
          statValue: "79",
        },
      ],
    };
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
</style>
