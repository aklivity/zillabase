<template>
  <router-view />
</template>

<script setup>
defineOptions({
  name: "App",
  mounted() {
    window.zillaApp = this;
  }
});
</script>
