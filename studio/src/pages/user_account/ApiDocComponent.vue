<template>
  <div class="q-py-md iframe">
    <async-api :schema="iframeSrc"></async-api>
  </div>
</template>
<script>
import { appApiDocs } from "src/services/api";
import { defineComponent } from "vue";

export default defineComponent({
  name: "ApiDocComponent",
  components: {},
  data() {
    return {
      iframeSrc: "",
    };
  },
  mounted() {
    appApiDocs("http-asyncapi").then(({ data }) => {
      this.iframeSrc = data;
    });
  },
});
</script>
