<template>
  <div class="q-pa-lg">
    <p class="text-custom-text-secondary text-h6 fw-600">Docs</p>
    <p class="text-custom-gray-dark text-subtitle1 text-weight-light">
      Lorem ipsum dolor sit amet, consectetur adipiscing elit.
    </p>
    <overview-card />
  </div>
  <q-separator />
  <div class="q-pa-md">
    <p class="text-custom-text-secondary text-h6 fw-600">Our Projects</p>
    <p class="text-custom-gray-dark text-subtitle1 text-weight-light">
      Lorem ipsum dolor sit amet, consectetur adipiscing elit.
    </p>
  </div>
</template>
<script>
import { defineComponent } from "vue";
import OverviewCard from "../shared/OverviewCard.vue";
export default defineComponent({
  name: "OverviewComponent",
  components: {
    OverviewCard,
  },
});
</script>
