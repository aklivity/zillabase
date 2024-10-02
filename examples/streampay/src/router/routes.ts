import { RouteRecordRaw } from 'vue-router';

const routes: RouteRecordRaw[] = [
  {
    path: '/main',
    component: () => import('layouts/MainLayout.vue'),
    children: [{ path: '', component: () => import('pages/MainPage.vue') }],
  },
  {
    path: '/payorrequest',
    component: () => import('layouts/MainLayout.vue'),
    children: [
      { path: '', component: () => import('pages/PayOrRequestPage.vue') },
      {
        name: 'PayByRequestId',
        path: ':requestId',
        component: () => import('pages/PayOrRequestPage.vue'),
      },
    ],
  },
  {
    path: '/request',
    component: () => import('layouts/MainLayout.vue'),
    children: [{ path: '', component: () => import('pages/RequestPage.vue') }],
  },
  {
    path: '/statement',
    component: () => import('layouts/MainLayout.vue'),
    children: [{ path: '', component: () => import('pages/StatementPage.vue') }],
  },

  {
    path: '/&state=:state',
    component: () => import('layouts/MainLayout.vue'),
    children: [{ path: '', component: () => import('pages/MainPage.vue') }],
  },
  {
    path: '/main&state=:state',
    component: () => import('layouts/MainLayout.vue'),
    children: [{ path: '', component: () => import('pages/MainPage.vue') }],
  },
  {
    path: '/request&state=:state',
    component: () => import('layouts/MainLayout.vue'),
    children: [{ path: '', component: () => import('pages/RequestPage.vue') }],
  },
  {
    path: '/payorrequest&state=:state',
    component: () => import('layouts/MainLayout.vue'),
    children: [{ path: '', component: () => import('pages/PayOrRequestPage.vue') }],
  },
  {
    path: '/statement&state=:state',
    component: () => import('layouts/MainLayout.vue'),
    children: [{ path: '', component: () => import('pages/StatementPage.vue') }],
  },

  // Always leave this as last one,
  // but you can also remove it
  {
    path: '/:catchAll(.*)*',
    component: () => import('pages/ErrorNotFound.vue'),
  },
];

export default routes;
