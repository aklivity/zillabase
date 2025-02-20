const routes = [
  {
    path: "/",
    component: () => import("layouts/MainLayout.vue"),
    children: [
      {
        path: "/overview",
        name: "overview",
        component: () =>
          import("/src/pages/user_account/OverviewComponent.vue"),
        meta: {
          title: "Overview",
          icon: "/icons/layer.svg",
        },
      },
      {
        path: "/apidoc",
        name: "apidoc",
        component: () => import("/src/pages/user_account/ApiDocComponent.vue"),
        meta: {
          title: "API Docs",
          icon: "/icons/document-text.svg",
        },
      },
      {
        path: "/tables",
        name: "tables",
        component: () => import("/src/pages/user_account/TableComponent.vue"),
        meta: {
          title: "Tables",
          icon: "/icons/grid-6.svg",
        },
      },
      {
        path: "/functions",
        name: "functions",
        component: () =>
          import("/src/pages/user_account/FunctionComponent.vue"),
        meta: {
          title: "Functions",
          icon: "/icons/function.svg",
        },
      },
      {
        path: "/views",
        name: "views",
        component: () => import("/src/pages/user_account/ViewsComponent.vue"),
        meta: {
          title: "Views",
          icon: "/icons/view.svg",
        },
      },
      {
        path: "/auth",
        name: "Auth",
        component: () => import("/src/pages/user_account/AuthComponent.vue"),
        meta: {
          title: "Auth",
          icon: "/icons/auth.svg",
        },
      },
      {
        path: "/storage",
        name: "Storage",
        component: () => import("/src/pages/user_account/StorageComponent.vue"),
        meta: {
          title: "Storage",
          icon: "/icons/storage.svg",
        },
      },
      {
        path: "/sql",
        name: "Sql",
        component: () => import("/src/pages/user_account/SqlComponent.vue"),
        meta: {
          title: "Sql Editor",
          icon: "/icons/editor.svg",
        },
      },
    ],
  },

  // Always leave this as last one,
  // but you can also remove it
  {
    path: "/:catchAll(.*)*",
    component: () => import("pages/ErrorNotFound.vue"),
  },
];

export default routes;
