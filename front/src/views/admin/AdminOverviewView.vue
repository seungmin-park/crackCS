<template>
  <section>
    <header class="admin-page-heading">
      <div><p class="eyebrow">ADMIN WORKSPACE</p><h1>콘텐츠 운영</h1></div>
      <p>분류 체계에서 시작해 근거 문서와 평가 문제를 공개합니다.</p>
    </header>
    <div class="admin-flow" aria-label="콘텐츠 공개 순서">
      <RouterLink to="/admin/taxonomy"><span>01</span><strong>분류와 개념</strong><small>활성 분류 체계 구성</small></RouterLink>
      <RouterLink to="/admin/knowledge-documents"><span>02</span><strong>근거 문서</strong><small>출처·버전 검수</small></RouterLink>
      <RouterLink to="/admin/questions"><span>03</span><strong>문제</strong><small>평가 기준·가중치 검수</small></RouterLink>
    </div>
  </section>
</template>
