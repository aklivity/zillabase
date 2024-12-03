<template>
  <div class="q-px-sm q-pb-sm q-gutter-sm">
    <q-splitter v-model="splitterModel" class="storage-tab-splitter">
      <template v-slot:before>
        <q-tabs
          v-model="selectedTab"
          vertical
          class="text-primary"
          align="left"
          animated
        >
          <q-card-section class="flex justify-between q-pa-sm q-mt-md q-mb-sm">
            <div class="flex justify-between items-center full-width">
              <div class="flex">
                <p class="text-custom-text-secondary text-h6 fw-600 text-left">
                  All Buckets
                </p>
              </div>
              <q-btn
                unelevated
                icon="add"
                :ripple="false"
                class="bg-light-green rounded-10 text-white text-capitalize self-center q-pa-sm"
                @click="addNewBucketDialog"
              />
            </div>
            <div class="row q-mt-md full-width">
              <q-input
                outlined
                dense
                :placeholder="`Search Bucket..`"
                class="rounded-10 self-center search-input text-weight-light rounded-input full-width"
              >
                <template v-slot:append>
                  <q-icon
                    name="img:/icons/search.svg"
                    class="fs-lg filter-gray-dark"
                  />
                </template>
              </q-input>
            </div>
          </q-card-section>
          <q-tab v-for="(tab, index) in tabs" :key="tab.name" :name="tab.name">
            <!-- <div class="row q-pl-sm justify-between items-center"> -->
            <!-- Tab name on the left -->
            <span
              class="text-custom-gray-dark text-capitalize text-weight-light"
              >{{ tab.name }}</span
            >
            <!-- Buttons on the right -->
            <div class="flex">
              <q-btn
                flat
                dense
                icon="img:/icons/edit.svg"
                class="filter-text-secondary"
                @click="editTab(index)"
                size="14px"
              />
              <q-btn
                flat
                dense
                icon="img:/icons/trash.svg"
                class="q-ml-md"
                @click="deleteTab(index)"
                size="14px"
              />
            </div>
            <!-- </div> -->
          </q-tab>
        </q-tabs>
      </template>

      <template v-slot:after>
        <q-tab-panels
          v-model="selectedTab"
          animated
          swipeable
          vertical
          transition-prev="jump-up"
          transition-next="jump-up"
          class="q-mt-sm"
        >
          <q-tab-panel name="initialTab">
            <div class="flex justify-between q-pb-lg">
              <div class="">
                <p class="text-custom-text-secondary text-h6 fw-600">
                  Select A Bucket To View
                </p>
              </div>
            </div>

            <div class="row justify-center q-pt-lg">
              <div
                class="column q-pa-lg bg-custom-primary text-center select-bucket"
              >
                <div class="flex flex-center q-mb-md">
                  <q-icon class="fs-60" name="img:/icons/folder-Icon.svg" />
                </div>
                <div class="fs-18 text-custom-text-secondary">
                  Select a <b>Bucket</b> to get started
                </div>
                <div class="text-caption text-custom-text-secondary q-my-sm">
                  OR
                </div>
                <q-btn
                  unelevated
                  label="Add A Bucket"
                  icon="add"
                  :ripple="false"
                  class="bg-light-green rounded-10 text-white text-capitalize self-center btn-add-new q-mt-sm"
                  @click="addNewBucketDialog"
                />
              </div>
            </div>
          </q-tab-panel>
          <q-tab-panel v-for="tab in tabs" :key="tab.name" :name="tab.name">
            <common-table
              :title="tab.name"
              description=""
              :columns="tableColumns"
              :rows="tab.tableData"
              :searchInputPlaceholder="searchLabel"
              :showAddButton="false"
              :hideBottom="false"
              noDataLabel="No Data"
              @add-new="openTableDialog"
              @move-row="openMoveDialog"
              @rename-row="openRenameDialog"
              showStorage
              isMultipleChecked
            />

            <template v-if="tab.tableData.length <= 0">
              <div class="row justify-center q-pt-lg">
                <div
                  class="column q-pa-lg bg-custom-primary text-center bucket-file-upload"
                >
                  <div class="flex flex-center q-mb-md">
                    <q-icon
                      class="fs-60"
                      name="img:/icons/folder-add-bucket.svg"
                    />
                  </div>
                  <div class="fs-18 text-custom-text-secondary">
                    Start dropping your files here
                  </div>
                  <div class="text-caption text-custom-text-secondary q-my-sm">
                    OR
                  </div>
                  <q-btn
                    unelevated
                    label="Upload A File"
                    icon="add"
                    :ripple="false"
                    class="bg-light-green rounded-10 text-white text-capitalize self-center btn-add-new q-mt-sm"
                    @click="addNewBucketDialog"
                  />
                </div>
              </div>
            </template>
          </q-tab-panel>
        </q-tab-panels>
      </template>
    </q-splitter>
  </div>

  <!-- Moving row Dialog -->
  <q-dialog
    v-model="isMovingRow"
    backdrop-filter="blur(4px)"
    class="storage-dialog"
  >
    <q-card class="highlighted-border">
      <q-card-section class="flex justify-between items-center q-pa-lg">
        <div class="flex items-center">
          <q-icon
            size="sm"
            name="img:/icons/login-02.svg"
            class="filter-custom-dark"
          />
          <p class="text-custom-text-secondary fw-600 q-ml-md text-subtitle1">
            Moving {{ selectedRow?.name }}
          </p>
        </div>
        <q-icon
          name="close"
          class="cursor-pointer fs-20"
          @click="isMovingRow = false"
        />
      </q-card-section>
      <q-separator />
      <q-card-section class="q-pb-lg">
        <p class="text-custom-gray-dark text-weight-light q-pb-sm">
          Write Directory Path
        </p>
        <q-input
          dense
          outlined
          placeholder="e.g folder1/subfodler"
          class="rounded-10 self-center text-weight-light rounded-input bg-custom-primary"
        />
        <p class="text-custom-gray-dark text-weight-light q-pt-xs">
          Leave blank to move items to the root of the bucket
        </p>
      </q-card-section>
      <q-separator />
      <q-card-actions align="right" class="q-pa-md">
        <q-btn
          label="Cancel"
          unelevated
          color="dark"
          class="rounded-10 text-capitalize min-w-80 highlighted-border"
          @click="isMovingRow = false"
        />
        <q-btn
          label="Move"
          unelevated
          color="light-green"
          class="rounded-10 text-capitalize min-w-80"
        />
      </q-card-actions>
    </q-card>
  </q-dialog>

  <!-- Rename row Dialog -->
  <q-dialog
    v-model="isRenameRow"
    backdrop-filter="blur(4px)"
    class="storage-dialog"
  >
    <q-card class="highlighted-border">
      <q-card-section class="flex justify-between items-center q-pa-lg">
        <div class="flex items-center">
          <q-icon
            size="sm"
            name="img:/icons/edit.svg"
            class="filter-custom-dark"
          />
          <p class="text-custom-text-secondary fw-600 q-ml-md text-subtitle1">
            Renaming {{ selectedRow?.name }}
          </p>
        </div>
        <q-icon
          name="close"
          class="cursor-pointer fs-20"
          @click="isRenameRow = false"
        />
      </q-card-section>
      <q-separator />
      <q-card-section class="q-pb-lg">
        <p class="text-custom-gray-dark text-weight-light q-pb-sm">
          Write New Name
        </p>
        <q-input
          dense
          outlined
          placeholder="e.g my-image"
          class="rounded-10 self-center text-weight-light rounded-input bg-custom-primary"
        />
      </q-card-section>
      <q-separator />
      <q-card-actions align="right" class="q-pa-md">
        <q-btn
          label="Cancel"
          unelevated
          color="dark"
          class="rounded-10 text-capitalize min-w-80 highlighted-border"
          @click="isRenameRow = false"
        />
        <q-btn
          label="Rename"
          unelevated
          color="light-green"
          class="rounded-10 text-capitalize min-w-80"
        />
      </q-card-actions>
    </q-card>
  </q-dialog>

  <q-dialog
    v-model="addNewBucket"
    backdrop-filter="blur(4px)"
    class="snippet-dialog"
  >
    <q-card class="highlighted-border">
      <q-card-section class="flex justify-between items-center q-pa-lg">
        <div class="flex items-center">
          <q-icon size="sm" name="add" class="filter-custom-dark" />
          <p class="text-custom-text-secondary fw-600 q-ml-md text-subtitle1">
            Add New Bucket
          </p>
        </div>
        <q-icon
          name="close"
          class="cursor-pointer fs-20"
          @click="addNewBucket = false"
        />
      </q-card-section>
      <q-separator />
      <q-card-section class="q-pb-lg">
        <p class="text-custom-gray-dark text-weight-light q-pb-sm">
          Write Bucket Name
        </p>
        <q-input
          dense
          outlined
          placeholder="e.g my-bucket"
          class="rounded-10 self-center text-weight-light rounded-input bg-custom-primary"
        />
      </q-card-section>
      <q-separator />
      <q-card-actions align="right" class="q-pa-md">
        <q-btn
          label="Cancel"
          unelevated
          color="dark"
          class="rounded-10 text-capitalize min-w-80 highlighted-border"
          @click="addNewBucket = false"
        />
        <q-btn
          label="Add Now"
          unelevated
          color="light-green"
          class="rounded-10 text-capitalize min-w-80"
        />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>
<script>
import CommonTable from "../shared/CommonTable.vue";
import { defineComponent } from "vue";
import { ref } from "vue";

export default defineComponent({
  name: "StorageComponent",
  components: {
    CommonTable,
  },
  data() {
    return {
      selectedTab: "initialTab",
      searchLabel: "Bucket",
      isMovingRow: false,
      isRenameRow: false,
      selectedRow: null,
      addNewBucket: false,
      tableColumns: [
        { name: "name", label: "Name", align: "left", field: "name" },
        {
          name: "size",
          label: "Size",
          align: "left",
          field: "size",
        },
        {
          name: "tabType",
          label: "Type",
          align: "center",
          field: "tabType",
          sortable: true,
        },
        {
          name: "createdAt",
          label: "Created At",
          align: "left",
          field: "createdAt",
          sortable: true,
        },
        {
          name: "lastUpdated",
          label: "Last Updated",
          align: "left",
          field: "lastUpdated",
          sortable: true,
        },
        { name: "tabActions", label: "Actions", align: "center" },
      ],
      tabs: [
        {
          name: "Bucket 1",
          tableData: [
            {
              id: 1,
              name: "Screenshot 2024-11-11",
              size: "145.4 KB",
              tabType: "img/png",
              createdAt: "10/30/2024, 7:51:16 PM",
              lastUpdated: "10/30/2024, 7:51:16 PM",
            },
            {
              id: 2,
              name: "New Folder",
              size: "145.4 KB",
              tabType: "img/png",
              createdAt: "10/30/2024, 7:51:16 PM",
              lastUpdated: "10/30/2024, 7:51:16 PM",
            },
            {
              id: 3,
              name: "Screenshot 2024-11-11",
              size: "145.4 KB",
              tabType: "img/png",
              createdAt: "10/30/2024, 7:51:16 PM",
              lastUpdated: "10/30/2024, 7:51:16 PM",
            },
            {
              id: 4,
              name: "Screenshot 2024-11-11",
              size: "145.4 KB",
              tabType: "img/png",
              createdAt: "10/30/2024, 7:51:16 PM",
              lastUpdated: "10/30/2024, 7:51:16 PM",
            },
            {
              id: 5,
              name: "Screenshot 2024-11-11",
              size: "145.4 KB",
              tabType: "img/png",
              createdAt: "10/30/2024, 7:51:16 PM",
              lastUpdated: "10/30/2024, 7:51:16 PM",
            },
          ],
        },
        {
          name: "Bucket 2",
          tableData: [
            {
              id: 1,
              name: "Screenshot 2024-11-11",
              size: "145.4 KB",
              tabType: "img/png",
              createdAt: "10/30/2024, 7:51:16 PM",
              lastUpdated: "10/30/2024, 7:51:16 PM",
            },
            {
              id: 2,
              name: "Screenshot 2024-11-11",
              size: "145.4 KB",
              tabType: "img/png",
              createdAt: "10/30/2024, 7:51:16 PM",
              lastUpdated: "10/30/2024, 7:51:16 PM",
            },
            {
              id: 3,
              name: "Screenshot 2024-11-11",
              size: "145.4 KB",
              tabType: "img/png",
              createdAt: "10/30/2024, 7:51:16 PM",
              lastUpdated: "10/30/2024, 7:51:16 PM",
            },
            {
              id: 4,
              name: "Screenshot 2024-11-11",
              size: "145.4 KB",
              tabType: "img/png",
              createdAt: "10/30/2024, 7:51:16 PM",
              lastUpdated: "10/30/2024, 7:51:16 PM",
            },
            {
              id: 5,
              name: "Screenshot 2024-11-11",
              size: "145.4 KB",
              tabType: "img/png",
              createdAt: "10/30/2024, 7:51:16 PM",
              lastUpdated: "10/30/2024, 7:51:16 PM",
            },
          ],
        },
        {
          name: "Bucket 3",
          tableData: [],
        },
      ],
    };
  },
  methods: {
    handleClick() {},
    openTableDialog() {
      this.addNewView = !this.addNewView;
    },
    openMoveDialog(row) {
      this.isMovingRow = !this.isMovingRow;
      this.selectedRow = row;
    },
    openRenameDialog(row) {
      this.isRenameRow = !this.isRenameRow;
      this.selectedRow = row;
    },
    editTab(index) {},
    deleteTab(index) {},
    addNewBucketDialog() {
      this.addNewBucket = !this.addNewBucket;
    },
  },
  setup() {
    return {
      splitterModel: ref(20),
    };
  },
});
</script>
<style scoped lang="scss">
.search-input {
  width: 222px;
  color: rgba(0, 0, 0, 0.57);
}

.select-bucket {
  border: 1px dashed var(--q-color-gray-dark);
  border-radius: 20px;
  width: 400px;
}

.bucket-file-upload {
  border: 2px dashed var(--q-color-gray-dark);
  border-radius: 20px;
  width: 929px;
}

.q-dialog__inner {
  .q-card {
    border-radius: 15px;
    background-color: var(--q-color-bg);
    box-shadow: none;
    width: 550px;

    .q-card__actions {
      .q-btn--rectangle {
        min-width: 80px;
      }
    }
  }
}
</style>
