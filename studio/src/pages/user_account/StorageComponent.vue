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
          <q-tab
            @click="getStorageObjects"
            v-for="tab in tabs"
            :key="tab.name.path"
            :name="tab.name.path"
          >
            <!-- Tab name on the left -->
            <span class="text-custom-gray-dark text-capitalize text-weight-light" style="text-transform: none;">
              {{ tab.name.path }}
            </span>
            <!-- Buttons on the right -->
            <div class="flex">
              <q-btn
                flat
                dense
                icon="img:/icons/edit.svg"
                class="filter-text-secondary"
                @click="(e) => editTab(e, tab?.name.path)"
                size="14px"
              />
              <q-btn
                flat
                dense
                icon="img:/icons/trash.svg"
                class="q-ml-md"
                @click="(e) => deleteBucket(e, tab?.name.path)"
                size="14px"
              />
            </div>
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
          <q-tab-panel
            v-for="tab in tabs"
            :key="tab.name.path"
            :name="tab.name.path"
          >
            <common-table
              :title="tab.name.path"
              description=""
              :columns="tableColumns"
              :rows="tab.tableData"
              :searchInputPlaceholder="searchLabel"
              :showAddButton="false"
              :hideBottom="false"
              noDataLabel="No Data"
              @delete-item="deleteBucketItems"
              @move-row="openMoveDialog"
              @rename-row="openEditFileDialog"
              @edit-row="openEditFileDialog"
              @delete-row="openBucketObjectDeleteDialog"
              @add-item="addNewBucketObjectContent = true"
              @add-file="addNewBucketObjectDialog"
              showStorage
              isMultipleChecked
            />

            <div
              class="row justify-center q-pt-lg"
              v-if="!tab.tableData.length"
            >
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
                <div class="flex justify-center">
                  <q-btn
                    unelevated
                    label="Upload A File"
                    icon="add"
                    :ripple="false"
                    class="bg-light-green rounded-10 text-white text-capitalize self-center btn-add-new q-mt-sm q-mx-sm"
                    @click="addNewBucketObjectDialog"
                  />
                  <q-btn
                    unelevated
                    label="Add A File"
                    icon="add"
                    :ripple="false"
                    color="dark"
                    class="rounded-10 text-white text-capitalize self-center btn-add-new q-mt-sm q-mx-sm"
                    @click="addNewBucketObjectContent = true"
                  />
                </div>
              </div>
            </div>
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
          Directory Path
        </p>
        <q-input
          dense
          outlined
          placeholder="folder1/subfodler"
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
          Name
        </p>
        <q-input
          dense
          outlined
          placeholder="my-image"
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
          @click="updateStorageObject"
          class="rounded-10 text-capitalize min-w-80"
        />
      </q-card-actions>
    </q-card>
  </q-dialog>

  <!-- Add new bucket Dialog -->
  <q-dialog
    v-model="addEditBucketDialog.isOpen"
    backdrop-filter="blur(4px)"
    class="snippet-dialog"
  >
    <q-card class="highlighted-border">
      <q-card-section class="flex justify-between items-center q-pa-lg">
        <div class="flex items-center">
          <q-icon size="sm" name="add" class="filter-custom-dark" />
          <p class="text-custom-text-secondary fw-600 q-ml-md text-subtitle1">
            {{ addEditBucketDialog.bucketName ? "Edit" : "Add New" }} Bucket
          </p>
        </div>
        <q-icon
          name="close"
          class="cursor-pointer fs-20"
          @click="closeAddEditBucketDialog"
        />
      </q-card-section>
      <q-separator />
      <q-card-section class="q-pb-lg">
        <p class="text-custom-gray-dark text-weight-light q-pb-sm">
          Bucket Name
        </p>
        <q-input
          dense
          outlined
          v-model="newBucketName"
          placeholder="my-bucket"
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
          @click="closeAddEditBucketDialog"
        />
        <q-btn
          label="Add Now"
          unelevated
          color="light-green"
          @click="addStorageBuckets"
          class="rounded-10 text-capitalize min-w-80"
        />
      </q-card-actions>
    </q-card>
  </q-dialog>

  <!-- Add new bucket object Dialog -->
  <q-dialog
    v-model="addNewBucketObject"
    backdrop-filter="blur(4px)"
    class="snippet-dialog"
  >
    <q-card class="highlighted-border">
      <q-card-section class="flex justify-between items-center q-pa-lg">
        <div class="flex items-center">
          <q-icon size="sm" name="add" class="filter-custom-dark" />
          <p class="text-custom-text-secondary fw-600 q-ml-md text-subtitle1">
            Add New {{ selectedTab }}
          </p>
        </div>
        <q-icon
          name="close"
          class="cursor-pointer fs-20"
          @click="addNewBucketObject = false"
        />
      </q-card-section>
      <q-separator />
      <q-card-section class="cursor-pointer" @click="openFilePicker">
        <div class="flex flex-center q-mb-md">
          <q-icon class="fs-60" name="img:/icons/folder-add-bucket.svg" />
        </div>
        <div class="fs-18 text-custom-text-secondary text-center">
          {{ selectedFile ? selectedFile?.name : "Select a file" }}
        </div>
      </q-card-section>
      <input
        type="file"
        ref="fileInput"
        style="display: none"
        @change="handleFileSelect"
      />
      <q-separator />
      <q-card-actions align="right" class="q-pa-md">
        <q-btn
          label="Cancel"
          unelevated
          color="dark"
          class="rounded-10 text-capitalize min-w-80 highlighted-border"
          @click="addNewBucketObject = false"
        />
        <q-btn
          label="Add Now"
          unelevated
          color="light-green"
          class="rounded-10 text-capitalize min-w-80"
          :disable="!selectedFile"
          @click="addStorageObject"
        />
      </q-card-actions>
    </q-card>
  </q-dialog>

  <q-dialog
    v-model="addNewBucketObjectContent"
    backdrop-filter="blur(4px)"
    class="snippet-dialog"
  >
    <q-card class="highlighted-border">
      <q-card-section class="flex justify-between items-center q-pa-lg">
        <div class="flex items-center">
          <q-icon size="sm" name="add" class="filter-custom-dark" />
          <p class="text-custom-text-secondary fw-600 q-ml-md text-subtitle1">
            {{ etag ? "Edit " : "Add New " }} {{ selectedTab }}
          </p>
        </div>
        <q-icon
          name="close"
          class="cursor-pointer fs-20"
          @click="addNewBucketObjectContent = false"
        />
      </q-card-section>
      <q-separator />
      <q-card-section class="q-pb-lg">
        <p class="text-custom-gray-dark text-weight-light q-pb-sm">File Name</p>
        <q-input
          dense
          outlined
          v-model="fileName"
          :disable="etag ? true : false"
          placeholder="file-name"
          class="rounded-10 self-center text-weight-light rounded-input bg-custom-primary"
        />

        <p class="text-custom-gray-dark text-weight-light q-pb-sm q-mt-md">
          File Content
        </p>
        <q-input
          dense
          outlined
          type="textarea"
          rows="5"
          v-model="fileContent"
          placeholder="file-content"
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
          @click="addNewBucketObjectContent = false"
        />
        <q-btn
          :label="etag ? 'Update Now' : 'Add Now'"
          unelevated
          color="light-green"
          class="rounded-10 text-capitalize min-w-80"
          :disable="!fileName || !fileContent"
          @click="
            etag ? updateStorageObjectContent() : addStorageObjectContent()
          "
        />
      </q-card-actions>
    </q-card>
  </q-dialog>

  <!-- Delete Bucket Dialog -->
  <q-dialog
    v-model="deletedBucket.isDeleted"
    backdrop-filter="blur(4px)"
    class="delete-dialog"
  >
    <q-card class="highlighted-border">
      <q-card-section class="flex justify-between items-center q-pa-lg">
        <div class="flex items-center">
          <q-icon size="sm" name="img:/icons/trash.svg" />
          <p class="text-custom-text-secondary fw-600 q-ml-md text-subtitle1">
            Delete {{ deletedBucket.bucketName }}?
          </p>
        </div>
        <q-icon
          name="close"
          class="cursor-pointer fs-20"
          @click="deletedBucket.isDeleted = false"
        />
      </q-card-section>
      <q-separator />
      <q-card-section>
        <p class="text-custom-gray-dark text-weight-light q-pa-sm w-90">
          Are you sure you want to delete this
          <span class="fw-600">{{ deletedBucket.bucketName }}</span
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
          @click="deletedBucket.isDeleted = false"
        />
        <q-btn
          label="Delete"
          unelevated
          color="negative"
          class="rounded-10 text-capitalize min-w-80"
          @click="deleteStorageBuckets"
        />
      </q-card-actions>
    </q-card>
  </q-dialog>

  <!-- Delete Bucket object Dialog -->
  <q-dialog
    v-model="isOpenBucketObjectDeleteDialog.isDeleted"
    backdrop-filter="blur(4px)"
    class="delete-dialog"
  >
    <q-card class="highlighted-border">
      <q-card-section class="flex justify-between items-center q-pa-lg">
        <div class="flex items-center">
          <q-icon size="sm" name="img:/icons/trash.svg" />
          <p class="text-custom-text-secondary fw-600 q-ml-md text-subtitle1">
            Delete {{ isOpenBucketObjectDeleteDialog.selectedRow?.name.path }}?
          </p>
        </div>
        <q-icon
          name="close"
          class="cursor-pointer fs-20"
          @click="isOpenBucketObjectDeleteDialog.isDeleted = false"
        />
      </q-card-section>
      <q-separator />
      <q-card-section>
        <p class="text-custom-gray-dark text-weight-light q-pa-sm w-90">
          Are you sure you want to delete this
          <span class="fw-600">{{
            isOpenBucketObjectDeleteDialog.selectedRow?.name.path
          }}</span
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
          @click="isOpenBucketObjectDeleteDialog.isDeleted = false"
        />
        <q-btn
          label="Delete"
          unelevated
          color="negative"
          class="rounded-10 text-capitalize min-w-80"
          @click="deleteStorageObject"
        />
      </q-card-actions>
    </q-card>
  </q-dialog>

  <!-- Delete Bucket multiple object Dialog -->
  <q-dialog
    v-model="deleteMultipleSelectedRows.isDeleted"
    backdrop-filter="blur(4px)"
    class="delete-dialog"
  >
    <q-card class="highlighted-border">
      <q-card-section class="flex justify-between items-center q-pa-lg">
        <div class="flex items-center">
          <q-icon size="sm" name="img:/icons/trash.svg" />
          <p class="text-custom-text-secondary fw-600 q-ml-md text-subtitle1">
            Delete Objects?
          </p>
        </div>
        <q-icon
          name="close"
          class="cursor-pointer fs-20"
          @click="deleteMultipleSelectedRows.isDeleted = false"
        />
      </q-card-section>
      <q-separator />
      <q-card-section>
        <p class="text-custom-gray-dark text-weight-light q-pa-sm w-90">
          Are you sure you want to delete this
          <span class="fw-600">{{
            deleteMultipleSelectedRows.selectedRows
              ?.map((row) => row?.name)
              .join(", ")
          }}</span
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
          @click="deleteMultipleSelectedRows.isDeleted = false"
        />
        <q-btn
          label="Delete"
          unelevated
          color="negative"
          class="rounded-10 text-capitalize min-w-80"
          @click="deleteStorageObjects"
        />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>
<script>
import CommonTable from "../shared/CommonTable.vue";
import { defineComponent } from "vue";
import { ref } from "vue";
import {
  appAddStorageBuckets,
  appAddStorageObject,
  appAddStorageObjectContent,
  appDeleteStorageBuckets,
  appDeleteStorageObject,
  appGetStorageBuckets,
  appGetStorageObjectDetail,
  appGetStorageObjects,
  appUpdateStorageObject,
  appUpdateStorageObjectContent,
} from "src/services/api";
import {showError, showSuccess} from "src/services/notification";
import app from "src/services/app";

export default defineComponent({
  name: "StorageComponent",
  components: {
    CommonTable,
  },
  data() {
    return {
      fileName: "",
      fileContent: "",
      fileUrl: "",
      addNewBucketObjectContent: false,
      newObjectBucketName: "",
      newBucketName: "",
      selectedTab: "initialTab",
      searchLabel: "Bucket",
      selectedFile: null,
      isMovingRow: false,
      isRenameRow: false,
      selectedRow: null,
      addNewBucketObject: false,
      addEditBucketDialog: {
        isOpen: false,
        bucketName: "",
      },
      deletedBucket: {
        isDeleted: false,
        bucketName: "",
      },
      isOpenBucketObjectDeleteDialog: {
        isDeleted: false,
        selectedRow: null,
      },
      deleteMultipleSelectedRows: {
        isDeleted: false,
        selectedRows: [],
      },
      tableColumns: [
        { name: "name", label: "Name", align: "left", field: "name" },
        {
          name: "url",
          label: "URL",
          align: "left",
          field: "url",
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
        { name: "tabActions", label: "Actions", align: "center" },
      ],
      etag: null,
      tabs: []
    };
  },
  mounted() {
    this.getStorageBuckets();
  },
  methods: {
    copyToClipboard() {
      if (this.fileUrl) {
        navigator.clipboard.writeText(this.fileUrl);
        showSuccess("Copied!");
      }
    },
    getStorageBuckets() {
      appGetStorageBuckets().then(({ data }) => {
        this.tabs = data.map((x) => ({
          name: x,
          tableData: [],
        }));
      });
    },
    getStorageObjects() {
      this.addNewBucketObjectContent = false;
      this.addNewBucketObject = false;
      appGetStorageObjects(this.selectedTab).then(({ data }) => {
        const tabs = this.tabs.find((x) => x.name?.path === this.selectedTab);
        if (tabs) {
          tabs.tableData = data.map((x, i) => ({
            name: decodeURIComponent(x.path),
            url: `${app.apiEndpoint}/storage/objects/${this.selectedTab}/${x.path}`,
            id: i + 1,
            tabType: x.type,
          }));
        }
      });
    },
    deleteStorageObjects() {
      this.deleteMultipleSelectedRows.isDeleted = false;
      const names = this.deleteMultipleSelectedRows.selectedRows.map(row => row.name);
      Promise.all(
        names.map(name =>
          appDeleteStorageObject(this.selectedTab, name)
        )
      ).then(() => {
        this.getStorageObjects();
      });
    },
    deleteStorageObject() {
      this.isOpenBucketObjectDeleteDialog.isDeleted = false;
      appDeleteStorageObject(
        this.selectedTab,
        this.isOpenBucketObjectDeleteDialog.selectedRow?.name
      ).then(({ data }) => {
        this.getStorageObjects();
      });
    },
    updateStorageObject() {
      this.isRenameRow = false;
      appUpdateStorageObject(this.selectedTab, this.selectedRow?.name).then(
        ({ data }) => {
          this.getStorageObjects();
        }
      );
    },
    addStorageObject() {
      this.addNewBucketObject = false;
      appAddStorageObject(
        this.selectedTab,
        this.selectedFile
      ).then(({ data }) => {
        this.getStorageObjects();
      });
    },
    addStorageObjectContent() {
      appAddStorageObjectContent(
        this.selectedTab,
        this.fileName,
        this.fileContent
      ).then(({ data }) => {
        this.getStorageObjects();
      });
    },
    openEditFileDialog(row) {
      this.fileName = row.name;
      this.getStorageObjectDetail();
    },
    getStorageObjectDetail() {
      appGetStorageObjectDetail(this.selectedTab, this.fileName).then(
        (response) => {
          this.etag = response.headers["etag"];
          this.fileContent = response.data;
          this.addNewBucketObjectContent = true;
        }
      );
    },
    updateStorageObjectContent() {
      appUpdateStorageObjectContent(
        this.selectedTab,
        this.fileName,
        this.fileContent,
        this.etag
      ).then((response) => {
        this.addNewBucketObjectContent = false;
        this.fileName = "";
        this.fileContent = "";
        this.etag = null;
        this.getStorageObjects();
      });
    },
    addStorageBuckets() {
      this.addEditBucketDialog.isOpen = false;
      appAddStorageBuckets(this.newBucketName).then(({ data }) => {
        this.getStorageBuckets();
      });
    },
    deleteStorageBuckets() {
      this.deletedBucket.isDeleted = false;
      appDeleteStorageBuckets(this.deletedBucket.bucketName).then(
        ({ data }) => {
          this.getStorageBuckets();
        }
      ).catch(error => {
        const message = error?.status === 409 ? "Bucket is not empty" : "Something went wrong";
        showError(message);
      });
    },
    handleClick() {},
    openMoveDialog(row) {
      this.isMovingRow = !this.isMovingRow;
      this.selectedRow = row;
    },
    openRenameDialog(row) {
      this.isRenameRow = !this.isRenameRow;
      this.selectedRow = row;
    },
    deleteBucketItems(selectedRows) {
      this.deleteMultipleSelectedRows.isDeleted = true;
      this.deleteMultipleSelectedRows.selectedRows = selectedRows;
    },
    editTab(e, bucketName) {
      e.stopPropagation();
      this.addEditBucketDialog.isOpen = true;
      this.addEditBucketDialog.bucketName = bucketName;
      this.newBucketName = bucketName;
    },
    deleteBucket(e, bucketName) {
      e.stopPropagation();
      this.deletedBucket.isDeleted = true;
      this.deletedBucket.bucketName = bucketName;
    },
    openBucketObjectDeleteDialog(row) {
      this.isOpenBucketObjectDeleteDialog.isDeleted = true;
      this.isOpenBucketObjectDeleteDialog.selectedRow = row;
    },
    addNewBucketDialog() {
      this.addEditBucketDialog.isOpen = true;
      this.addEditBucketDialog.bucketName = "";
    },
    addNewBucketObjectDialog() {
      this.addNewBucketObject = !this.addNewBucketObject;
    },
    closeAddEditBucketDialog() {
      this.addEditBucketDialog.isOpen = false;
      this.addEditBucketDialog.bucketName = "";
      this.newBucketName = "";
    },
    openFilePicker() {
      this.$refs.fileInput.click();
    },
    handleFileSelect(event) {
      const file = event.target.files[0];
      if (file) {
        this.selectedFile = file;
      }
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
