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
          </q-card-section>
          <q-tab v-for="(tab, index) in tabs" :key="tab.name" :name="tab.name">
            <!-- Tab name on the left -->
            <span
              class="text-custom-gray-dark text-capitalize text-weight-light no-uppercase"
              >{{ tab.name }}</span
            >
            <!-- Buttons on the right -->
            <div class="flex">
              <q-btn
                flat
                dense
                icon="img:/icons/trash.svg"
                class="q-ml-md"
                @click="deleteTab(index)"
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
                <div
                  class="editor-header flex justify-between items-center bg-custom-primary q-px-md q-py-sm"
                >
                  <div
                    class="text-subtitle1 text-custom-text-secondary text-weight-medium"
                  >
                    Editor
                  </div>
                  <q-btn
                    unelevated
                    label="Save"
                    :ripple="false"
                    @click="saveSnippet"
                    class="bg-primary rounded-10 text-white"
                  />
                </div>
                <q-separator />

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
                <div>
                  <q-table
                    :rows="rows"
                    :columns="columns"
                    row-key="id"
                  />
                </div>
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
            v-model="newSnippetName"
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
            @click="createSnippet"
          />
        </q-card-actions>
      </q-card>
    </q-dialog>
  </div>
</template>
<script>
import { defineComponent } from "vue";
import { ref } from "vue";
import axios from "axios";
export default defineComponent({
  name: "SqlComponent",
  data() {
    return {
      addNewSnippet: false,
      selectedTab: "initialTab",
      tabs: [],
      rows: [],
      columns: [],
      query: "",
      baseUrl: "http://localhost:7184/v1",
      newSnippetName: "",
      snippetEtag: "",
    };
  },
  watch: {
    selectedTab(newVal) {
      this.query = "";
      this.rows = [];
      this.columns = [];
      this.snippetEtag = "";
      if (newVal && newVal !== "initialTab") {
        this.loadSnippet(newVal);
      }
    },
  },
  setup() {
    return {
      splitterModel: ref(20),
    };
  },
  mounted() {
    this.fetchSnippets();
    this.$ws.addMessageHandler((data) => {
      if (data.type == "execute_queries") {
        this.processQueryResult(data.data);
      }
    });
  },
  beforeUnmount() {
    this.$ws.removeAll();
  },
  methods: {
    addNewSnippetDialog() {
      this.newSnippetName = "";
      this.addNewSnippet = true;
    },
    async deleteTab(index) {
      const snippetName = this.tabs[index].name;
      try {
        await axios.delete(`${this.baseUrl}/snippet/${encodeURIComponent(snippetName)}`);
        this.tabs.splice(index, 1);
        if (this.tabs.length === 0) {
          this.selectedTab = "initialTab";
          this.query = "";
        } else if (this.selectedTab === snippetName) {
          this.selectedTab = this.tabs[0].name;
        }
      } catch (err) {
        console.error("Failed to delete snippet", err);
      }
    },
    runQuery() {
      this.$ws.sendMessage(this.query, "execute_queries");
    },
    processQueryResult(data) {
      if (data.length > 0) {
        this.columns = Object.keys(data[0]).map((key) => ({
          name: key,
          label: key.replace(/_/g, ' ').toUpperCase(), // Optional: format label
          align: "left",
          field: key,
        }));
        this.rows = data.map((row, index) => ({ id: index + 1, ...row }));
      } else {
        this.columns = [];
        this.rows = [];
      }
    },
    async fetchSnippets() {
      try {
        const { data } = await axios.get(`${this.baseUrl}/snippet`);
        this.tabs = data
          .filter((item) => item.type === "file")
          .map((item) => ({ name: item.path }));
        if (this.tabs.length > 0) {
          if (!this.tabs.some((t) => t.name === this.selectedTab)) {
            this.selectedTab = this.tabs[0].name;
          }
        } else {
          this.selectedTab = "initialTab";
        }
      } catch (err) {
        console.error("Failed to list snippets", err);
      }
    },
    async loadSnippet(name) {
      try {
        const response = await axios.get(
          `${this.baseUrl}/snippet/${encodeURIComponent(name)}`
        );
        let text = "";
        const body = response.data;
        if (typeof body === "string") {
          try {
            const parsed = JSON.parse(body);
            text =
              parsed && typeof parsed === "object" && "content" in parsed
                ? parsed.content
                : body;
          } catch {
            text = body;
          }
        } else if (body && typeof body === "object" && "content" in body) {
          text = body.content;
        }
        this.query = text;
        this.snippetEtag = response.headers.etag || "";
      } catch (err) {
        console.error("Failed to load snippet", err);
      }
    },
    async createSnippet() {
      const name = this.newSnippetName.trim();
      if (!name) return;
      try {
        // Always start a brand‑new snippet with empty content
        await axios.post(
          `${this.baseUrl}/snippet/${encodeURIComponent(name)}`,
          { content: "" },
          { headers: { "Content-Type": "application/json" } }
        );
        this.addNewSnippet = false;

        // Refresh list and focus the newly created snippet
        await this.fetchSnippets();
        this.selectedTab = name;
        this.query = "";
        this.snippetEtag = "";
      } catch (err) {
        console.error("Failed to create snippet", err);
      }
    },
    async saveSnippet() {
      if (!this.selectedTab || this.selectedTab === "initialTab") return;
      try {
        await axios.put(
          `${this.baseUrl}/snippet/${encodeURIComponent(this.selectedTab)}`,
          this.query,
          {
            headers: {
              "If-Match": this.snippetEtag || "*"
            },
          }
        );
        await this.loadSnippet(this.selectedTab);
      } catch (err) {
        if (axios.isAxiosError(err) && err.response?.status === 412) {
          this.$q.notify({
            type: "negative",
            message: "Snippet was modified elsewhere—please reload and try again.",
          });
        } else {
          console.error("Failed to update snippet", err);
        }
      }
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
  .no-uppercase {
    text-transform: none;
  }

.sql-editor-area {
  /* allow textarea to scroll while keeping header fixed */
  max-height: 400px;      /* adjust as needed */
  overflow-y: auto;
}

.editor-header {
  position: sticky;
  top: 0;
  z-index: 2;
}
</style>
