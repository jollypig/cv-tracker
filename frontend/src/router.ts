import { createRouter, createWebHistory } from 'vue-router'
import PeopleView from './views/PeopleView.vue'
import PersonFormView from './views/PersonFormView.vue'
import WorkspaceView from './views/WorkspaceView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', component: PeopleView, meta: { title: 'People' } },
    { path: '/people/new', component: PersonFormView, meta: { title: 'Add person' } },
    { path: '/people/:id/edit', component: PersonFormView, meta: { title: 'Edit person' } },
    { path: '/cvs', component: WorkspaceView, meta: { title: 'CV library' } },
  ],
})

export default router