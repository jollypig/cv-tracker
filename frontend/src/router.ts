import { createRouter, createWebHistory } from 'vue-router'
import PeopleView from './views/PeopleView.vue'
import PersonFormView from './views/PersonFormView.vue'
import WorkspaceView from './views/WorkspaceView.vue'
import CvFormView from './views/CvFormView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', component: PeopleView, meta: { title: 'People' } },
    { path: '/people/new', component: PersonFormView, meta: { title: 'Add person' } },
    { path: '/people/:id/edit', component: PersonFormView, meta: { title: 'Edit person' } },
    { path: '/cvs', component: WorkspaceView, meta: { title: 'CV library' } },
    { path: '/cvs/new', component: CvFormView, meta: { title: 'Create CV' } },
    { path: '/cvs/:id/edit', component: CvFormView, meta: { title: 'Edit CV' } },
    { path: '/people/:personId/cvs', component: WorkspaceView, meta: { title: 'Person CVs' } },
    { path: '/people/:personId/cvs/new', component: CvFormView, meta: { title: 'Create CV' } },
  ],
})

export default router