<template>
  <div class="q-pa-lg">
    <p class="text-custom-text-secondary text-h6 fw-600">Summersumir - Web</p>
    <p class="text-custom-gray-dark text-subtitle1 text-weight-light q-mt-lg">
      <q-select
        v-model="selectedItem"
        :options="options"
        outlined
        dense
        placeholder="Select type"
        dropdown-icon="keyboard_arrow_down"
        style="width: 320px"
        class="custom-select"
      >
        <template v-slot:after>
          <span class="text-subtitle1"
            >Statistics for past {{ selectedItem }}</span
          >
        </template>
      </q-select>
    </p>
    <overview-card />
  </div>
  <q-separator />
  <div class="row q-pa-md">
    <div class="col-9">
      <common-table
        title="Active Projects"
        description="Lorem ipsum dolor sit amet, consectetur adipiscing elit."
        :columns="activeProjectTableColumns"
        :rows="activeProjectTableData"
        :hideBottom="false"
        buttonLabel="Create New Project"
        :showSearch="false"
        :additionButton="true"
        :showLabelBottom="false"
      />
    </div>
    <div class="col-3">
      <q-card class="no-shadow rounded-15 q-mx-sm flex justify-center" bordered>
        <q-card-section>
          <p class="text-custom-gray-dark text-subtitle1">
            Storage is at <strong class="text-custom-text-secondary">{{ circularProgress }}%</strong> capacity.
            Consider upgrading
          </p>
        </q-card-section>
        <q-card-section>
          <q-circular-progress
            show-value
            class="q-ma-md"
            :value="circularProgress"
            size="175px"
            :thickness="0.2"
            color="orange"
            track-color="transparent"
          >
            <p class="flex" style="flex-direction: column">
              <q-icon
                name="img:/icons/storage.svg"
                class="filter-light-green"
                size="md"
              />
              <span class="text-h5 q-mt-sm">{{ circularProgress }}%</span>
            </p>
          </q-circular-progress>
        </q-card-section>
        <q-card-section>
          <p class="filter-light-green text-subtitle1 underline">Upgrading Storage <q-icon name="arrow_forward" class="filter-light-green" size="sm"></q-icon></p>
        </q-card-section>
      </q-card>
    </div>
  </div>
</template>
<script>
import { defineComponent } from "vue";
import { ref } from "vue";
import OverviewCard from "../shared/OverviewCard.vue";
import CommonTable from "../shared/CommonTable.vue";
export default defineComponent({
  name: "OverviewComponent",
  components: {
    OverviewCard,
    CommonTable,
  },
  data() {
    return {
      options: ["7 days", "14 days", "30 days"],
      selectedItem: "7 days",
      activeProjectTableColumns: [
        { name: "name", label: "Project Name", align: "left", field: "name" },
        {
          name: "update",
          label: "Last Update",
          align: "left",
          field: "update",
        },
        { name: "type", label: "Status", align: "left", field: "type" },
        {
          name: "apiRequest",
          label: "API Requests(Last 7 Days)",
          align: "left",
          field: "apiRequest",
        },
        {
          name: "storage",
          label: "Storage Used",
          align: "left",
          field: "storage",
        },
        { name: "actions", label: "Actions", align: "center" },
      ],
      activeProjectTableData: [
        {
          name: "MyApp Backend",
          update: "1 day ago",
          type: "Active",
          apiRequest: "1,200",
          storage: "300 MB of 1GB",
        },
        {
          name: "E-commerce Platform",
          update: "",
          type: "Real-time Synced",
          apiRequest: "800",
          storage: "",
        },
        {
          name: "Blog API",
          update: "1 day ago",
          type: "External",
          apiRequest: "800",
          storage: "300 MB of 1GB",
        },
        {
          name: "E-commerce Platform",
          update: "",
          type: "Embedded",
          apiRequest: "800",
          storage: "",
        },
      ],
      circularProgress: 80,
    };
  },
});
</script>
<style lang="css" scoped>
.filter-light-green {
  color: #00b894  ; /* Customize as needed */
}
.underline
{
  text-decoration: underline;
}
</style>
