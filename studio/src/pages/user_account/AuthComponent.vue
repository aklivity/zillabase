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
            @click="addNewUser = !addNewUser"
            class="rounded-10"
          />
          <p class="text-custom-text-secondary text-h6 fw-600">
            Create New User
          </p>
        </div>
        <q-icon
          name="img:/icons/auth.svg"
          class="fs-30 filter-custom-dark"
          style="min-height: 30px"
        />
      </q-card-section>
      <q-separator />
      <q-card-section class="q-py-xl px-28">
        <div class="row items-center">
          <div class="col-3">
            <span class="text-custom-gray-dark text-subtitle1 text-weight-light"
              >Name</span
            >
          </div>
          <div class="col-9">
            <q-input
              dense
              outlined
              placeholder="User Name"
              class="rounded-10 self-center text-weight-light rounded-input"
            />
          </div>
        </div>
        <div class="row items-center q-mt-lg q-pt-md">
          <div class="col-3">
            <span class="text-custom-gray-dark text-subtitle1 text-weight-light"
              >Email</span
            >
          </div>
          <div class="col-9">
            <q-input
              dense
              outlined
              placeholder="User Email"
              type="email"
              class="rounded-10 self-center text-weight-light rounded-input"
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
          class="text-capitalize rounded-10 highlighted-border"
        />
        <q-btn
          unelevated
          label="Add User"
          icon="add"
          :ripple="false"
          class="bg-light-green rounded-10 text-white text-capitalize self-center"
        />
      </q-card-section>
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
            @click="addNewProvider = !addNewProvider"
            class="rounded-10"
          />
          <p class="text-custom-text-secondary text-h6 fw-600">
            Add A Provider
          </p>
        </div>
        <q-icon
          name="img:/icons/auth.svg"
          class="fs-30 filter-custom-dark"
          style="min-height: 30px"
        />
      </q-card-section>
      <q-separator />
      <q-card-section class="q-py-xl px-28">
        <div class="row items-center">
          <div class="col-3">
            <span class="text-custom-gray-dark text-subtitle1 text-weight-light"
              >Name</span
            >
          </div>
          <div class="col-9">
            <q-input
              dense
              outlined
              placeholder="e.g Google"
              class="rounded-10 self-center text-weight-light rounded-input"
            />
          </div>
        </div>
        <div class="row items-center q-mt-lg q-pt-md">
          <div class="col-3 flex items-center">
            <span class="text-custom-gray-dark text-subtitle1 text-weight-light"
              >Enabled</span
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
            <q-checkbox dense v-model="isEnabled" color="light-green" />
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
          class="text-capitalize rounded-10 highlighted-border"
        />
        <q-btn
          unelevated
          label="Add Provider"
          icon="add"
          :ripple="false"
          class="bg-light-green rounded-10 text-white text-capitalize"
        />
      </q-card-section>
    </q-card>
  </q-dialog>
</template>
<script>
import { defineComponent } from "vue";
import CommonTable from "../shared/CommonTable.vue";

export default defineComponent({
  name: "AuthComponent",
  components: {
    CommonTable,
  },
  data() {
    return {
      addNewUser: false,
      addNewProvider: false,
      isEnabled: true,
      userTableColumns: [
        { name: "id", label: "ID", align: "left", field: "id", sortable: true },
        { name: "name", label: "Name", align: "left", field: "name" },
        { name: "email", label: "Email", align: "left", field: "email" },
        { name: "actions", label: "Actions", align: "center" },
      ],
      userTableData: [
        { id: 1, name: "John Doe", email: "johndoe@example.com" },
        { id: 2, name: "John Doe", email: "johndoe@example.com" },
        { id: 3, name: "John Doe", email: "johndoe@example.com" },
        { id: 4, name: "John Doe", email: "johndoe@example.com" },
        { id: 5, name: "John Doe", email: "johndoe@example.com" },
        { id: 6, name: "John Doe", email: "johndoe@example.com" },
      ],
      ssoTableColumns: [
        {
          name: "providers",
          label: "Providers",
          align: "left",
          field: "providers",
          sortable: true,
        },
        {
          name: "enabled",
          label: "Enabled",
          align: "center",
          field: "Enabled",
        },
        { name: "actions", label: "Actions", align: "center" },
      ],
      ssoTableData: [
        { providers: "Google", enabled: true },
        { providers: "Github", enabled: false },
        { providers: "Yahoo", enabled: true },
        { providers: "LinkedIn", enabled: false },
        { providers: "Stackoverflow", enabled: true },
        { providers: "Custom", enabled: false },
      ],
    };
  },
  methods: {
    openUserDialog() {
      this.addNewUser = !this.addNewUser;
    },
    openProviderDialog() {
      this.addNewProvider = !this.addNewProvider;
    },
  },
});
</script>
