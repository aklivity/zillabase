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
                  All Snippets
                </p>
              </div>
              <q-btn
                unelevated
                icon="add"
                :ripple="false"
                class="bg-light-green rounded-10 text-white text-capitalize self-center q-pa-sm"
                @click="addNewSnippetDialog"
              />
            </div>
            <div class="row q-mt-md full-width">
              <q-input
                outlined
                dense
                :placeholder="`Search Snippets...`"
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
                  Select A Snippet To Start Editing
                </p>
              </div>
            </div>

            <div class="row justify-center q-pt-lg">
              <div
                class="column q-pa-lg bg-custom-primary text-center select-bucket"
              >
                <div class="flex flex-center q-mb-md">
                  <q-icon class="fs-60" name="img:/icons/sql-snippet.svg" />
                </div>
                <div class="fs-18 text-custom-text-secondary">
                  Select a <b>Snippet</b> to Edit
                </div>
                <div class="text-caption text-custom-text-secondary q-my-sm">
                  OR
                </div>
                <q-btn
                  unelevated
                  label="Add A Snippet"
                  icon="add"
                  :ripple="false"
                  @click="addNewSnippetDialog"
                  class="bg-light-green rounded-10 text-white text-capitalize self-center btn-add-new q-mt-sm"
                />
              </div>
            </div>
          </q-tab-panel>
          <q-tab-panel v-for="tab in tabs" :key="tab.name" :name="tab.name">
            <div class="sql-editor">
              <p class="text-custom-text-secondary text-h6 fw-600 q-pb-lg">
                {{ tab.name }}
              </p>

              <div class="sql-editor-area">
                <q-input
                  outlined
                  type="textarea"
                  placeholder="SELECT * FROM `ZillaBase` ORDER BY id;"
                  rows="12"
                  autogrow
                  v-model="query"
                  class="rounded-10 self-center text-weight-light rounded-input"
                />
              </div>
            </div>
            <q-card
              flat
              bordered
              class="q-mt-md q-pa-none sql-result-container"
            >
              <q-card-section class="q-pa-none overflow-hidden">
                <div
                  class="flex justify-between items-center bg-custom-primary q-px-md q-py-sm"
                >
                  <div
                    class="text-subtitle1 text-custom-text-secondary text-weight-medium"
                  >
                    Results
                  </div>
                  <q-btn
                    unelevated
                    label="Run"
                    :ripple="false"
                    @click="runQuery"
                    class="bg-light-green rounded-10 text-white q-mt-sm"
                  />
                </div>
                <q-separator />
                <q-card-section
                  class="text-grey bg-custom-dark-color sql-result"
                >
                  <pre>{{ resuleSet }}</pre>
                </q-card-section>
              </q-card-section>
            </q-card>
          </q-tab-panel>
        </q-tab-panels>
      </template>
    </q-splitter>
    <q-dialog
      v-model="addNewSnippet"
      backdrop-filter="blur(4px)"
      class="snippet-dialog"
    >
      <q-card class="highlighted-border">
        <q-card-section class="flex justify-between items-center q-pa-lg">
          <div class="flex items-center">
            <q-icon size="sm" name="add" class="filter-custom-dark" />
            <p class="text-custom-text-secondary fw-600 q-ml-md text-subtitle1">
              Add New Snippet
            </p>
          </div>
          <q-icon
            name="close"
            class="cursor-pointer fs-20"
            @click="addNewSnippet = false"
          />
        </q-card-section>
        <q-separator />
        <q-card-section class="q-pb-lg">
          <p class="text-custom-gray-dark text-weight-light q-pb-sm">
            Write Snippet Name
          </p>
          <q-input
            dense
            outlined
            placeholder="e.g my-snippet"
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
            @click="addNewSnippet = false"
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
  </div>
</template>
<script>
import { defineComponent } from "vue";
import { ref } from "vue";
export default defineComponent({
  name: "SqlComponent",
  data() {
    return {
      addNewSnippet: false,
      selectedTab: "initialTab",
      tabs: [
        {
          name: "Run Queries",
        },
      ],
      resuleSet: null,
      query: "",
    };
  },
  setup() {
    return {
      splitterModel: ref(20),
    };
  },
  mounted() {
    this.$ws.addMessageHandler((data) => {
      if (data.type == "execute_queries") {
        this.resuleSet = JSON.stringify(data.data, null, 4);
      }
    });
  },
  beforeUnmount() {
    this.$ws.removeAll();
  },
  methods: {
    addNewSnippetDialog() {
      this.addNewSnippet = !this.addNewSnippet;
    },
    runQuery() {
      this.$ws.sendMessage(this.query, "execute_queries");
    },
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

.sql-result-container {
  border-radius: 20px;
  .sql-result {
    height: calc(100vh - 490px);
    overflow: auto;
  }
}
</style>
