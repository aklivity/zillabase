<template>
  <div class="q-pa-lg">
    <common-table
      title="All Tables"
      description="Create and manage your tables"
      :columns="tableColumns"
      :rows="tableData"
      buttonLabel="Add Table"
      @edit-row="openEditDialog"
      @delete-row="openDeleteDialog"
      @add-new="openTableDialog"
    />
  </div>

  <q-dialog
    v-model="addNewTable"
    persistent
    full-height
    maximized
    position="right"
    backdrop-filter="blur(4px)"
    class="add-new-dialog"
  >
    <q-card class="full-height">
      <q-form @submit="addTable" @reset="resetTable" ref="addTableForm">
        <q-card-section class="flex justify-between items-center q-pa-lg">
          <div class="flex q-gutter-lg">
            <q-btn
              unelevated
              color="light-green"
              :icon="addNewTable ? 'chevron_left' : 'chevron_right'"
              style="width: 30px; min-height: 30px"
              @click="addNewTable = !addNewTable"
              class="rounded-10"
            />
            <p class="text-custom-text-secondary text-h6 fw-600">
              {{ dialogTitle }}
            </p>
          </div>
          <q-icon
            name="img:/icons/grid-6.svg"
            class="fs-30 filter-custom-dark"
            style="min-height: 30px"
          />
        </q-card-section>
        <q-separator />
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
                v-model="tableInfo.name"
                placeholder="Name"
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
                >ZTable</span
              >
              <div>
                <q-icon
                  name="img:icons/question-circle.svg"
                  class="fs-lg filter-gray-dark q-ml-sm"
                />
                <q-tooltip anchor="bottom middle" self="top middle">
                  A Table creates the topic and CRUD APIs to insert and query data.
                </q-tooltip>
              </div>
            </div>
            <div class="col-9">
              <q-checkbox
                dense
                v-model="tableInfo.zTableType"
                color="light-green"
              />
            </div>
          </div>
        </q-card-section>
        <q-separator />
        <q-card-section class="q-py-lg px-28">
          <div class="flex justify-between items-center q-mb-sm">
            <p class="text-custom-text-secondary text-subtitle1 fw-600">
              Columns
            </p>
            <div>
              <q-tooltip anchor="center left" self="center end">
                Data Type Docs
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
            :columns="dataTypeColumns"
            :rows="dataTypeRow"
            :typeOptions="dataTypeOptions"
            ref="dataTypeTable"
            @add-row="addRow"
            @remove-row="removeRow"
            @setting-row="openRowSettingDialog"
            :isSettingShow="tableInfo.zTableType"
          />
        </q-card-section>
        <q-separator />
        <q-separator />
        <q-card-section class="flex justify-end q-gutter-lg q-pa-lg">
          <q-btn
            unelevated
            label="Cancel"
            @click="addNewTable = !addNewTable"
            :ripple="false"
            color="dark"
            class="text-capitalize rounded-10 highlighted-border"
          />
          <q-btn
            unelevated
            label="Add Table"
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
            Delete Table?
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
          unelevated
          color="dark"
          class="rounded-10 text-capitalize min-w-80 highlighted-border"
          @click="isDeleteDialogOpen = false"
        />
        <q-btn
          label="Delete"
          unelevated
          color="negative"
          class="rounded-10 text-capitalize min-w-80"
          @click="confirmDelete"
        />
      </q-card-actions>
    </q-card>
  </q-dialog>

  <!-- Row Setting Dialog -->
  <q-dialog
    v-model="isRowSettingDialogOpen"
    backdrop-filter="blur(4px)"
    class="row-setting-dialog"
    @update:model-value="closeSettings"
  >
    <q-card class="highlighted-border">
      <q-card-section class="q-px-lg q-pt-lg">
        <div class="flex justify-between items-center no-wrap">
          <div class="flex items-center no-wrap q-gutter-md">
            <q-icon
              size="sm"
              name="img:/icons/setting-2.svg"
              class="filter-custom-white-dark"
            />
            <p class="text-custom-text-secondary fw-600 text-subtitle1">
              Constraints
            </p>
          </div>
          <q-icon
            name="close"
            class="cursor-pointer fs-20"
            @click="
              isRowSettingDialogOpen = false;
              closeSettings();
            "
          />
        </div>
      </q-card-section>
      <q-separator />
      <q-card-section>
        <p
          class="text-custom-text-secondary fw-600 text-subtitle1 q-pb-sm q-pl-sm"
        >
          Generated Always As:
        </p>
        <div class="flex items-start q-gutter-md q-pa-sm">
          <q-radio
            v-model="selectedRow.constraints"
            val="identity"
            dense
            color="light-green"
          />
          <div>
            <p class="text-custom-text-secondary text-weight-medium">
              Identity
            </p>
            <p class="text-custom-gray-dark text-weight-light q-mt-xs">
              Identity value will be auto populated.
            </p>
          </div>
        </div>
        <div class="flex items-start q-gutter-md q-pa-sm">
          <q-radio
            v-model="selectedRow.constraints"
            val="now"
            dense
            color="light-green"
          />
          <div>
            <p class="text-custom-text-secondary text-weight-medium">Now</p>
            <p class="text-custom-gray-dark text-weight-light q-mt-xs">
              Timestamp value will be auto populated.
            </p>
          </div>
        </div>
      </q-card-section>
    </q-card>
  </q-dialog>
</template>
<script>
import { defineComponent } from "vue";
import CommonTable from "../shared/CommonTable.vue";
import DataTypeTable from "../shared/DataTypeTable.vue";
import { showError } from "./../../services/notification";
export default defineComponent({
  name: "TableComponent",
  components: {
    CommonTable,
    DataTypeTable,
  },
  data() {
    return {
      isDeleteDialogOpen: false,
      addNewTable: false,
      isRowSettingDialogOpen: false,
      zTableType: false,
      selectedRow: null,
      activeRowSetting: {},
      tableInfo: {
        name: "",
        description: "",
        zTableType: false,
      },
      tableColumns: [
        { name: "name", label: "Name", align: "left", field: "name" },
        { name: "type", label: "Type", align: "center", field: "type" },
        { name: "actions", label: "Actions", align: "center" },
      ],
      tableData: [],
      zTableData: [],
      tablesLoaded: false,
      zTablesLoaded: false,
      dataTypeRow: [
        { name: "", type: "", defaultValue: "", primary: false, id: 1 },
        {
          name: "",
          type: "",
          defaultValue: "",
          primary: false,
          id: 2,
        },
        {
          name: "",
          type: "",
          defaultValue: "",
          primary: false,
          id: 3,
        },
        { name: "", type: "", defaultValue: "", primary: false, id: 4 },
      ],
      dataTypeColumns: [
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
        {
          name: "primary",
          label: "Primary",
          align: "center",
          field: "primary",
        },
        { name: "actions", label: "Actions", align: "center" },
      ],
      dataTypeOptions: [
        "boolean",
        "smallint",
        "integer",
        "bigint",
        "numeric",
        "real",
        "double precision",
        "varchar",
        "bytea",
        "date",
        "time without time zone",
        "timestamp without time zone",
        "timestamp with time zone",
        "interval",
        "struct",
        "array",
        "map",
        "JSONB",
      ],
      rowSettingData: [
        {
          id: 1,
          primary: true,
          label: "Identity",
          description: "Identity value will be auto populated.",
        },
        {
          id: 2,
          primary: false,
          label: "Now",
          description:
            "Timestamp value will be auto populated.",
        },
      ],
    };
  },
  computed: {
    dialogTitle() {
      return this.selectedRow ? "Edit Table" : "Create New Table";
    },
  },
  mounted() {
    this.$ws.connect(() => {
      this.getTableInformations();
      this.getZTables()
    });
    this.$ws.addMessageHandler((data) => {
      if (
        data.type == "create_table" ||
        data.type == "create_ztable" ||
        data.type == "drop_ztable"
      ) {
        this.getTableInformations();
        this.getZTables();
      }

      this.handleReceivedData(data);
    });
  },
  beforeUnmount() {
    this.$ws.removeAll();
  },
  methods: {
    handleReceivedData(data) {
      if (data.type == "get_table") {
        this.tableData = data.data.map((x, i) => ({
          id: i + 1,
          name: x.Name,
          description: x.table_description,
          columns: x.total_columns,
          rows: x.total_rows,
          type: "Table",
        }));
        this.tablesLoaded = true;
        this.updateTableTypes();
      }

      if (data.type == "get_ztables") {
        this.zTableData = data.data.filter((x) => x.Name).map((item) => item.Name.toLowerCase());
        this.zTablesLoaded = true;
        this.updateTableTypes();
      }
    },
    updateTableTypes() {
      if (this.tablesLoaded && this.zTablesLoaded) {
        this.tableData.forEach((item) => {
          if (this.zTableData.includes(item.name.toLowerCase())) {
            item.type = "ZTable";
          }
        });
      }
    },
    setEditTableInfo(data) {
      this.addNewTable = true;
      this.tableInfo = {
        name: data.find((x) => x.Name == "table description")?.Type,
        description: data.find((x) => x.Name == "table description")
          ?.Description,
        zTableType: this.selectedRow.type == "ZTable",
      };
      const excludeIds = [
        "primary key",
        "distribution key",
        "table description",
      ];
      this.dataTypeRow = [];
      data
        .filter((x) => !excludeIds.includes(x.Name))
        .forEach((item, index) => {
          this.dataTypeRow.push({
            name: item.Name,
            type: item.Type,
            defaultValue: "",
            primary: data.some((x) => x.Type == item.Name),
            id: index + 1,
          });
        });
    },
    getTableInformations() {
      this.tableData = [];
      this.$ws.sendMessage(`show tables;`, "get_table");
    },
    getZTables() {
      this.$ws.sendMessage(`show ztables;`, "get_ztables");
    },
    addTable() {
      const hasValidData = this.dataTypeRow.some(
        (row) => row.name.trim() && row.type.trim()
      );

      if (!hasValidData) {
        this.$q.notify({
          type: "negative",
          message: "Please fill in at least one row.",
          position: "top-right",
        });
        return;
      }

      const columns = this.$refs.dataTypeTable.rows
        .filter((x) => x.name)
        .map((field) => {
          let columnDef = `${field.name} ${field.type.toUpperCase()}`;

          if (field.defaultValue) {
            columnDef += ` DEFAULT '${field.defaultValue}'`;
          }

          if (field.constraints == "identity") {
            columnDef += " GENERATED ALWAYS AS IDENTITY";
          }

          return columnDef;
        });
      if (
        this.tableInfo.zTableType &&
        this.$refs.dataTypeTable.rows.some(
          (field) => field.constraints == "identity" && field.type == "integer"
        )
      ) {
        showError("Integer is not allowed for Identity Column");
        return;
      }
      const primaryKey = this.$refs.dataTypeTable.rows
        .filter((field) => field.primary)
        .map((field) => field.name);
      if (primaryKey.length > 0) {
        columns.push(`PRIMARY KEY (${primaryKey.join(", ")})`);
      }
      if (this.tableInfo.zTableType) {
        const zTableQuery = `CREATE ZTABLE ${
          this.tableInfo.name
        } (${columns.join(",\n    ")});`;
        this.$ws.sendMessage(zTableQuery, "create_ztable");
      } else {
        const query = `CREATE TABLE ${this.tableInfo.name} (${columns.join(
          ",\n    "
        )});`;
        this.$ws.sendMessage(query, "create_table");
      }
      this.addNewTable = false;
      this.$refs.addTableForm.reset();
    },
    resetTable() {
      this.tableInfo = {
        name: "",
        description: "",
        zTableType: false,
      };

      this.dataTypeRow = [
        {
          name: "",
          type: "",
          defaultValue: "",
          primary: false,
          isNullable: true,
          id: 1,
        },
      ];
      this.$nextTick(() => {
        if (this.$refs.dataTypeTable) {
          this.$refs.dataTypeTable.rows = this.dataTypeRow;
        }
      });
    },
    openEditDialog(row) {
      this.selectedRow = row;
      this.$ws.sendMessage(`describe ${row.name};`, "get_table_name");
    },
    openDeleteDialog(row) {
      this.selectedRow = row;
      this.isDeleteDialogOpen = true;
    },
    confirmDelete() {
      this.isDeleteDialogOpen = false;
      if (this.selectedRow.type === "ZTable") {
        this.$ws.sendMessage(
          `DROP ZTABLE ${this.selectedRow.name};`,
          "drop_ztable"
        );
      } else {
        this.$ws.sendMessage(
          `DROP TABLE ${this.selectedRow.name};`,
          "drop_table"
        );
      }
      this.selectedRow = null;
    },
    openTableDialog() {
      this.addNewTable = !this.addNewTable;
      this.resetTable();
    },
    openRowSettingDialog(row) {
      this.selectedRow = row;
      this.isRowSettingDialogOpen = !this.isRowSettingDialogOpen;
    },
    closeSettings() {
      this.activeRowSetting.unique =
        this.rowSettingData.find((x) => x.id == 1)?.primary || false;
      this.activeRowSetting.nullable =
        this.rowSettingData.find((x) => x.id == 2)?.primary || false;
      this.activeRowSetting.identity =
        this.rowSettingData.find((x) => x.id == 3)?.primary || false;
    },
    addRow() {
      this.dataTypeRow.push({
        id: Date.now().valueOf(),
        name: "",
        type: "",
        defaultValue: "",
        primary: false,
        isNullable: true,
      });
    },
    removeRow(row) {
      this.dataTypeRow = this.dataTypeRow.filter((r) => r.id !== row.id);
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

.foreign-key {
  border: 2px dashed var(--q-color-highlight);
}
</style>
