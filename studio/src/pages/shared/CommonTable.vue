<template>
  <div>
    <div class="flex justify-between items-center q-pb-lg">
      <div class="">
        <p class="text-custom-text-secondary text-h6 fw-600">{{ title }}</p>
        <p class="text-custom-gray-dark text-subtitle1 text-weight-light">
          {{ description }}
        </p>
      </div>
      <div class="flex q-gutter-md">
        <q-btn
          outline
          unelevated
          label="View All API Keys"
          :ripple="false"
          class="rounded-10 text-capitalize self-center btn-add-new"
          @click="handleClick"
          v-if="additionButton"
          color="secondary"
        />
        <q-btn
          unelevated
          :label="buttonLabel"
          icon="add"
          :ripple="false"
          class="bg-light-green rounded-10 text-white text-capitalize self-center btn-add-new"
          @click="handleClick"
          v-if="showAddButton"
        />

        <q-btn
          unelevated
          icon="img:icons/trash-white.svg"
          :ripple="false"
          v-if="showStorage && selectedRows.length > 0"
          color="negative"
          class="rounded-10 text-white text-capitalize self-center btn-add-new"
          @click="handleDelete"
        />

        <q-btn
          unelevated
          icon="img:/icons/login.svg"
          :ripple="false"
          v-if="showStorage && selectedRows.length > 0"
          color="dark"
          class="rounded-10 text-white text-capitalize self-center btn-add-new highlighted-border"
          @click="handleClick"
        />

        <q-btn
          unelevated
          icon="img:/icons/export-bucket.svg"
          :ripple="false"
          v-if="showStorage && selectedRows.length > 0"
          color="light-green"
          class="rounded-10 text-white text-capitalize self-center btn-add-new"
          @click="handleClick"
        />

        <q-btn
          unelevated
          icon="add"
          :ripple="false"
          v-if="showStorage"
          color="light-green"
          class="rounded-10 text-white text-capitalize self-center btn-add-new highlighted-border"
          @click="$emit('add-file')"
        />

        <q-btn
          unelevated
          icon="img:/icons/folder-add.svg"
          :ripple="false"
          v-if="showStorage"
          color="dark"
          class="rounded-10 text-white text-capitalize self-center btn-add-new highlighted-border"
          @click="handleAdd"
        />

        <q-input
          outlined
          v-model="searchQuery"
          dense
          :placeholder="`Search ${searchInputPlaceholder}..`"
          class="rounded-10 self-center search-input text-weight-light rounded-input"
          v-if="showSearch"
        >
          <template v-slot:append>
            <q-icon
              name="img:/icons/search.svg"
              class="fs-lg filter-gray-dark"
            />
          </template>
        </q-input>
      </div>
    </div>

    <q-table
      :rows="paginatedRows"
      :columns="columns"
      row-key="id"
      flat
      bordered
      :rows-per-page-options="showPagination ? [5, 10, 20] : [0]"
      :no-data-label="noDataLabel"
      :selection="isMultipleChecked ? 'multiple' : 'none'"
      v-model:selected="selectedRows"
      class="rounded-15 data-table"
    >
      <template v-slot:header-selection="scope">
        <q-checkbox v-model="scope.selected" dense color="light-green" />
      </template>

      <template v-slot:body-selection="scope">
        <q-checkbox
          :model-value="scope.selected"
          dense
          color="light-green"
          @update:model-value="
            (val, evt) => {
              Object.getOwnPropertyDescriptor(scope, 'selected').set(val, evt);
            }
          "
        />
      </template>
      <template v-slot:body-cell-url="props">
        <q-td :props="props">
          <q-icon
            size="sm"
            class="cursor-pointer"
            @click="copyToClipboard(props.row.url)"
            :name="'content_copy'"
          />
          {{ props.row.url }}
        </q-td>
      </template>
      <template v-slot:header-cell-ztable="props">
        <q-th :props="props">
          {{ props.col.label }}
          <q-icon
            name="img:icons/question-circle.svg"
            class="fs-lg filter-gray-dark q-ml-xs"
          />
          <q-tooltip anchor="bottom middle" self="top middle">
            Lorem ipsum dolor sit amet, consectetur adipiscing elit.
          </q-tooltip>
        </q-th>
      </template>
      <template v-slot:body-cell-ztable="props">
        <q-td :props="props">
          <q-icon
            size="sm"
            :name="props.row.ztable ? 'check_circle' : 'cancel'"
            :color="props.row.ztable ? '' : 'negative'"
            :class="props.row.ztable ? 'text-default-light-green' : ''"
          />
        </q-td>
      </template>

      <template v-slot:header-cell-zview="props">
        <q-th :props="props">
          {{ props.col.label }}
          <q-icon
            name="img:icons/question-circle.svg"
            class="fs-lg filter-gray-dark q-ml-xs"
          />
          <q-tooltip anchor="bottom middle" self="top middle">
            Lorem ipsum dolor sit amet, consectetur adipiscing elit.
          </q-tooltip>
        </q-th>
      </template>
      <template v-slot:body-cell-zview="props">
        <q-td :props="props">
          <q-icon
            size="sm"
            :name="props.row.zview ? 'check_circle' : 'cancel'"
            :color="props.row.zview ? '' : 'negative'"
            :class="props.row.zview ? 'text-default-light-green' : ''"
          />
        </q-td>
      </template>

      <template v-slot:header-cell-materialized="props">
        <q-th :props="props">
          {{ props.col.label }}
          <q-icon
            name="img:icons/question-circle.svg"
            class="fs-lg filter-gray-dark q-ml-xs"
          />
          <q-tooltip anchor="bottom middle" self="top middle">
            Lorem ipsum dolor sit amet, consectetur adipiscing elit.
          </q-tooltip>
        </q-th>
      </template>

      <template v-slot:body-cell-materialized="props">
        <q-td :props="props">
          <q-icon
            size="sm"
            :name="props.row.materialized ? 'check_circle' : 'cancel'"
            :color="props.row.materialized ? '' : 'negative'"
            :class="props.row.materialized ? 'text-default-light-green' : ''"
          />
        </q-td>
      </template>

      <template v-slot:header-cell-type="props" v-if="showLabelBottom">
        <q-th :props="props">
          {{ props.col.label }}
          <q-icon
            name="img:icons/question-circle.svg"
            class="fs-lg filter-gray-dark"
          />
          <q-tooltip anchor="bottom middle" self="top middle">
            Lorem ipsum dolor sit amet, consectetur adipiscing elit.
          </q-tooltip>
        </q-th>
      </template>
      <template v-slot:body-cell-type="props">
        <q-td :props="props" :align="columns.align">
          <p
            class="function-type-cell inline-block text-white"
            :class="{
              'bg-light-green':
                props.row.type === 'External' || props.row.type === 'Active',
              'bg-custom-dark':
                props.row.type === 'Embedded' ||
                props.row.zfunction === true ||
                props.row.type === 'Real-time Synced',
            }"
          >
            {{ props.row.type }}
          </p>
        </q-td>
      </template>
      <template v-slot:body-cell-bodyOrExternalName="props">
        <q-td :props="props">
          <q-icon
            size="sm"
            name="img:/icons/eye.svg"
            class="icon-outline text-default-light-green"
          />
        </q-td>
      </template>

      <template v-slot:body-cell-enabled="props">
        <q-td :props="props">
          <q-icon
            size="sm"
            :name="props.row.enabled ? 'check_circle' : 'cancel'"
            :color="props.row.enabled ? '' : 'negative'"
            :class="props.row.enabled ? 'text-default-light-green' : ''"
          />
        </q-td>
      </template>

      <template v-slot:body-cell-actions="props">
        <q-td :props="props">
          <q-btn
            v-if="tableName === 'function-table'"
            flat
            dense
            icon="img:/icons/eye.svg"
            class="icon-outline text-default-light-green q-mr-md"
            @click="viewRow(props.row)"
          />
          <q-btn
            v-if="isShowEdit"
            flat
            dense
            icon="img:/icons/edit.svg"
            class="filter-text-secondary"
            @click="editRow(props.row)"
          />
          <q-btn
            flat
            dense
            icon="img:/icons/trash.svg"
            class="q-ml-md"
            @click="deleteRow(props.row)"
          />
        </q-td>
      </template>
      <template v-slot:body-cell-tabActions="props">
        <q-td :props="props">
          <!-- <q-btn
            flat
            dense
            round
            icon="img:/icons/more.svg"
            ref="menuButton"
            class="filter-text-secondary"
          >
            <q-menu class="zillabase-menu">
              <q-list style="min-width: 150px">
                <q-item clickable v-close-popup @click="onMoveRow(props.row)">
                  <q-item-section>
                    <div class="flex">
                      <q-icon
                        name="img:/icons/more-menu-move.svg"
                        size="sm"
                        class="q-pr-md filter-gray-dark"
                      />
                      <span>Move</span>
                    </div>
                  </q-item-section>
                </q-item>
                <q-separator />
                <q-item clickable v-close-popup>
                  <q-item-section>
                    <div class="flex">
                      <q-icon
                        name="img:/icons/more-menu-copy.svg"
                        size="sm"
                        class="q-pr-md filter-gray-dark"
                      />
                      <span>Copy URL</span>
                    </div>
                  </q-item-section>
                </q-item>
                <q-separator />
                <q-item clickable v-close-popup @click="onRenameRow(props.row)">
                  <q-item-section>
                    <div class="flex">
                      <q-icon
                        name="img:/icons/more-menu-rename.svg"
                        size="sm"
                        class="q-pr-md filter-gray-dark"
                      />
                      <span>Edit</span>
                    </div>
                  </q-item-section>
                </q-item>
                <q-separator />
                <q-item clickable v-close-popup>
                  <q-item-section>
                    <div class="flex">
                      <q-icon
                        name="img:/icons/more-menu-download.svg"
                        size="sm"
                        class="q-pr-md filter-gray-dark"
                      />
                      <span>Download</span>
                    </div>
                  </q-item-section>
                </q-item>
              </q-list>
            </q-menu>
          </q-btn> -->
          <q-btn
            flat
            dense
            icon="img:/icons/edit.svg"
            class="filter-text-secondary"
            @click="editRow(props.row)"
          />
          <q-btn
            flat
            dense
            icon="img:/icons/trash.svg"
            class="q-ml-sm"
            @click="deleteRow(props.row)"
          />
        </q-td>
      </template>
      <!-- Custom Bottom Slot for Pagination -->
      <template v-slot:pagination>
        <q-btn
          icon="img:/icons/arrow-circle-right.svg"
          color="grey-8"
          round
          dense
          flat
          :disable="pagination.page === 1"
          @click="prevPage"
        />
        <!-- Page Numbers -->
        <div class="q-px-sm row">
          <div
            v-for="page in totalPages"
            :key="page"
            class="q-px-md cursor-pointer"
            :class="
              page === pagination.page
                ? 'text-custom-text-secondary'
                : 'text-custom-gray-dark'
            "
            @click="goToPage(page)"
          >
            {{ page }}
          </div>
        </div>
        <q-btn
          icon="img:/icons/arrow-circle-left.svg"
          color="grey-8"
          round
          dense
          flat
          :disable="pagination.page === totalPages"
          @click="nextPage"
        />
      </template>
      <template v-slot:no-data="{ message }">
        <p style="color: #868686">{{ message }}</p>
      </template>
    </q-table>
  </div>
</template>
<script>
import { showSuccess } from "src/services/notification";
import { defineComponent } from "vue";
export default defineComponent({
  name: "CommonTable",
  props: {
    title: {
      type: String,
      default: "Example Table",
    },
    description: {
      type: String,
      default: "Lorem ipsum dolor sit amet, consectetur adipiscing elit.",
    },
    columns: {
      type: Array,
      required: true,
    },
    rows: {
      type: Array,
      required: true,
    },
    buttonLabel: {
      type: String,
      default: "",
    },
    searchInputPlaceholder: {
      type: String,
      default: "Data",
    },
    showAddButton: {
      type: Boolean,
      default: true,
    },
    showPagination: {
      type: Boolean,
      default: false,
    },
    showStorage: {
      type: Boolean,
      default: false,
    },
    isMultipleChecked: {
      type: Boolean,
      default: false,
    },
    showNoData: {
      type: Boolean,
      default: false,
    },
    hideBottom: {
      type: Boolean,
      default: true,
    },
    noDataLabel: {
      type: String,
      default: "No data available",
    },
    showSearch: {
      type: Boolean,
      default: true,
    },
    additionButton: {
      type: Boolean,
      default: false,
    },
    showLabelBottom: {
      type: Boolean,
      default: true,
    },
    tableName: {
      type: String,
      default: "",
    },
    isShowEdit: {
      type: Boolean,
      default: true,
    },
  },
  data() {
    return {
      searchQuery: "",
      menuTarget: null,
      selectedRows: [],
      pagination: {
        page: 1,
        rowsPerPage: 5,
      },
    };
  },
  computed: {
    filteredRows() {
      if (!this.searchQuery) return this.rows;
      const query = this.searchQuery.toLowerCase();
      return this.rows.filter((row) =>
        Object.values(row).some((value) =>
          String(value).toLowerCase().includes(query)
        )
      );
    },
    paginatedRows() {
      // Only apply pagination when `showPagination` is true
      if (!this.showPagination) return this.filteredRows;

      const start = (this.pagination.page - 1) * this.pagination.rowsPerPage;
      const end = start + this.pagination.rowsPerPage;
      return this.filteredRows.slice(start, end);
    },
    totalPages() {
      const rowsLength = this.filteredRows.length; // Adjust for filtered rows if using search
      return Math.ceil(rowsLength / this.pagination.rowsPerPage);
    },
  },
  methods: {
    copyToClipboard(url) {
      if (url) {
        navigator.clipboard.writeText(url);
        showSuccess("Copied!");
      }
    },
    viewRow(row) {
      this.$emit("view-row", row);
    },
    editRow(row) {
      this.$emit("edit-row", row);
    },
    deleteRow(row) {
      this.$emit("delete-row", row);
    },
    handleClick() {
      this.$emit("add-new");
    },
    handleDelete() {
      this.$emit("delete-item", this.selectedRows);
    },
    handleAdd() {
      this.$emit("add-item");
    },
    goToPage(page) {
      if (page >= 1 && page <= this.totalPages) {
        this.pagination.page = page;
        console.log("Navigated to page:", page);
      }
    },
    prevPage() {
      if (this.pagination.page > 1) {
        this.pagination.page--;
      }
    },
    nextPage() {
      if (this.pagination.page < this.totalPages) {
        this.pagination.page++;
      }
    },
    onMoveRow(row) {
      this.$emit("move-row", row);
    },
    onRenameRow(row) {
      this.$emit("rename-row", row);
    },
  },
  mounted() {
    // Set the target for menu positioning
    if (this.$refs.menuButton) {
      this.menuTarget = this.$refs.menuButton.$el;
    }
  },
});
</script>
<style scoped lang="scss">
.function-type-cell {
  border-radius: 6px;
  border: 1px;
  padding: 6px 12px;
}

.q-btn.btn-add-new {
  padding: 4px 10px;
}

.q-item__section--main {
  opacity: 0.5;
}
</style>
