<template>
  <div class="q-pa-lg">
    <common-table
      title="External & Embedded"
      description="Create and manage your functions."
      :columns="tableColumns"
      :rows="tableData"
      buttonLabel="Add function"
      searchInputPlaceholder="Functions"
      @edit-row="openEditDialog"
      @view-row="openEditDialog"
      @delete-row="openDeleteDialog"
      @add-new="openFunctionDialog"
      :isShowEdit="false"
    />
  </div>

  <q-dialog
    v-model="addNewFunction"
    persistent
    full-height
    maximized
    position="right"
    backdrop-filter="blur(4px)"
    class="add-new-dialog function-add-new-dialog"
  >
    <q-card class="full-height">
      <q-form
        @submit="addFunction"
        @reset="resetFunction"
        ref="addFunctionForm"
      >
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
        <q-card-section class="q-py-lg px-28">
          <div class="row items-center">
            <div class="col-3 flex items-center">
              <span
                class="text-custom-gray-dark text-subtitle1 text-weight-light"
                >Embedded</span
              >
            </div>
            <div class="col-9">
              <q-radio
                dense
                v-model="functionInfo.functionType"
                val="embedded"
                color="light-green"
              />
            </div>
          </div>
          <div class="row items-center q-mt-md">
            <div class="col-3 flex items-center">
              <span
                class="text-custom-gray-dark text-subtitle1 text-weight-light"
                >External</span
              >
            </div>
            <div class="col-9">
              <q-radio
                dense
                v-model="functionInfo.functionType"
                val="external"
                color="light-green"
              />
            </div>
          </div>
          <div class="row items-center q-mt-md">
            <div class="col-3 flex items-center">
              <span
                class="text-custom-gray-dark text-subtitle1 text-weight-light"
                >ZFunction</span
              >
            </div>
            <div class="col-9">
              <q-radio
                dense
                v-model="functionInfo.functionType"
                val="zfunction"
                color="light-green"
              />
            </div>
          </div>
        </q-card-section>
        <q-separator />
        <q-card-section
          v-if="
            functionInfo.functionType == 'embedded' ||
            functionInfo.functionType == 'zfunction'
          "
          class="q-py-sm"
        >
          <div class="row items-start">
            <div class="col-3">
              <span
                class="text-custom-gray-dark text-subtitle1 text-weight-light"
                >Body</span
              >
            </div>
            <div class="col-9">
              <q-input
                outlined
                v-model="functionInfo.body"
                type="textarea"
                placeholder="Write function..."
                rows="8"
                autogrow
                class="rounded-10 self-center text-weight-light rounded-input"
                :rules="[
                  (val) =>
                    functionInfo.functionType !== 'external' ||
                    !!val ||
                    'Body is required',
                ]"
              />
            </div>
          </div>
        </q-card-section>
        <q-card-section class="q-pb-xl">
          <div class="row items-start">
            <div class="col-3">
              <span
                class="text-custom-gray-dark text-subtitle1 text-weight-light"
                >Language</span
              >
            </div>
            <div class="col-9">
              <q-select
                v-model="functionInfo.language"
                :options="filteredLanguageOptions"
                outlined
                dense
                placeholder="Select Language"
                dropdown-icon="keyboard_arrow_down"
                class="rounded-input"
                :rules="[(val) => !!val || 'Field is required']"
                @update:model-value="getExternalFunction"
              />
            </div>
          </div>
          <div
            v-if="functionInfo.functionType !== 'external'"
            class="row items-start"
          >
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
                v-model="functionInfo.name"
                placeholder="Function Name"
                class="rounded-10 self-center text-weight-light rounded-input"
                :rules="[(val) => !!val || 'Field is required']"
              />
            </div>
          </div>
          <div
            v-if="functionInfo.functionType === 'external'"
            class="row items-start"
          >
            <div class="col-3">
              <span
                class="text-custom-gray-dark text-subtitle1 text-weight-light"
                >Name</span
              >
            </div>
            <div class="col-9">
              <q-select
                v-model="functionInfo.name"
                :options="externalFunctionName"
                outlined
                dense
                placeholder="Select Function Name"
                dropdown-icon="keyboard_arrow_down"
                class="rounded-input"
                :rules="[(val) => !!val || 'Field is required']"
              />
            </div>
          </div>
          <div
            v-if="functionInfo.functionType === 'zfunction'"
            class="row items-start"
          >
            <div class="col-3">
              <span
                class="text-custom-gray-dark text-subtitle1 text-weight-light"
                >Event Name</span
              >
            </div>
            <div class="col-9">
              <q-select
                v-model="functionInfo.eventName"
                :options="eventTables"
                outlined
                dense
                placeholder="Select Event"
                dropdown-icon="keyboard_arrow_down"
                class="rounded-input"
              />
            </div>
          </div>
        </q-card-section>
        <q-separator v-if="functionInfo.functionType !== 'external'" />
        <q-card-section
          v-if="functionInfo.functionType !== 'external'"
          class="q-py-lg px-28"
        >
          <div class="flex justify-between items-center q-mb-sm">
            <p class="text-custom-text-secondary text-subtitle1 fw-600">
              Return Type
            </p>
          </div>
          <data-type-table
            :columns="functionParamTypeColumns"
            :rows="functionParmaTypeRow"
            :typeOptions="functionTypeOptions"
            @add-row="addReturnRow"
            ref="dataReturnTypeTable"
            @remove-row="removeReturnRow"
            :isSettingShow="false"
            :isMultiSelect="isMultiSelect"
          />
        </q-card-section>
        <q-separator v-if="functionInfo.functionType !== 'external'" />
        <q-card-section
          v-if="functionInfo.functionType !== 'external'"
          class="q-py-lg px-28"
        >
          <div class="flex justify-between items-center q-mb-sm">
            <p class="text-custom-text-secondary text-subtitle1 fw-600">
              Parameters
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
            ref="dataTypeTable"
            @remove-row="removeRow"
            :isSettingShow="false"
          />
        </q-card-section>
        <q-separator />
        <q-card-section class="flex justify-end q-gutter-lg q-pa-lg">
          <q-btn
            unelevated
            label="Cancel"
            :ripple="false"
            color="dark"
            @click="addNewFunction = false"
            class="text-capitalize rounded-10 highlighted-border"
          />
          <q-btn
            unelevated
            label="Add Function"
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
            Delete Function?
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
</template>
<script>
import { appGetExternalFunctionDetails } from "src/services/api";
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
      functionInfo: {
        name: "",
        returnType: "",
        language: "",
        functionType: "embedded",
        body: "",
      },
      allOptions: {
        embedded: ["python", "javascript", "rust"],
        external: ["python", "java"],
        zfunction: ["sql"],
      },
      tableColumns: [
        { name: "name", label: "Name", align: "left", field: "name" },
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
          name: "parameters",
          label: "Parameters",
          align: "left",
          field: "parameters",
          width: "200px",
        },
        {
          name: "returnType",
          label: "Return Type",
          align: "center",
          field: "returnType",
          sortable: true,
        },
        { name: "actions", label: "Actions", align: "center" },
      ],
      tableData: [],
      eventTables: [],
      functionTypeRow: [{ name: "", type: "" }],
      functionParmaTypeRow: [{ type: "" }],
      functionDetails: [],
      functionTypeColumns: [
        {
          name: "name",
          required: true,
          label: "Name",
          align: "left",
          field: "name",
        },
        { name: "type", label: "Type", align: "left", field: "type" },
        { name: "actions", label: "Actions", align: "center" },
      ],
      functionTypeOptions: [
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
    };
  },
  mounted() {
    this.$ws.connect(() => {
      this.getFunctionInformations();
      this.$ws.sendMessage(`show tables;`, "get_table");
    });
    this.$ws.addMessageHandler((data) => {
      if (data.type == "get_function_name") {
        console.log(data.data);
      }
      if (data.type == "get_function") {
        this.tableData = data.data.map((x, i) => ({
          id: i + 1,
          name: x.Name,
          zfunction: false,
          type: x.Link ? "External" : "Embedded",
          parameters: x.Arguments,
          returnType: x["Return Type"],
          language: x.Language,
          rows: x.total_rows,
        }));
      }
      if (data.type == "get_table") {
        this.eventTables = data.data.map((x) => x.Name);
      }
      if (data.type == "get_zfunction") {
        this.tableData = this.tableData.filter((x) => !x.zfunction);
        this.tableData = [
          ...this.tableData,
          ...data.data.map((x, i) => ({
            id: i + 1,
            name: x.Name,
            zfunction: true,
            type: "Z Function",
            parameters: x.Arguments,
            returnType: x["Return Type"],
            language: x.Language,
            rows: x.total_rows,
          })),
        ];
      }
      if (data.type == "create_function" || data.type == "drop_function") {
        this.getFunctionInformations();
      }
    });
  },
  beforeUnmount() {
    this.$ws.removeAll();
  },
  methods: {
    getExternalFunction() {
      if (this.functionInfo.functionType == "external") {
        appGetExternalFunctionDetails(this.functionInfo.language).then(
          ({ data }) => {
            this.functionDetails = data;
            const existingFunctions = this.tableData
              .filter((x) => x.type == "External")
              .map((x) => x.name);
            this.functionDetails = this.functionDetails.filter(
              (x) => !existingFunctions.some((y) => y == x.name)
            );
          }
        );
      }
    },
    addFunction() {
      const hasValidData = this.functionTypeRow.some(
        (row) => row.name.trim() && row.type.trim()
      );

      if (!hasValidData && this.functionInfo.functionType != "external") {
        this.$q.notify({
          type: "negative",
          message: "Please fill in at least one row.",
          position: "top-right",
        });
        return;
      }

      const query =
        this.functionInfo.functionType == "external"
          ? this.generateExternalFunction()
          : this.functionInfo.functionType == "zfunction"
          ? this.generateZFunction()
          : this.generateEmbeddedFunction();
      this.$ws.sendMessage(query, "create_function");
      this.addNewFunction = false;
      this.$refs.addFunctionForm.reset();
    },
    resetFunction() {
      this.functionInfo = {
        name: "",
        returnType: "",
        language: "",
        functionType: "embedded",
        body: "",
      };

      this.functionTypeRow = [{ name: "", type: "", defaultValue: "" }];
      this.functionParmaTypeRow = [{ type: "" }];
      this.$nextTick(() => {
        if (this.$refs.dataTypeTable) {
          this.$refs.dataTypeTable.rows = this.functionTypeRow;
        }
        if (this.$refs.dataReturnTypeTable) {
          this.$refs.dataReturnTypeTable.rows = this.functionParmaTypeRow;
        }
      });
    },
    dropFunction() {
      if (this.selectedRow.zfunction) {
        this.$ws.sendMessage(
          `DROP ZFUNCTION ${this.selectedRow.name};`,
          "drop_function"
        );
      } else {
        this.$ws.sendMessage(
          `DROP FUNCTION ${this.selectedRow.name};`,
          "drop_function"
        );
      }
    },
    getFunctionInformations() {
      this.$ws.sendMessage(`SHOW FUNCTIONS;`, "get_function");
      this.$ws.sendMessage(`SHOW ZFUNCTIONS;`, "get_zfunction");
    },
    generateZFunction() {
      const params = this.$refs.dataTypeTable.rows
        .filter((x) => x.name && x.type)
        .map((param) => {
          const { name, type, defaultValue } = param;
          return defaultValue
            ? `${name} ${type} DEFAULT ${defaultValue}`
            : `${name} ${type}`;
        })
        .join(", ");

      return `
      CREATE ZFUNCTION ${
        this.functionInfo.name
      }(${params}) RETURNS TABLE(${this.functionParmaTypeRow
        .filter((x) => x.type && x.name)
        .map((x) => `${x.name} ${x.type}`)
        .join(", ")})
      LANGUAGE ${this.functionInfo.language}
      AS $$
        ${this.functionInfo.body}
      $$
      WITH (
        EVENTS = '${this.functionInfo.eventName}'
      );`;
    },
    generateExternalFunction() {
      const functions = this.functionDetails.find(
        (x) => x.name == this.functionInfo.name
      );
      if (functions) {
        const rows = functions.input_type.map((x) => ({
          name: x.name,
          type:
            x.type == "string"
              ? "varchar"
              : x.type == "double"
              ? "double precision"
              : x.type,
        }));
        const params = rows
          .filter((x) => x.type)
          .map((param) => {
            const { name, type } = param;
            return type;
          })
          .join(", ");

        return `
      CREATE FUNCTION ${
        this.functionInfo.name
      }(${params}) RETURNS ${functions.result_type[0].type
          ?.replaceAll("string", "varchar")
          .replaceAll("double", "double precision")
          .replaceAll(": ", " ")}
      LANGUAGE ${this.functionInfo.language}
      AS '${this.functionInfo.name}';`;
      }
    },
    generateEmbeddedFunction() {
      const params = this.$refs.dataTypeTable.rows
        .filter((x) => x.name && x.type)
        .map((param) => {
          const { name, type, defaultValue } = param;
          return defaultValue
            ? `${name} ${type} DEFAULT ${defaultValue}`
            : `${name} ${type}`;
        })
        .join(", ");

      return `
      CREATE FUNCTION ${
        this.functionInfo.name
      }(${params}) RETURNS ${this.functionParmaTypeRow
        .filter((x) => x.type)
        .map((x) => x.type)
        .join(", ")}
        LANGUAGE ${this.functionInfo.language}
      AS $$
        ${this.functionInfo.body}
      $$;`;
    },
    openEditDialog(row) {
      this.functionInfo = {
        name: row.name,
        returnType: row.returnType,
        language: row.language,
        functionType: row.link ? "external" : "embedded",
        body: row.link,
      };
      this.functionTypeRow = [{ name: "", type: "", defaultValue: "" }];
      const parameters = row.parameters?.split(",");
      if (parameters?.length) {
        this.functionTypeRow = parameters.map((x) => ({
          name: "",
          type: x.trim(),
          defaultValue: "",
        }));
      }

      this.addNewFunction = true;
    },
    openDeleteDialog(row) {
      this.selectedRow = row;
      this.isDeleteDialogOpen = true;
    },
    confirmDelete() {
      this.dropFunction();
      this.isDeleteDialogOpen = false;
      this.selectedRow = null;
    },
    openFunctionDialog() {
      this.addNewFunction = !this.addNewFunction;
      this.resetFunction();
    },
    addRow() {
      this.functionTypeRow.push({ name: "", type: "", defaultValue: "" });
    },
    removeRow(row) {
      this.functionTypeRow = this.functionTypeRow.filter((r) => r !== row);
    },
    addReturnRow() {
      const row = this.isMultiSelect ? { type: [], name: "" } : { type: "" };
      this.functionParmaTypeRow.push(row);
    },
    removeReturnRow(row) {
      this.functionParmaTypeRow = this.functionParmaTypeRow.filter(
        (r) => r !== row
      );
    },
  },
  computed: {
    externalFunctionName() {
      return this.functionDetails.map((x) => x.name);
    },
    filteredLanguageOptions() {
      return this.functionInfo.functionType
        ? this.allOptions[this.functionInfo.functionType]
        : [];
    },
    isMultiSelect() {
      return (
        this.functionInfo.functionType === "external" ||
        this.functionInfo.functionType === "zfunction"
      );
    },
    functionParamTypeColumns() {
      const baseColumns = [
        { name: "type", label: "Type", align: "left", field: "type" },
      ];
      if (this.isMultiSelect) {
        baseColumns.splice(0, 0, {
          name: "name",
          label: "Name",
          align: "left",
          field: "name",
        });
        baseColumns.push({
          name: "actions",
          label: "Actions",
          align: "center",
        });
      }
      return baseColumns;
    },
  },
  watch: {
    "functionInfo.functionType"(newVal, oldVal) {
      this.functionInfo.language = "";
      this.functionInfo.body = "";
    },
    isMultiSelect(newVal) {
      this.functionParmaTypeRow = this.functionParmaTypeRow.map((row) => {
        if (newVal) {
          return {
            ...row,
            type: Array.isArray(row.type)
              ? row.type
              : [row.type].filter(Boolean),
            name: row.name || "",
          };
        } else {
          return {
            ...row,
            type: Array.isArray(row.type) ? row.type[0] || "" : row.type,
          };
        }
      });
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
