<script setup lang="ts">
import type { KnowledgeStatus, TopicKnowledge } from "@/api/learning";
import QuestionState from "@/components/QuestionState.vue";

defineProps<{ topics: TopicKnowledge[]; details?: boolean }>();
const labels: Record<KnowledgeStatus, string> = { UNKNOWN: "미평가", LEARNING: "학습 중", STABLE: "안정" };
const score = (value: number) => Number(value.toFixed(1));
const date = (value: string) => value.replace("T", " ").slice(0, 16);
</script>

<template>
  <QuestionState v-if="!topics.length" kind="empty" title="아직 학습할 개념이 없어요" description="학습 자료가 준비되면 이곳에 표시됩니다." />
  <div v-else class="knowledge-topics">
    <article v-for="topic in topics" :key="topic.topicId" class="knowledge-topic">
      <header class="section-heading">
        <h3>{{ topic.topicName }}</h3>
        <span class="knowledge-status" :data-status="topic.status">{{ labels[topic.status] }}</span>
      </header>
      <p class="topic-counts">미평가 {{ topic.unknownCount }} · 학습 중 {{ topic.learningCount }} · 안정 {{ topic.stableCount }}</p>
      <p class="topic-scores">
        <span>{{ topic.masteryScore === null ? '숙련도 미평가' : `숙련도 ${score(topic.masteryScore)} / 100` }}</span>
        <span>신뢰도 {{ score(topic.confidenceScore) }} / 100</span>
      </p>
      <ul v-if="details && topic.concepts.length" class="knowledge-concepts">
        <li v-for="concept in topic.concepts" :key="concept.conceptId" :data-concept-id="concept.conceptId">
          <div class="section-heading"><h4>{{ concept.conceptName }}</h4><span class="knowledge-status" :data-status="concept.status">{{ labels[concept.status] }}</span></div>
          <p v-if="concept.masteryScore === null">숙련도 미평가 · 신뢰도 {{ score(concept.confidenceScore) }} / 100</p>
          <template v-else>
            <p>숙련도 {{ score(concept.masteryScore) }} / 100 · 신뢰도 {{ score(concept.confidenceScore) }} / 100</p>
            <meter :value="concept.masteryScore" min="0" max="100" :aria-label="`${concept.conceptName} 숙련도`" />
          </template>
          <p class="concept-history">평가 {{ concept.attemptCount }}회 <template v-if="concept.lastEvaluatedAt">· 최근 <time :datetime="concept.lastEvaluatedAt">{{ date(concept.lastEvaluatedAt) }}</time></template></p>
        </li>
      </ul>
    </article>
  </div>
</template>
