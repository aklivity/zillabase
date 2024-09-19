import { RouteRecordRaw } from 'vue-router';

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    component: () => import('layouts/IndexLayout.vue'),
    children: [{ path: '', component: () => import('pages/IndexPage.vue') }],
  },
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

  // Always leave this as last one,
  // but you can also remove it
  {
    path: '/:catchAll(.*)*',
    component: () => import('pages/ErrorNotFound.vue'),
  },
];

export default routes;
