<template>
  <q-breadcrumbs separator-color="grey" class="text-subtitle2">
    <q-breadcrumbs-el
      v-for="(crumb, index) in breadcrumbs"
      :key="index"
      :label="getBreadcrumbLabel(crumb, index)"
    />
  </q-breadcrumbs>
</template>

<script>
import { defineComponent } from "vue";
import { useRoute } from "vue-router";

export default defineComponent({
  name: "DynamicBreadCrumb",
  props: {
    staticCrumbs: {
      type: Array,
      default: () => ["Zillabase"],
    },
    dynamicLabel: {
      type: String,
      default: "Overview",
    },
  },
  setup(props) {
    const route = useRoute();

    const breadcrumbs = [...props.staticCrumbs, props.dynamicLabel];

    const getBreadcrumbLabel = (crumb, index) => {
      if (index === breadcrumbs.length - 1) {
        return route.meta.title || props.dynamicLabel;
      }
      return crumb;
    };

    return {
      breadcrumbs,
      getBreadcrumbLabel,
    };
  },
});
</script>
<style scoped lang="scss">
.q-breadcrumbs {
  .q-breadcrumbs__el {
    color: var(--q-color-gray-dark);
    font-weight: 300;
  }
  .q-breadcrumbs--last {
    .q-breadcrumbs__el {
      color: var(--q-color-text-secondary);
      font-weight: 500;
    }
  }
}
</style>
