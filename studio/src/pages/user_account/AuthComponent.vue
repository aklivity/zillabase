<template>
  <div class="q-px-lg">
    <div class="row no-wrap">
      <div class="col-6 q-pr-md q-pt-md">
        <common-table
          title="Users"
          description=""
          :columns="userTableColumns"
          :rows="userTableData"
          buttonLabel=""
          searchInputPlaceholder="Users"
          @add-new="openUserDialog"
          showPagination
          :hideBottom="false"
          @edit-row="getUserById"
          @delete-row="openDeleteDialog"
        />
      </div>
      <q-separator vertical />
      <div class="col-6 q-pl-lg q-pt-md">
        <common-table
          title="SSO Providers"
          description=""
          :columns="ssoTableColumns"
          :rows="ssoTableData"
          buttonLabel=""
          searchInputPlaceholder="Providers"
          @add-new="openProviderDialog"
          showPagination
          :hideBottom="false"
          @edit-row="getSSOProviderById"
          @delete-row="openSSOProviderDeleteDialog"
        />
      </div>
    </div>
  </div>
  <!-- add user Dialog -->
  <q-dialog
    v-model="addNewUser"
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
            :icon="addNewUser ? 'chevron_left' : 'chevron_right'"
            style="width: 30px; min-height: 30px"
            @click="closeUserDialog"
            class="rounded-10"
          />
          <p class="text-custom-text-secondary text-h6 fw-600">
            Create {{ hasUserInfoValues ? "Edit" : "New" }} User
          </p>
        </div>
        <q-icon
          name="img:/icons/auth.svg"
          class="fs-30 filter-custom-dark"
          style="min-height: 30px"
        />
      </q-card-section>
      <q-separator />
      <q-form @submit="addUser" @reset="resetUser" ref="addUserForm">
        <q-card-section class="q-py-xl px-28">
          <div class="row items-start q-mt-sm q-pt-md">
            <div class="col-3">
              <span
                class="text-custom-gray-dark text-subtitle1 text-weight-light"
                >First Name</span
              >
            </div>
            <div class="col-9">
              <q-input
                dense
                v-model="userInfo.firstName"
                outlined
                placeholder="First Name"
                class="rounded-10 self-center text-weight-light rounded-input"
                :rules="[(val) => !!val || 'Field is required']"
              />
            </div>
          </div>
          <div class="row items-start q-mt-sm q-pt-md">
            <div class="col-3">
              <span
                class="text-custom-gray-dark text-subtitle1 text-weight-light"
                >Last Name</span
              >
            </div>
            <div class="col-9">
              <q-input
                dense
                v-model="userInfo.lastName"
                outlined
                placeholder="Last Name"
                class="rounded-10 self-center text-weight-light rounded-input"
                :rules="[(val) => !!val || 'Field is required']"
              />
            </div>
          </div>
          <div class="row items-start q-mt-sm q-pt-md">
            <div class="col-3">
              <span
                class="text-custom-gray-dark text-subtitle1 text-weight-light"
                >Username</span
              >
            </div>
            <div class="col-9">
              <q-input
                dense
                v-model="userInfo.username"
                outlined
                placeholder="User Name"
                class="rounded-10 self-center text-weight-light rounded-input"
                :rules="[(val) => !!val || 'Field is required']"
              />
            </div>
          </div>
          <div class="row items-start q-mt-sm q-pt-md">
            <div class="col-3">
              <span
                class="text-custom-gray-dark text-subtitle1 text-weight-light"
                >Email</span
              >
            </div>
            <div class="col-9">
              <q-input
                dense
                outlined
                v-model="userInfo.email"
                placeholder="User Email"
                type="email"
                class="rounded-10 self-center text-weight-light rounded-input"
                :rules="[(val) => !!val || 'Field is required']"
              />
            </div>
          </div>
          <div class="row items-start q-mt-sm q-pt-md">
            <div class="col-3">
              <span
                class="text-custom-gray-dark text-subtitle1 text-weight-light"
                >Password</span
              >
            </div>
            <div class="col-9">
              <q-input
                dense
                outlined
                v-model="userInfo.password"
                placeholder="Password"
                type="password"
                class="rounded-10 self-center text-weight-light rounded-input"
                :rules="[(val) => !!val || 'Field is required']"
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
            @click="closeUserDialog"
            class="text-capitalize rounded-10 highlighted-border"
          />
          <q-btn
            unelevated
            label="Add User"
            icon="add"
            :ripple="false"
            type="submit"
            class="bg-light-green rounded-10 text-white text-capitalize self-center"
          />
        </q-card-section>
      </q-form>
    </q-card>
  </q-dialog>
  <!-- add provider Dialog -->
  <q-dialog
    v-model="addNewProvider"
    persistent
    full-height
    maximized
    position="right"
    backdrop-filter="blur(4px)"
    class="add-new-dialog"
  >
    <q-card class="full-height" style="width: 871px; max-width: 80vw">
      <q-card-section class="flex justify-between items-center q-pa-lg">
        <div class="flex q-gutter-x-lg">
          <q-btn
            unelevated
            color="light-green"
            :icon="addNewProvider ? 'chevron_left' : 'chevron_right'"
            style="width: 30px; min-height: 30px"
            @click="closeProviderDialog"
            class="rounded-10"
          />
          <p class="text-custom-text-secondary text-h6 fw-600">
            {{ hasProviderInfoValues ? "Edit" : "Add" }} A Provider
          </p>
        </div>
        <q-icon
          name="img:/icons/auth.svg"
          class="fs-30 filter-custom-dark"
          style="min-height: 30px"
        />
      </q-card-section>
      <q-separator />
      <q-form
        @submit="addSSOProvider"
        @reset="resetSSOProvider"
        ref="addSSOProviderForm"
      >
        <q-card-section class="q-py-xl px-28">
          <div class="row items-start">
            <div class="col-3">
              <span
                class="text-custom-gray-dark text-subtitle1 text-weight-light"
                >Provider</span
              >
            </div>
            <div class="col-9">
              <q-input
                dense
                outlined
                placeholder="e.g Google"
                v-model="providerInfo.providerId"
                class="rounded-10 self-center text-weight-light rounded-input"
                :rules="[(val) => !!val || 'Field is required']"
              />
            </div>
          </div>
          <div class="row items-start q-mt-sm q-pt-md">
            <div class="col-3">
              <span
                class="text-custom-gray-dark text-subtitle1 text-weight-light"
                >Alias</span
              >
            </div>
            <div class="col-9">
              <q-input
                dense
                outlined
                v-model="providerInfo.alias"
                class="rounded-10 self-center text-weight-light rounded-input"
                :rules="[(val) => !!val || 'Field is required']"
              />
            </div>
          </div>
          <div class="row items-start q-mt-sm q-pt-md">
            <div class="col-3">
              <span
                class="text-custom-gray-dark text-subtitle1 text-weight-light"
                >Client</span
              >
            </div>
            <div class="col-9">
              <q-input
                dense
                outlined
                v-model="providerInfo.clientId"
                class="rounded-10 self-center text-weight-light rounded-input"
                :rules="[(val) => !!val || 'Field is required']"
              />
            </div>
          </div>
        </q-card-section>
        <q-separator />
        <q-card-section class="flex justify-end q-gutter-x-lg q-pa-lg">
          <q-btn
            unelevated
            label="Cancel"
            :ripple="false"
            color="dark"
            @click="closeProviderDialog"
            class="text-capitalize rounded-10 highlighted-border"
          />
          <q-btn
            unelevated
            label="Add Provider"
            icon="add"
            :ripple="false"
            type="submit"
            class="bg-light-green rounded-10 text-white text-capitalize"
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
            Delete User?
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
          <span class="fw-600">{{ this.userInfo.username }}</span
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

  <q-dialog
    v-model="isDeleteSSOProviderDialogOpen"
    backdrop-filter="blur(4px)"
    class="delete-dialog"
  >
    <q-card class="highlighted-border">
      <q-card-section class="flex justify-between items-center q-pa-lg">
        <div class="flex items-center">
          <q-icon size="sm" name="img:/icons/trash.svg" />
          <p class="text-custom-text-secondary fw-600 q-ml-md text-subtitle1">
            Delete SSO Provider?
          </p>
        </div>
        <q-icon
          name="close"
          class="cursor-pointer fs-20"
          @click="isDeleteSSOProviderDialogOpen = false"
        />
      </q-card-section>
      <q-separator />
      <q-card-section>
        <p class="text-custom-gray-dark text-weight-light q-pa-sm w-90">
          Are you sure you want to delete this
          <span class="fw-600">{{ this.providerInfo.providerId }}</span
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
          @click="isDeleteSSOProviderDialogOpen = false"
        />
        <q-btn
          label="Delete"
          unelevated
          color="negative"
          class="rounded-10 text-capitalize min-w-80"
          @click="confirmSSOProviderDelete"
        />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>
<script>
import { defineComponent } from "vue";
import CommonTable from "../shared/CommonTable.vue";
import {
  appAddSSOProviders,
  appAddUsers,
  appDeleteSSOProvidersById,
  appDeleteUserById,
  appGetSSOProviders,
  appGetSSOProvidersById,
  appGetUserById,
  appGetUsers,
} from "./../../services/api";

export default defineComponent({
  name: "AuthComponent",
  components: {
    CommonTable,
  },
  data() {
    return {
      addNewUser: false,
      addNewProvider: false,
      providerInfo: {
        providerId: "",
        alias: "",
        clientId: "",
        secret: "",
        enabled: false,
      },
      userInfo: {
        firstName: "",
        lastName: "",
        username: "",
        email: "",
        password: "",
      },
      isEnabled: true,
      userTableColumns: [
        { name: "id", label: "ID", align: "left", field: "id", sortable: true },
        {
          name: "username",
          label: "Username",
          align: "left",
          field: "username",
        },
        { name: "email", label: "Email", align: "left", field: "email" },
        { name: "actions", label: "Actions", align: "center" },
      ],
      isDeleteDialogOpen: false,
      isDeleteSSOProviderDialogOpen: false,
      userTableData: [],
      ssoTableColumns: [
        {
          name: "providerId",
          label: "Providers",
          align: "left",
          field: "providerId",
          sortable: true,
        },
        {
          name: "alias",
          label: "Alias",
          align: "center",
          field: "alias",
        },
        { name: "actions", label: "Actions", align: "center" },
      ],
      ssoTableData: [],
    };
  },
  mounted() {
    this.getUsers();
    this.getSSOProvider();
  },
  methods: {
    openUserDialog() {
      this.addNewUser = !this.addNewUser;
    },
    openProviderDialog() {
      this.addNewProvider = !this.addNewProvider;
    },
    closeUserDialog() {
      this.addNewUser = false;
      this.$refs.addUserForm.reset();
    },
    closeProviderDialog() {
      this.addNewProvider = false;
      this.$refs.addSSOProviderForm.reset();
    },
    // Auth User
    addUser() {
      appAddUsers(this.userInfo)
        .then(({ data }) => {
          this.getUsers();
        })
        .catch((err) => {})
        .finally(() => {
          this.addNewUser = false;
        });
      this.$refs.addUserForm.reset();
    },
    resetUser() {
      this.userInfo = {
        firstName: "",
        lastName: "",
        username: "",
        email: "",
        password: "",
      };
    },
    getUsers() {
      appGetUsers()
        .then(({ data }) => {
          this.userTableData = data;
        })
        .catch((err) => {});
    },
    getUserById(user) {
      appGetUserById(user.id)
        .then(({ data }) => {
          this.userInfo = data;
          this.addNewUser = true;
        })
        .catch((err) => {});
    },
    openDeleteDialog(row) {
      this.userInfo = row;
      this.isDeleteDialogOpen = true;
    },
    confirmDelete() {
      this.isDeleteDialogOpen = false;
      appDeleteUserById(user.id)
        .then(({ data }) => {
          this.getUsers();
        })
        .catch((err) => {});
    },
    // SSO Proovider
    addSSOProvider() {
      appAddSSOProviders(this.providerInfo)
        .then(({ data }) => {
          this.getSSOProvider();
        })
        .catch((err) => {})
        .finally(() => {
          this.addNewProvider = false;
        });
      this.$refs.addSSOProviderForm.reset();
    },
    resetSSOProvider() {
      this.providerInfo = {
        providerId: "",
        alias: "",
        clientId: "",
        secret: "",
        enabled: false,
      };
    },
    getSSOProvider() {
      appGetSSOProviders()
        .then(({ data }) => {
          this.ssoTableData = data;
        })
        .catch((err) => {});
    },
    getSSOProviderById(user) {
      appGetSSOProvidersById(user.alias)
        .then(({ data }) => {
          this.providerInfo = data;
          this.providerInfo.clientId = data.config?.clientId;
          this.addNewProvider = true;
        })
        .catch((err) => {
          this.addNewProvider = true;
        });
    },
    openSSOProviderDeleteDialog(row) {
      this.providerInfo = row;
      this.isDeleteSSOProviderDialogOpen = true;
    },
    confirmSSOProviderDelete() {
      this.isDeleteSSOProviderDialogOpen = false;
      appDeleteSSOProvidersById(user.alias)
        .then(({ data }) => {
          this.getSSOProvider();
        })
        .catch((err) => {});
    },
  },
  computed: {
    hasUserInfoValues() {
      return Object.values(this.userInfo).some((value) => value);
    },
    hasProviderInfoValues() {
      return Object.values(this.providerInfo).some((value) => value);
    },
  },
});
</script>
