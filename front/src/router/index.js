import { createRouter, createWebHistory } from 'vue-router'
import DashboardView from '../views/DashboardView.vue'
import MemberView from '@/views/MemberView.vue'
import HomeView from '@/views/HomeView.vue'
import OrganizationView from '@/views/OrganizationView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView
    },
    {
      path: '/member',
      name: 'member',
      component: MemberView,
      redirect: { name: 'memberLogin' },
      children: [
        {
          path: 'login',
          name: 'memberLogin',
          component: () => import('@/components/member/MemberLogin.vue')
        },
        {
          path: 'signup',
          name: 'memberSignup',
          component: () => import('@/components/member/MemberSignup.vue')
        }
      ]
    },
    {
      path: '/dashboard',
      name: 'dashboard',
      component: DashboardView,
      redirect: { name: 'dashboardMain' },
      children: [
        {
          path: 'vault',
          name: 'dashboardMain',
          component: () => import('@/components/dashboard/DashboardMain.vue')
        }
        // {
        //   path: 'vault/itemid',
        //   name: 'DashboardInputLoginModal',
        //   component: () => import('@/components/dashboard/DashboardInputLoginModal.vue')
        // }
      ]

      // route level code-splitting
      // this generates a separate chunk (About.[hash].js) for this route
      // which is lazy-loaded when the route is visited.
    },
    {
      path: '/organization',
      name: 'organization',
      component: OrganizationView,
      redirect: { name: 'organizationTeam' },
      children: [
        {
          path: 'team',
          name: 'organizationTeam',
          component: () => import('@/components/organization/OrganizationMain.vue')
        },
        {
          path: 'member',
          name: 'organizationMember',
          component: () => import('@/components/organization/OrganizationMember.vue')
        },
        {
          path: 'team/management',
          name: 'teamManagement',
          component: () => import('@/components/organization/TeamManagement.vue')
        }
      ]
    }
  ]
})

export default router
