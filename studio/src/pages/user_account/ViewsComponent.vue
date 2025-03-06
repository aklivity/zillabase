<template>
  <div class="q-pa-lg">
    <common-table
      title="All Views"
      description="Create and manage your views."
      :columns="tableColumns"
      :rows="tableData"
      buttonLabel="Add View"
      searchInputPlaceholder="Views"
      @edit-row="openEditDialog"
      @delete-row="openDeleteDialog"
      @add-new="openTableDialog"
      :isShowEdit="false"
    />
  </div>
  <!-- add Dialog -->
  <q-dialog
    v-model="addNewView"
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
            :icon="addNewView ? 'chevron_left' : 'chevron_right'"
            style="width: 30px; min-height: 30px"
            @click="addNewView = !addNewView"
            class="rounded-10"
          />
          <p class="text-custom-text-secondary text-h6 fw-600">
            Create New View
          </p>
        </div>
        <q-icon
          name="img:/icons/view.svg"
          class="fs-30 filter-custom-dark"
          style="min-height: 30px"
        />
      </q-card-section>
      <q-separator />
      <q-form @submit="createViews" @reset="resetViews" ref="addViewsForm">
        <q-card-section class="q-py-xl px-28">
          <div class="row items-start">
            <div class="col-3">
              <span
                class="text-custom-gray-dark text-subtitle1 text-weight-light"
                >Name</span
              >
            </div>
            <div class="col-9">
              <q-input
                dense
                outlined
                placeholder="Name"
                v-model="viewInfo.name"
                class="rounded-10 self-center text-weight-light rounded-input"
                :rules="[(val) => !!val || 'Field is required']"
              />
            </div>
          </div>
          <div class="row items-start q-mt-lg">
            <div class="col-3">
              <span
                class="text-custom-gray-dark text-subtitle1 text-weight-light"
                >SQL Query</span
              >
            </div>
            <div class="col-9">
              <q-input
                outlined
                type="textarea"
                placeholder="Write SELECT Query..."
                v-model="viewInfo.body"
                rows="8"
                autogrow
                class="rounded-10 self-center text-weight-light rounded-input"
                :rules="[(val) => !!val || 'Field is required']"
              />
            </div>
          </div>
        </q-card-section>

        <q-separator />
        <q-card-section class="q-py-lg px-28">
          <div class="row items-center">
            <div class="col-3 flex items-center">
              <span
                class="text-custom-gray-dark text-subtitle1 text-weight-light"
                >ZView</span
              >
              <div>
                <q-icon
                  name="img:icons/question-circle.svg"
                  class="fs-lg filter-gray-dark q-ml-sm"
                />
                <q-tooltip anchor="bottom middle" self="top middle">
                  A Stream creates the topic and APIs to fetch and stream data.
                </q-tooltip>
              </div>
            </div>
            <div class="col-9">
              <q-radio
                dense
                v-model="viewInfo.selectionType"
                val="zview"
                color="light-green"
              />
            </div>
          </div>
          <div class="row items-center q-mt-md">
            <div class="col-3 flex items-center">
              <span
                class="text-custom-gray-dark text-subtitle1 text-weight-light"
                >Materialized View</span
              >
            </div>
            <div class="col-9">
              <q-radio
                dense
                color="light-green"
                v-model="viewInfo.selectionType"
                val="material"
              />
            </div>
          </div>
          <div class="row items-center q-mt-md">
            <div class="col-3 flex items-center">
              <span
                class="text-custom-gray-dark text-subtitle1 text-weight-light"
                >Views</span
              >
            </div>
            <div class="col-9">
              <q-radio
                dense
                color="light-green"
                v-model="viewInfo.selectionType"
                val="views"
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
            @click="addNewView = false"
            color="dark"
            class="text-capitalize rounded-10 highlighted-border"
          />
          <q-btn
            unelevated
            label="Add View"
            icon="add"
            :ripple="false"
            type="submit"
            class="bg-light-green rounded-10 text-white text-capitalize self-center"
          />
        </q-card-section>
      </q-form>
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
            Delete View?
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
          <span class="fw-600">{{ this.selectedRow.name }}</span
          >? This action is irreversible.
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
import { defineComponent } from "vue";
import CommonTable from "../shared/CommonTable.vue";
export default defineComponent({
  name: "ViewsComponent",
  components: {
    CommonTable,
  },
  data() {
    return {
      isDeleteDialogOpen: false,
      selectedRow: null,
      addNewView: false,
      viewInfo: {
        name: "",
        description: "",
        body: "",
        selectionType: "",
        views: "",
      },
      tableColumns: [
        { name: "name", label: "Name", align: "left", field: "name" },
        {
          name: "type",
          label: "Type",
          align: "center",
          field: "type",
          sortable: true,
        },
        { name: "actions", label: "Actions", align: "center" },
      ],
      tableData: [],
      viewsData: [],
      materializedViewsData: [],
      zViewsData: [],
    };
  },
  mounted() {
    this.$ws.connect(() => {
      this.loadAllViews();
    });
    this.$ws.addMessageHandler((data) => {
      if (data.type === "get_views") {
        this.viewsData = data.data.map((item) => ({
          ...item,
          name: item.Name,
          type: "View",
        }));
        this.updateTableData();
      }
      if (data.type === "get_materialized_views") {
        this.materializedViewsData = data.data.map((item) => ({
          ...item,
          name: item.Name,
          type: "Materialized View",
        }));
        this.updateTableData();
      }
      if (data.type === "get_z_views") {
        this.zViewsData = data.data.map((item) => ({
          ...item,
          name: item.Name,
          type: "ZView",
        }));
        this.updateTableData();
      }

      if (data.type === "create_view" || data.type === "drop_view") {
        this.getViews();
      }

      if (data.type === "create_zview" || data.type === "drop_zview") {
        this.getZViews();
      }

      if (data.type === "create_materialized_view" || data.type === "drop_materialized_view") {
        this.getMaterializedViews();
      }
    });
  },
  beforeUnmount() {
    this.$ws.removeAll();
  },
  methods: {
    createViews() {
      if (this.viewInfo.selectionType === "material") {
        this.createMaterializedView();
      } else if (this.viewInfo.selectionType === "zview") {
        this.createZView();
      } else {
        this.createView();
      }
      this.addNewView = false;
      this.$refs.addViewsForm.reset();
    },
    resetViews() {
      this.viewInfo = {
        name: "",
        description: "",
        body: "",
        selectionType: "",
      };
    },
    createMaterializedView() {
      const query = `CREATE MATERIALIZED VIEW ${this.viewInfo.name} AS ${this.viewInfo.body};`;
      this.$ws.sendMessage(query, "create_materialized_view");
    },
    createZView() {
      const query = `CREATE ZVIEW ${this.viewInfo.name} AS ${this.viewInfo.body};`;
      this.$ws.sendMessage(query, "create_zview");
    },
    createView() {
      const query = `CREATE VIEW ${this.viewInfo.name} AS ${this.viewInfo.body};`;
      this.$ws.sendMessage(query, "create_view");
    },
    getViews() {
      this.$ws.sendMessage(`show views;`, "get_views");
    },
    getMaterializedViews() {
      this.$ws.sendMessage(`show materialized views;`, "get_materialized_views");
    },
    getZViews() {
      this.$ws.sendMessage(`show zviews;`, "get_z_views");
    },
    loadAllViews() {
      this.getViews();
      this.getZViews();
      this.getMaterializedViews();
    },
    updateTableData() {
      this.tableData = [...this.viewsData, ...this.materializedViewsData, ...this.zViewsData];
    },
    openEditDialog(row) {
      this.$ws.sendMessage(`describe ${row.name};`, "get_view_name");
    },
    openDeleteDialog(row) {
      this.selectedRow = row;
      this.isDeleteDialogOpen = true;
    },
    confirmDelete() {
      if (this.selectedRow.type === "ZView") {
        this.$ws.sendMessage(`DROP ZVIEW ${this.selectedRow.name};`, "drop_zview");
      } else if (this.selectedRow.type === "Materialized View") {
        this.$ws.sendMessage(`DROP MATERIALIZED VIEW ${this.selectedRow.name};`, "drop_materialized_view");
      } else if (this.selectedRow.type === "View") {
        this.$ws.sendMessage(`DROP VIEW ${this.selectedRow.name};`, "drop_view");
      }
      this.isDeleteDialogOpen = false;
      this.selectedRow = null;
    },
    openTableDialog() {
      this.addNewView = !this.addNewView;
      this.resetViews();
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
