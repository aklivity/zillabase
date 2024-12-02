<template>
  <div class="q-pa-lg flex justify-between items-center">
    <div>
      <p class="text-custom-text-secondary text-h6 fw-600">AsyncAPI Studio</p>
      <p class="text-custom-gray-dark text-subtitle1 text-weight-light">
        Lorem ipsum dolor sit amet, consectetur adipiscing elit.
      </p>
    </div>
    <q-icon name="img:/icons/export.svg" class="filter-light-green" size="sm" />
  </div>
  <div class="q-py-md iframe">
    <iframe
      :src="iframeSrc"
      width="100%"
      height="400px"
      frameborder="0"
      allowfullscreen
      style="display: block; border: none"
    >
    </iframe>
  </div>
</template>
<script>
import { defineComponent } from "vue";
export default defineComponent({
  name: "ApiDocComponent",
  components: {},
  data() {
    return {
      iframeSrc:
        "https://editor-next.swagger.io/?url=https://raw.githubusercontent.com/aklivity/zillabase/refs/heads/feature/local-platform-api/api/openapi.yaml", // Change this URL as needed
    };
  },
});
</script>
