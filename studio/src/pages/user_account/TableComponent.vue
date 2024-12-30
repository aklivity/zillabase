<template>
  <div class="q-pa-lg">
    <common-table
      title="Example Table"
      description="Lorem ipsum dolor sit amet, consectetur adipiscing elit."
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
              Create New Table
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
                placeholder="Table Name"
                class="rounded-10 self-center text-weight-light rounded-input"
                :rules="[(val) => !!val || 'Field is required']"
              />
            </div>
          </div>
          <div class="row items-start q-mt-lg">
            <div class="col-3">
              <span
                class="text-custom-gray-dark text-subtitle1 text-weight-light"
                >Description</span
              >
            </div>
            <div class="col-9">
              <q-input
                outlined
                type="textarea"
                placeholder="Table Description..."
                rows="6"
                v-model="tableInfo.description"
                autogrow
                class="rounded-10 self-center text-weight-light rounded-input"
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
                  Lorem ipsum dolor sit amet, consectetur adipiscing elit.
                </q-tooltip>
              </div>
            </div>
            <div class="col-9">
              <q-checkbox
                dense
                v-model="tableInfo.zTableVal"
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
          />
        </q-card-section>
        <q-separator />
        <!-- <q-card-section class="q-py-lg px-28">
        <div class="flex justify-between items-center q-mb-sm">
          <p class="text-custom-text-secondary text-subtitle1 fw-600">
            Foreign Keys
          </p>
          <div>
            <q-tooltip anchor="center left" self="center end">
              Foreign Key Docs
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
        <div class="flex justify-between q-pa-md foreign-key rounded-10">
          <span class="text-custom-gray-dark">No Foreign Keys</span>
          <q-btn
            unelevated
            color="light-green"
            icon="add"
            style="width: 30px; min-height: 30px"
            class="rounded-10 q-pa-none text-custom-dark-color"
          />
        </div>
      </q-card-section> -->
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
      <q-card-section class="flex justify-between items-center q-pa-lg">
        <div class="flex items-center q-gutter-md">
          <q-icon
            size="sm"
            name="img:/icons/setting-2.svg"
            class="filter-custom-white-dark"
          />
          <p class="text-custom-text-secondary fw-600 text-subtitle1">
            Row Settings
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
      </q-card-section>
      <q-separator />
      <q-card-section>
        <div
          v-for="setting in rowSettingData"
          :key="setting.id"
          class="flex items-start q-gutter-md q-pa-sm"
        >
          <q-checkbox v-model="setting.primary" dense color="light-green" />
          <div>
            <p class="text-custom-text-secondary text-weight-medium">
              {{ setting.label }}
            </p>
            <p class="text-custom-gray-dark text-weight-light q-mt-xs">
              {{ setting.description }}
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
      zTableVal: false,
      selectedRow: null,
      activeRowSetting: {},
      tableInfo: {
        name: "",
        description: "",
        zTableVal: false,
      },
      tableColumns: [
        { name: "name", label: "Table Name", align: "left", field: "name" },
        {
          name: "description",
          label: "Description",
          align: "left",
          field: "description",
        },
        { name: "ztable", label: "ZTable", align: "center", field: "ztable" },
        { name: "rows", label: "Rows", align: "right", field: "rows" },
        { name: "columns", label: "Columns", align: "right", field: "columns" },
        { name: "actions", label: "Actions", align: "center" },
      ],
      tableData: [],
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
        "smallint",
        "integer",
        "bigint",
        "decimal",
        "numeric",
        "real",
        "double precision",
        "serial",
        "bigserial",
        "money",
        "character varying",
        "varchar",
        "character",
        "char",
        "text",
        "bytea",
        "timestamp",
        "timestamp without time zone",
        "timestamp with time zone",
        "date",
        "time",
        "time without time zone",
        "time with time zone",
        "interval",
        "boolean",
        "enum",
        "point",
        "line",
        "lseg",
        "box",
        "path",
        "polygon",
        "circle",
        "cidr",
        "inet",
        "macaddr",
        "macaddr8",
        "json",
        "jsonb",
        "uuid",
        "xml",
      ],
      rowSettingData: [
        {
          id: 1,
          primary: true,
          label: "IsUnique()",
          description:
            "Lorem ipsum dolor sit amet, consectetur adipiscing elit.",
        },
        {
          id: 2,
          primary: false,
          label: "IsNullable()",
          description:
            "Lorem ipsum dolor sit amet, consectetur adipiscing elit.",
        },
        {
          id: 3,
          primary: false,
          label: "IsIdentity()",
          description:
            "Lorem ipsum dolor sit amet, consectetur adipiscing elit.",
        },
      ],
    };
  },
  mounted() {
    this.$ws.connect(() => {
      this.getTableInformations();
    });
    this.$ws.addMessageHandler((data) => {
      if (data.type == "get_table_name") {
        this.setEditTableInfo(data.data);
      }
      if (data.type == "get_table") {
        this.tableData = data.data.map((x, i) => ({
          id: i + 1,
          name: x.Name,
          description: x.table_description,
          columns: x.total_columns,
          rows: x.total_rows,
          ztable: false,
        }));
        this.getZTables();
      }
      if (data.type == "get_ztables") {
        data.data
          .filter((x) => x.Name)
          .forEach((item) => {
            const itemData = this.tableData.find(
              (x) => x.name.toLowerCase() == item.Name.toLowerCase()
            );
            if (itemData) {
              itemData.ztable = true;
            }
          });
      }
      if (
        data.type == "create_table" ||
        data.type == "create_ztable" ||
        data.type == "drop_ztable"
      ) {
        this.getTableInformations();
      }
    });
  },
  beforeUnmount() {
    this.$ws.removeAll();
  },
  methods: {
    setEditTableInfo(data) {
      this.addNewTable = true;
      this.tableInfo = {
        name: data.find((x) => x.Name == "table description")?.Type,
        description: data.find((x) => x.Name == "table description")
          ?.Description,
        zTableVal: this.selectedRow.ztable,
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

          // if (!field.nullable) {
          //   columnDef += " NOT NULL";
          // }

          if (field.defaultValue) {
            columnDef += ` DEFAULT '${field.defaultValue}'`;
          }

          // if (field.identity) {
          //   columnDef += " GENERATED ALWAYS AS IDENTITY";
          // }

          return columnDef;
        });

      const primaryKey = this.$refs.dataTypeTable.rows
        .filter((field) => field.primary)
        .map((field) => field.name);
      if (primaryKey.length > 0) {
        columns.push(`PRIMARY KEY (${primaryKey.join(", ")})`);
      }
      const query = `CREATE OR ALTER TABLE \"${
        this.tableInfo.name
      }\" (${columns.join(",\n    ")});`;
      this.$ws.sendMessage(query, "create_table");
      if (this.tableInfo.zTableVal) {
        const zTableQuery = `CREATE OR ZTABLE \"${
          this.tableInfo.name
        }\" (${columns.join(",\n    ")});`;
        this.$ws.sendMessage(zTableQuery, "create_ztable");
      }
      this.addNewTable = false;
      this.$refs.addTableForm.reset();
    },
    resetTable() {
      this.tableInfo = {
        name: "",
        description: "",
        zTableVal: false,
      };

      this.dataTypeRow = [
        { name: "", type: "", defaultValue: "", primary: false, id: 1 },
        { name: "", type: "", defaultValue: "", primary: false, id: 2 },
        { name: "", type: "", defaultValue: "", primary: false, id: 3 },
        { name: "", type: "", defaultValue: "", primary: false, id: 4 },
      ];
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
      if (this.selectedRow.ztable) {
        this.$ws.sendMessage(
          `DROP ZTABLE ztable_${this.selectedRow.name};`,
          "drop_ztable"
        );
      }
      this.$ws.sendMessage(
        `DROP TABLE ${this.selectedRow.name};`,
        "drop_table"
      );
      this.selectedRow = null;
    },
    openTableDialog() {
      this.addNewTable = !this.addNewTable;
    },
    openRowSettingDialog(row) {
      this.activeRowSetting = this.dataTypeRow.find((x) => x.id == row.id);
      this.isRowSettingDialogOpen = !this.isRowSettingDialogOpen;
      this.rowSettingData.find((x) => x.id == 1).primary =
        this.activeRowSetting.unique || false;
      this.rowSettingData.find((x) => x.id == 2).primary =
        this.activeRowSetting.nullable || true;
      this.rowSettingData.find((x) => x.id == 3).primary =
        this.activeRowSetting.identity || false;
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
