import { createRouter, createWebHistory, type RouteLocationNormalized } from "vue-router";

import { useAuth } from "@/composables/useAuth";

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: "/", redirect: "/questions" },
    {
      path: "/sign-up",
      name: "sign-up",
      component: () => import("@/views/SignUpView.vue"),
      meta: { guestOnly: true },
    },
    {
      path: "/login",
      name: "login",
      component: () => import("@/views/LoginView.vue"),
      meta: { guestOnly: true },
    },
    {
      path: "/questions",
      name: "questions",
      component: () => import("@/views/QuestionListView.vue"),
      meta: { requiresAuth: true },
    },
    {
      path: "/questions/:questionId",
      name: "question-detail",
      component: () => import("@/views/QuestionDetailView.vue"),
      meta: { requiresAuth: true },
    },
    {
      path: "/admin",
      name: "admin",
      component: () => import("@/views/AdminDashboardView.vue"),
      meta: { requiresAuth: true, requiresAdmin: true },
    },
  ],
});

export async function authorizationGuard(to: RouteLocationNormalized) {
  const auth = useAuth();
  await auth.restoreAuthentication();

  if (to.meta.guestOnly && auth.currentMember.value) {
    return { name: "questions" };
  }
  if (to.meta.requiresAuth && !auth.currentMember.value) {
    return { name: "login", query: { redirect: to.fullPath } };
  }
  if (to.meta.requiresAdmin && auth.currentMember.value?.role !== "ADMIN") {
    return { name: "questions" };
  }
  return true;
}

router.beforeEach(authorizationGuard);

export default router;
