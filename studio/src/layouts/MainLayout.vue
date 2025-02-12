<template>
  <q-layout view="lHh Lpr lFf" class="zillbase-dashboard">
    <q-header bordered class="zillbase-header bg-custom-dark-color">
      <q-toolbar class="q-pl-lg q-px-none">
        <q-toolbar-title>
          <dynamic-bread-crumb />
        </q-toolbar-title>
        <q-space />
        <q-separator vertical />
        <q-btn
          flat
          :icon="darkModeIcon ? 'img:/icons/sun.svg' : 'img:/icons/moon.svg'"
          :ripple="false"
          class="filter-text-secondary w-70px"
          @click="toggleDarkMode"
        />
        <q-separator vertical />
        <q-btn
          flat
          icon="img:/icons/notification-bing.svg"
          :ripple="false"
          class="filter-text-secondary w-70px"
        />
        <q-separator vertical />
        <q-btn
          flat
          icon="img:/icons/message-question.svg"
          :ripple="false"
          class="filter-text-secondary w-70px"
        />
      </q-toolbar>
    </q-header>

    <q-drawer
      v-model="leftDrawerOpen"
      show-if-above
      bordered
      class="bg-custom-primary"
      side="left"
    >
      <q-list>
        <q-item-label
          header
          class="flex items-center w-full q-pb-sm fixed q-mb-xl border-bottom-custom-highlight w-full left-sidebar-log"
        >
          <div class="flex justify-between items-center w-full -margin-sm">
            <q-img
            :src="logoSrc"
            fit="contain"
            style="width: 150px; height: 22px"
          />
          <q-btn
            unelevated
            color="light-green"
            :icon="leftDrawerOpen ? 'chevron_left' : 'chevron_right'"
            style="width: 30px; min-height: 30px; border-radius: 10px"
            @click="leftDrawerOpen = !leftDrawerOpen"
          />
          </div>
        </q-item-label>
        <!-- <q-separator /> -->

        <q-item
          v-for="item in NavLinks"
          :key="item.groupTitle"
          class="column q-pb-md"
        >
          <q-item-section avatar class="q-pb-sm q-pr-none">
            <q-item-label
              class="text-uppercase text-custom-gray-light text-subtitle2 text-weight-light"
              >{{ item.groupTitle }}</q-item-label
            >
          </q-item-section>
          <q-item-section
            v-for="link in item.children"
            :key="link.title"
            class="nav-items q-py-md"
            :class="{ 'active-link': isActive(link.href) }"
            @click="navigate(link.href)"
          >
            <q-tooltip
              v-if="!leftDrawerOpen"
              anchor="center right"
              self="center middle"
            >
              {{ link.title }}
            </q-tooltip>
            <q-icon class="fs-22" :name="`img:${link.icon}`" />
            <q-item-label
              class="text-subtitle2 text-custom-dark text-weight-medium"
            >
              {{ link.title }}</q-item-label
            >
          </q-item-section>
        </q-item>
      </q-list>
    </q-drawer>

    <q-page-container>
      <div class="q-pa-lg flex justify-between">
        <div class="flex items-center">
          <h6 class="text-custom-text-secondary text-weight-bold text-h4">
            {{ route.meta.title }}
          </h6>
          <q-icon
            :name="`img:${route.meta.icon}`"
            class="fs-30 filter-text-secondary q-pl-lg"
          />
        </div>
      </div>
      <q-separator />
      <router-view />
    </q-page-container>
  </q-layout>
</template>

<script>
import { defineComponent, ref } from "vue";
import { Dark } from "quasar";
import NavLinks from "src/assets/data/navlinks";
import DynamicBreadCrumb from "src/pages/shared/DynamicBreadCrumb.vue";
import { useRoute } from "vue-router";

export default defineComponent({
  name: "MainLayout",

  components: {
    DynamicBreadCrumb,
  },
  data() {
    return {
      darkModeIcon: false,
    };
  },
  methods: {
    toggleDarkMode() {
      Dark.set(!Dark.isActive);
      this.darkModeIcon = !this.darkModeIcon;
    },
    navigate(href) {
      if (href) {
        this.$router.push(href);
      }
    },
  },

  setup() {
    const route = useRoute();

    const isActive = (href) => {
      return route.path === href;
    };
    return {
      leftDrawerOpen: ref(false),
      NavLinks: NavLinks,
      isActive,
      route,
    };
  },
  computed: {
    logoSrc() {
      if (this.leftDrawerOpen) {
        return this.darkModeIcon
          ? "/images/light-logo.svg"
          : "/images/logo.svg";
      } else {
        return this.darkModeIcon
          ? "/images/short-light-logo.svg"
          : "/images/short-logo.svg";
      }
    },
  },
});
</script>
<style scoped lang="scss">
.q-toolbar {
  min-height: 70px;
}
.q-drawer__content {
  .left-sidebar-log {
    min-height: 71px;
    z-index: 5;
  }
  .q-list {
    .q-item.q-item-type:nth-child(2) {
      padding-top: 5rem;
    }
  }
}
.nav-items {
  flex-direction: row;
  justify-content: flex-start;
  align-items: center;
  gap: 12px;
  margin-left: 0 !important;
  cursor: pointer;

  .q-item__label,
  .q-icon {
    transition: color 0.3s ease, filter 0.3s ease;
  }

  .q-icon {
    filter: var(--q-color-filter-custom-dark);
  }

  &:hover,
  &.active-link {
    .q-item__label {
      color: $light-green !important;
    }
    .q-icon {
      filter: var(--q-color-filter-light-green);
    }
  }
}
.border-bottom-custom-highlight {
  border-bottom: 1px solid var(--q-color-highlight);
}
</style>
