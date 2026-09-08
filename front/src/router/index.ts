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
    { path: "/answers", name: "answer-history", component: () => import("@/views/AnswerHistoryView.vue"), meta: { requiresAuth: true } },
    { path: "/answers/:answerId", name: "answer-detail", component: () => import("@/views/AnswerDetailView.vue"), meta: { requiresAuth: true } },
    {
      path: "/admin",
      component: () => import("@/views/admin/AdminLayoutView.vue"),
      meta: { requiresAuth: true, requiresAdmin: true },
      children: [
        { path: "", name: "admin", component: () => import("@/views/admin/AdminOverviewView.vue") },
        { path: "taxonomy", name: "admin-taxonomy", component: () => import("@/views/admin/AdminTaxonomyView.vue") },
        { path: "knowledge-documents", name: "admin-knowledge", component: () => import("@/views/admin/AdminKnowledgeDocumentView.vue") },
        { path: "questions", name: "admin-questions", component: () => import("@/views/admin/AdminQuestionView.vue") },
        { path: "evaluations", name: "admin-evaluations", component: () => import("@/views/admin/AdminEvaluationView.vue") },
        { path: "members", name: "admin-members", component: () => import("@/views/admin/AdminMembersView.vue") },
      ],
    },
    {
      path: "/admin/forbidden",
      name: "admin-forbidden",
      component: () => import("@/views/admin/AdminForbiddenView.vue"),
      meta: { requiresAuth: true },
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
    return { name: "admin-forbidden" };
  }
  return true;
}

router.beforeEach(authorizationGuard);

export default router;
