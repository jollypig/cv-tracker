import { createRouter, createWebHistory } from 'vue-router'
import WorkspaceView from './views/WorkspaceView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', component: WorkspaceView, meta: { title: 'People' } },
    { path: '/cvs', component: WorkspaceView, meta: { title: 'CV library' } },
  ],
})

export default router