<template>
  <div class="q-pa-lg">
    <common-table
      title="External & Embedded"
      description="Lorem ipsum dolor sit amet, consectetur adipiscing elit."
      :columns="tableColumns"
      :rows="tableData"
      buttonLabel="Add function"
      searchInputPlaceholder="Functions"
      @delete-row="openDeleteDialog"
      @add-new="openFunctionDialog"
    />
  </div>

  <q-dialog
    v-model="addNewFunction"
    persistent
    full-height
    maximized
    position="right"
    backdrop-filter="blur(4px)"
    class="add-new-dialog"
  >
    <q-card class="full-height">
      <q-card-section class="flex justify-between items-center q-pa-lg">
        <div class="flex q-gutter-lg">
          <q-btn
            unelevated
            color="light-green"
            :icon="addNewFunction ? 'chevron_left' : 'chevron_right'"
            style="width: 30px; min-height: 30px"
            @click="addNewFunction = !addNewFunction"
            class="rounded-10"
          />
          <p class="text-custom-text-secondary text-h6 fw-600">
            Create New Function
          </p>
        </div>
        <q-icon
          name="img:/icons/function.svg"
          class="fs-30 filter-custom-dark"
          style="min-height: 30px"
        />
      </q-card-section>
      <q-separator />
      <q-card-section class="q-py-xl px-28">
        <div class="row items-center">
          <div class="col-3">
            <span class="text-custom-gray-dark text-subtitle1 text-weight-light"
              >Name</span
            >
          </div>
          <div class="col-9">
            <q-input
              dense
              outlined
              placeholder="Function Name"
              class="rounded-10 self-center text-weight-light rounded-input"
            />
          </div>
        </div>
        <div class="row items-center q-mt-lg">
          <div class="col-3">
            <span class="text-custom-gray-dark text-subtitle1 text-weight-light"
              >Return Type</span
            >
          </div>
          <div class="col-9">
            <q-input
              dense
              outlined
              placeholder="Function Return Type"
              class="rounded-10 self-center text-weight-light rounded-input"
            />
          </div>
        </div>
        <div class="row items-center q-mt-lg">
          <div class="col-3">
            <span class="text-custom-gray-dark text-subtitle1 text-weight-light"
              >Language</span
            >
          </div>
          <div class="col-9">
            <q-select
              v-model="selectedLanguage"
              :options="languageOptions"
              outlined
              dense
              placeholder="Select Language"
              dropdown-icon="keyboard_arrow_down"
              class="rounded-input"
            />
          </div>
        </div>
      </q-card-section>
      <q-separator />
      <q-card-section class="q-py-lg px-28">
        <div class="flex justify-between items-center q-mb-sm">
          <p class="text-custom-text-secondary text-subtitle1 fw-600">
            Paramters
          </p>
          <div>
            <q-tooltip anchor="center left" self="center end">
              Functions Docs
            </q-tooltip>
            <q-btn
              flat
              icon="img:/icons/export.svg"
              size="md"
              class="filter-light-green"
              :ripple="false"
            />
          </div>
        </div>
        <data-type-table
          :columns="functionTypeColumns"
          :rows="functionTypeRow"
          :typeOptions="functionTypeOptions"
          @add-row="addRow"
          @remove-row="removeRow"
        />
      </q-card-section>
      <q-separator />
      <q-card-section class="q-py-lg px-28">
        <div class="row items-center">
          <div class="col-3 flex items-center">
            <span class="text-custom-gray-dark text-subtitle1 text-weight-light"
              >Embedded</span
            >
            <q-icon
              name="img:icons/question-circle.svg"
              class="fs-lg filter-gray-dark q-ml-sm"
            />
            <q-tooltip anchor="bottom middle" self="top middle">
              Lorem ipsum dolor sit amet, consectetur adipiscing elit.
            </q-tooltip>
          </div>
          <div class="col-9">
            <q-radio
              dense
              v-model="functionTypeVal"
              val="embedded"
              color="light-green"
            />
          </div>
        </div>
        <div class="row items-center q-mt-md">
          <div class="col-3 flex items-center">
            <span class="text-custom-gray-dark text-subtitle1 text-weight-light"
              >External</span
            >
            <q-icon
              name="img:icons/question-circle.svg"
              class="fs-lg filter-gray-dark q-ml-sm"
            />
            <q-tooltip anchor="bottom middle" self="top middle">
              Lorem ipsum dolor sit amet, consectetur adipiscing elit.
            </q-tooltip>
          </div>
          <div class="col-9">
            <q-radio
              dense
              v-model="functionTypeVal"
              val="external"
              color="light-green"
            />
          </div>
        </div>
      </q-card-section>
      <q-separator />
      <q-card-section class="q-py-lg px-28">
        <div class="row items-start">
          <div class="col-3">
            <span class="text-custom-gray-dark text-subtitle1 text-weight-light"
              >Body</span
            >
          </div>
          <div class="col-9">
            <q-input
              outlined
              type="textarea"
              placeholder="Write function..."
              rows="8"
              autogrow
              class="rounded-10 self-center text-weight-light rounded-input"
            />
          </div>
        </div>
      </q-card-section>
      <q-separator />
      <q-card-section class="flex justify-end q-gutter-lg q-pa-lg">
        <q-btn
          unelevated
          label="Cancel"
          :ripple="false"
          color="dark"
          class="text-capitalize rounded-10 highlighted-border"
        />
        <q-btn
          unelevated
          label="Add Function"
          icon="add"
          :ripple="false"
          class="bg-light-green rounded-10 text-white text-capitalize self-center"
        />
      </q-card-section>
    </q-card>
  </q-dialog>
</template>
<script>
import { defineComponent } from "vue";
import CommonTable from "../shared/CommonTable.vue";
import DataTypeTable from "../shared/DataTypeTable.vue";

export default defineComponent({
  nname: "FunctionComponent",
  components: {
    CommonTable,
    DataTypeTable,
  },
  data() {
    return {
      isDeleteDialogOpen: false,
      addNewFunction: false,
      selectedRow: null,
      functionTypeVal: "embedded",
      selectedLanguage: "PHP",
      languageOptions: ["PHP", "JavaScript", "R", "SQL", "Python"],
      tableColumns: [
        { name: "name", label: "Name", align: "left", field: "name" },
        {
          name: "parameters",
          label: "Parameters",
          align: "left",
          field: "parameters",
        },
        {
          name: "returnType",
          label: "Return Type",
          align: "center",
          field: "returnType",
          sortable: true,
        },
        {
          name: "language",
          label: "Language",
          align: "center",
          field: "language",
          sortable: true,
        },
        {
          name: "type",
          label: "Type",
          align: "center",
          field: "type",
          sortable: true,
        },
        {
          name: "bodyOrExternalName",
          label: "Body/ External Name",
          align: "center",
          field: "bodyOrExternalName",
          sortable: true,
        },
        { name: "actions", label: "Actions", align: "center" },
      ],
      tableData: [
        {
          id: 1,
          name: "Example Data Table",
          parameters: "Lorem ipsum",
          returnType: "Lorem ipsum",
          language: "PHP",
          type: "External",
        },
        {
          id: 2,
          name: "Example Data Table",
          parameters: "Lorem ipsum",
          returnType: "Lorem ipsum",
          language: "JavaScript",
          type: "Embedded",
        },
        {
          id: 3,
          name: "Example Data Table",
          parameters: "Lorem ipsum",
          returnType: "Lorem ipsum",
          language: "R",
          type: "External",
        },
        {
          id: 4,
          name: "Example Data Table",
          parameters: "Lorem ipsum",
          returnType: "Lorem ipsum",
          language: "PHP",
          type: "Embedded",
        },
        {
          id: 5,
          name: "Example Data Table",
          parameters: "Lorem ipsum",
          returnType: "Lorem ipsum",
          language: "SQL",
          type: "External",
        },
        {
          id: 6,
          name: "Example Data Table",
          parameters: "Lorem ipsum",
          returnType: "Lorem ipsum",
          language: "Python",
          type: "Embedded",
        },
      ],
      functionTypeRow: [
        { name: "id", type: "num", defaultValue: "id" },
        { name: "name", type: "string", defaultValue: "" },
        { name: "", type: "", defaultValue: "" },
      ],
      functionTypeColumns: [
        {
          name: "name",
          required: true,
          label: "Name",
          align: "left",
          field: "name",
        },
        { name: "type", label: "Type", align: "left", field: "type" },
        {
          name: "defaultValue",
          label: "Default Value",
          align: "left",
          field: "defaultValue",
        },
        // { name: "primary", label: "Primary", align: "center", field: "primary" },
        { name: "actions", label: "Actions", align: "center" },
      ],
      functionTypeOptions: ["num", "string", "timestamps()", "int", "float"],
    };
  },
  methods: {
    openDeleteDialog(row) {
      this.selectedRow = row;
      this.isDeleteDialogOpen = true;
    },
    confirmDelete() {
      // Handle deletion logic here
      this.isDeleteDialogOpen = false;
      this.selectedRow = null;
    },
    openFunctionDialog() {
      this.addNewFunction = !this.addNewFunction;
    },
    addRow() {
      this.functionTypeRow.push({ name: "", type: "", defaultValue: "" });
    },
    removeRow(row) {
      this.functionTypeRow = this.functionTypeRow.filter((r) => r !== row);
    },
  },
});
</script>
<style scoped lang="scss">
.q-dialog__inner {
  .q-card {
    border-radius: 15px;
    background-color: var(--q-color-bg);
    box-shadow: none;

    .q-card__actions {
      .q-btn--rectangle {
        min-width: 80px;
      }
    }
  }
}
</style>
