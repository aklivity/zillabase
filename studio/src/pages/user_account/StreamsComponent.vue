<template>
  <div class="q-pa-lg">
    <common-table
      title="ZStreams"
      description="Lorem ipsum dolor sit amet, consectetur adipiscing elit."
      :columns="tableColumns"
      :rows="tableData"
      buttonLabel="Add Stream"
      searchInputPlaceholder="Streams"
      @delete-row="openDeleteDialog"
      @add-new="openTableDialog"
    />
  </div>
  <!-- add Dialog -->
  <q-dialog
    v-model="addNewStream"
    persistent
    full-height
    maximized
    position="right"
    backdrop-filter="blur(4px)"
    class="add-new-dialog"
  >
    <q-card class="full-height" style="width: 871px; max-width: 80vw">
      <q-card-section class="flex justify-between items-center q-pa-lg">
        <div class="flex q-gutter-lg">
          <q-btn
            unelevated
            color="light-green"
            :icon="addNewStream ? 'chevron_left' : 'chevron_right'"
            style="width: 30px; min-height: 30px"
            @click="addNewStream = !addNewStream"
            class="rounded-10"
          />
          <p class="text-custom-text-secondary text-h6 fw-600">
            Create New Stream
          </p>
        </div>
        <q-icon
          name="img:/icons/stream.svg"
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
              placeholder="Stream Name"
              class="rounded-10 self-center text-weight-light rounded-input"
            />
          </div>
        </div>
        <div class="row items-start q-mt-lg q-pt-md">
          <div class="col-3">
            <span class="text-custom-gray-dark text-subtitle1 text-weight-light"
              >Type</span
            >
          </div>
          <div class="col-9">
            <q-select
              v-model="readWrite"
              :options="readWriteOptions"
              outlined
              dense
              placeholder="Read-Write"
              dropdown-icon="keyboard_arrow_down"
              class="rounded-input"
            />
          </div>
        </div>
        <div class="row items-start q-mt-lg q-pt-md">
          <div class="col-3">
            <span class="text-custom-gray-dark text-subtitle1 text-weight-light"
              >Target Function</span
            >
          </div>
          <div class="col-9">
            <q-select
              v-model="exampleFunction"
              :options="exampleFunctionOptions"
              outlined
              dense
              placeholder="ExampleFunction"
              dropdown-icon="keyboard_arrow_down"
              class="rounded-input"
            />
          </div>
        </div>
        <div class="row items-center q-mt-lg q-pt-md">
          <div class="col-3 flex items-center">
            <span class="text-custom-gray-dark text-subtitle1 text-weight-light"
              >Messages</span
            >
          </div>
          <div class="col-9">
            <q-input
              dense
              outlined
              placeholder="34"
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
          label="Add Stream"
          icon="add"
          :ripple="false"
          class="bg-light-green rounded-10 text-white text-capitalize self-center"
        />
      </q-card-section>
    </q-card>
  </q-dialog>

  <!-- Delete Dialog -->
  <q-dialog
    v-model="isDeleteDialogOpen"
    backdrop-filter="blur(4px)"
    class="delete-dialog"
  >
    <q-card class="highlighted-border">
      <q-card-section class="flex justify-between items-center q-pa-lg">
        <div class="flex items-center">
          <q-icon size="sm" name="img:/icons/trash.svg" />
          <p class="text-custom-text-secondary fw-600 q-ml-md text-subtitle1">
            Delete Stream?
          </p>
        </div>
        <q-icon
          name="close"
          class="cursor-pointer fs-20"
          @click="isDeleteDialogOpen = false"
        />
      </q-card-section>
      <q-separator />
      <q-card-section>
        <p class="text-custom-gray-dark text-weight-light q-pa-sm w-90">
          Are you sure you want to delete this
          <span class="fw-600">Stream</span>? This action is irreversible.
        </p>
      </q-card-section>
      <q-separator />
      <q-card-actions align="right" class="q-pa-md">
        <q-btn
          label="Cancel"
          color="dark"
          unelevated
          class="rounded-10 text-capitalize min-w-80 highlighted-border"
          @click="isDeleteDialogOpen = false"
        />
        <q-btn
          label="Delete"
          color="negative"
          unelevated
          class="rounded-10 text-capitalize min-w-80"
          @click="confirmDelete"
        />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>
<script>
import CommonTable from "../shared/CommonTable.vue";
import { defineComponent } from "vue";
export default defineComponent({
  name: "StreamsComponent",
  components: {
    CommonTable,
  },
  data() {
    return {
      isDeleteDialogOpen: false,
      selectedRow: null,
      addNewStream: false,
      zTableVal: true,
      materializedVal: true,
      readWrite: "",
      readWriteOptions: ["1", "2"],
      exampleFunction: "",
      exampleFunctionOptions: ["1", "2"],
      tableColumns: [
        { name: "name", label: "Name", align: "left", field: "name" },
        {
          name: "streamType",
          label: "Type",
          align: "left",
          field: "streamType",
        },
        {
          name: "streamFunction",
          label: "Target Function",
          align: "left",
          field: "streamFunction",
          sortable: true,
        },
        {
          name: "messages",
          label: "Messages",
          align: "left",
          field: "messages",
          sortable: true,
        },
        { name: "actions", label: "Actions", align: "center" },
      ],
      tableData: [
        {
          id: 1,
          name: "Example Stream",
          streamType: "Read",
          streamFunction: "function_1",
          messages: 45,
        },
        {
          id: 2,
          name: "Example Stream",
          streamType: "Write",
          streamFunction: "function_2",
          messages: 555,
        },
        {
          id: 3,
          name: "Example Stream",
          streamType: "Read-Write",
          streamFunction: "exampleFunction",
          messages: 32,
        },
        {
          id: 4,
          name: "Example Stream",
          streamType: "Request",
          streamFunction: "function_3",
          messages: 65,
        },
        {
          id: 5,
          name: "Example Stream",
          streamType: "Response",
          streamFunction: "newFunction",
          messages: 123,
        },
        {
          id: 6,
          name: "Example Stream",
          streamType: "Request-Response",
          streamFunction: "testFunc",
          messages: 9,
        },
      ],
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
    openTableDialog() {
      this.addNewStream = !this.addNewStream;
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
