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
      @edit-row="openEditDialog"
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
      <q-form @submit="addStream" @reset="resetStream" ref="addStreamForm">
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
                v-model="streamInfo.name"
                dense
                outlined
                placeholder="Stream Name"
                class="rounded-10 self-center text-weight-light rounded-input"
                :rules="[(val) => !!val || 'Field is required']"
              />
            </div>
          </div>
          <div class="row items-start q-mt-lg">
            <div class="col-3">
              <span
                class="text-custom-gray-dark text-subtitle1 text-weight-light"
                >Type</span
              >
            </div>
            <div class="col-9">
              <q-select
                v-model="streamInfo.type"
                :options="dataTypeOptions"
                outlined
                dense
                placeholder="Read-Write"
                dropdown-icon="keyboard_arrow_down"
                class="rounded-input"
                :rules="[(val) => !!val || 'Field is required']"
              />
            </div>
          </div>
          <div class="row items-start q-mt-lg">
            <div class="col-3">
              <span
                class="text-custom-gray-dark text-subtitle1 text-weight-light"
                >Target Function</span
              >
            </div>
            <div class="col-9">
              <q-select
                v-model="streamInfo.targetFunction"
                :options="exampleFunctionOptions"
                outlined
                dense
                multiple
                placeholder="ExampleFunction"
                dropdown-icon="keyboard_arrow_down"
                class="rounded-input"
                :rules="[(val) => !!val || 'Field is required']"
                option-value="value"
                option-label="label"
              >
                <template v-slot:option="scope">
                  <q-item
                    v-bind="scope.itemProps"
                    clickable
                    v-ripple
                    class="q-pl-sm"
                  >
                    <q-checkbox
                      v-model="scope.selected"
                      label=" "
                      color="primary"
                      :value="scope.opt.value"
                    />
                    <q-item-section>
                      {{ scope.opt.label }}
                    </q-item-section>
                  </q-item>
                </template>
              </q-select>
            </div>
          </div>
          <div class="row items-start q-mt-lg">
            <div class="col-3">
              <span
                class="text-custom-gray-dark text-subtitle1 text-weight-light"
                >Reply Function</span
              >
            </div>
            <div class="col-9">
              <q-input
                v-model="streamInfo.replyFunction"
                dense
                outlined
                placeholder="Reply Function"
                class="rounded-10 self-center text-weight-light rounded-input"
                :rules="[(val) => !!val || 'Field is required']"
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
                Stream Docs
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
            :isSettingShow="false"
            @add-row="addRow"
            @remove-row="removeRow"
          />
        </q-card-section>
        <q-separator />
        <q-card-section class="flex justify-end q-gutter-lg q-pa-lg">
          <q-btn
            unelevated
            label="Cancel"
            :ripple="false"
            color="dark"
            @click="addNewStream = !addNewStream"
            class="text-capitalize rounded-10 highlighted-border"
          />
          <q-btn
            unelevated
            label="Add Stream"
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
import CommonTable from "../shared/CommonTable.vue";
import { defineComponent } from "vue";
import DataTypeTable from "../shared/DataTypeTable.vue";
export default defineComponent({
  name: "StreamsComponent",
  components: {
    CommonTable,
    DataTypeTable,
  },
  data() {
    return {
      isDeleteDialogOpen: false,
      selectedRow: null,
      addNewStream: false,
      zTableVal: true,
      materializedVal: true,
      readWrite: "",
      exampleFunction: "",
      exampleFunctionOptions: [
        { value: "function1", label: "Function 1" },
        { value: "function2", label: "Function 2" },
        { value: "function3", label: "Function 3" },
      ],
      streamInfo: {
        name: "",
        type: "",
        targetFunction: [],
      },
      tableColumns: [
        { name: "name", label: "Name", align: "left", field: "name" },
        { name: "actions", label: "Actions", align: "center" },
      ],
      tableData: [],
      dataTypeRow: [
        { name: "", type: "", primary: false, id: 1 },
        { name: "", type: "", primary: false, id: 2 },
        { name: "", type: "", primary: false, id: 3 },
        { name: "", type: "", primary: false, id: 4 },
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
    };
  },
  mounted() {
    this.$ws.connect(() => {
      this.getFunctions();
      this.getStreamInformations();
    });
    this.$ws.addMessageHandler((data) => {
      if (data.type == "get_function") {
        this.exampleFunctionOptions = data.data.map((x, i) => ({
          id: i + 1,
          value: x.Name,
          label: x.Name,
        }));
      } else if (data.type == 'get_zstream_name') {
        console.log(data.data);
      } else if (data.type == "get_zstreams") {
        this.tableData = data.data.map((x, i) => ({
          id: i + 1,
          name: x.Name,
          streamType: "",
          streamFunction: "",
        }));
      } else if (data.type == "create_zstream" || data.type == "drop_zstream") {
        this.getStreamInformations();
      }
    });
  },
  beforeUnmount() {
    this.$ws.removeAll();
  },
  methods: {
    getFunctions() {
      this.$ws.sendMessage(`SHOW FUNCTIONS;`, "get_function");
    },
    getStreamInformations() {
      this.$ws.sendMessage(`SHOW ZSTREAMS;`, "get_zstreams");
    },
    openDeleteDialog(row) {
      this.selectedRow = row;
      this.isDeleteDialogOpen = true;
    },
    confirmDelete() {
      this.$ws.sendMessage(
        `DROP ZSTREAM ${this.selectedRow.name};`,
        "drop_zstream"
      );
      this.isDeleteDialogOpen = false;
      this.selectedRow = null;
    },
    openTableDialog() {
      this.resetStream();
      this.addNewStream = !this.addNewStream;
    },
    openEditDialog(row) {
      this.selectedRow = row;
      this.addNewStream = true;
      this.resetStream();
      this.streamInfo.name = row.name;
      this.$ws.sendMessage(`describe ${row.name};`, "get_zstream_name");
    },
    addStream() {
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

          if (field.primary) {
            columnDef += " GENERATED ALWAYS AS IDENTITY";
          }

          return columnDef;
        });

      const query = `CREATE ZSTREAM ${this.streamInfo.name} 
      (
        type ${this.streamInfo.type} GENERATED ALWAYS AS DISPATCH,
        ${columns.join(",\n    ")}
      )
      WITH (
        command_functions = {
          ${this.streamInfo.targetFunction
            .map((x) => `'${x.value}' = '${x.label}'`)
            .join(",\n")}
        },
        reply_function = '${this.streamInfo.replyFunction}'
      )`;
      this.$ws.sendMessage(query, "create_zstream");
      this.addNewStream = false;
      this.$refs.addStreamForm.reset();
    },
    resetStream() {
      this.streamInfo = {
        name: "",
        type: "",
        targetFunction: [],
        replyFunction: "",
      };
      this.dataTypeRow = [{ name: "", type: "", primary: false, id: 1 }];
      this.$nextTick(() => {
        this.$refs.dataTypeTable.rows = this.dataTypeRow;
      });
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
</style>
