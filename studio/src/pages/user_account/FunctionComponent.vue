<template>
  <div class="q-pa-lg">
    <common-table
      title="External & Embedded"
      description="Lorem ipsum dolor sit amet, consectetur adipiscing elit."
      :columns="tableColumns"
      :rows="tableData"
      buttonLabel="Add function"
      searchInputPlaceholder="Functions"
      @edit-row="openEditDialog"
      @view-row="openEditDialog"
      @delete-row="openDeleteDialog"
      @add-new="openFunctionDialog"
      :tableName="'function-table'"
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
                v-model="functionInfo.name"
                placeholder="Function Name"
                class="rounded-10 self-center text-weight-light rounded-input"
                :rules="[(val) => !!val || 'Field is required']"
              />
            </div>
          </div>
          <div class="row items-start q-mt-lg">
            <div class="col-3">
              <span
                class="text-custom-gray-dark text-subtitle1 text-weight-light"
                >Return Type</span
              >
            </div>
            <div class="col-9">
              <q-input
                dense
                outlined
                v-model="functionInfo.returnType"
                placeholder="Function Return Type"
                class="rounded-10 self-center text-weight-light rounded-input"
                :rules="[(val) => !!val || 'Field is required']"
              />
            </div>
          </div>
          <div class="row items-start q-mt-lg">
            <div class="col-3">
              <span
                class="text-custom-gray-dark text-subtitle1 text-weight-light"
                >Language</span
              >
            </div>
            <div class="col-9">
              <q-select
                v-model="functionInfo.language"
                :options="languageOptions"
                outlined
                dense
                placeholder="Select Language"
                dropdown-icon="keyboard_arrow_down"
                class="rounded-input"
                :rules="[(val) => !!val || 'Field is required']"
              />
            </div>
          </div>
        </q-card-section>
        <q-separator />
        <q-card-section class="q-py-lg px-28">
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
                v-model="functionInfo.functionType"
                val="external"
                color="light-green"
              />
            </div>
          </div>
        </q-card-section>
        <q-separator v-if="functionInfo.functionType == 'external'" />
        <q-card-section
          v-if="functionInfo.functionType == 'external'"
          class="q-py-lg px-28"
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
      languageOptions: ["php", "javaScript", "r", "sql", "python"],
      tableColumns: [
        { name: "name", label: "Name", align: "left", field: "name" },
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
        { name: "actions", label: "Actions", align: "center" },
      ],
      tableData: [],
      functionTypeRow: [{ name: "", type: "", defaultValue: "" }],
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
        { name: "actions", label: "Actions", align: "center" },
      ],
      functionTypeOptions: [
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
        "boolean",
        "date",
        "timestamp",
        "timestamp with time zone",
        "timestamp without time zone",
        "time",
        "time with time zone",
        "time without time zone",
        "interval",
        "uuid",
        "json",
        "jsonb",
        "xml",
        "array",
        "cidr",
        "inet",
        "macaddr",
        "macaddr8",
        "point",
        "line",
        "lseg",
        "box",
        "path",
        "polygon",
        "circle",
        "tsvector",
        "tsquery",
        "uuid",
        "bit",
        "bit varying",
        "hstore",
        "range types (int4range, int8range, numrange, tsrange, tstzrange, daterange)",
        "composite types",
        "custom types",
      ],
    };
  },
  mounted() {
    this.$ws.connect(() => {
      this.getFunctionInformations();
    });
    this.$ws.addMessageHandler((data) => {
      if (data.type == "get_function_name") {
        console.log(data.data);
      }
      if (data.type == "get_function") {
        this.tableData = data.data.map((x, i) => ({
          id: i + 1,
          name: x.Name,
          parameters: x.Arguments,
          returnType: x["Return Type"],
          language: x.Language,
          rows: x.total_rows,
          ztable: false,
          type: x.Link ? "Embedded" : "External",
        }));
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
    addFunction() {
      const hasValidData = this.functionTypeRow.some(
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

      const query =
        this.functionInfo.functionType == "external"
          ? this.generateExternalFunction()
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

      this.functionTypeRow = [
        { name: "", type: "", defaultValue: "" },
        { name: "", type: "", defaultValue: "" },
        { name: "", type: "", defaultValue: "" },
      ];
    },
    dropFunction() {
      this.$ws.sendMessage(
        `DROP FUNCTION \"${this.selectedRow.name}\";`,
        "drop_function"
      );
    },
    getFunctionInformations() {
      this.$ws.sendMessage(`SHOW FUNCTIONS;`, "get_function");
    },
    generateExternalFunction() {
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
      CREATE OR ALTER FUNCTION ${this.functionInfo.name}(${params}) RETURNS ${this.functionInfo.returnType}
      LANGUAGE ${this.functionInfo.language} 
      AS $$
        return ${this.functionInfo.body}
      $$;`;
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
      CREATE OR ALTER FUNCTION ${this.functionInfo.name}(${params}) RETURNS ${this.functionInfo.returnType}
        LANGUAGE ${this.functionInfo.language} 
      AS '${this.functionInfo.name}';`;
    },
    openEditDialog(row) {
      this.functionInfo = {
        name: row.name,
        returnType: row.returnType,
        language: row.language,
        functionType: row.link ? "embedded" : "external",
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
