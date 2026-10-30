<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from "vue";

const landing = ref<HTMLElement>();
let sectionObserver: IntersectionObserver | undefined;
let wordObserver: IntersectionObserver | undefined;
onMounted(() => {
  if (
    typeof IntersectionObserver === "undefined" ||
    (typeof matchMedia === "function" &&
      matchMedia("(prefers-reduced-motion: reduce)").matches)
  )
    return;
  const reveal: IntersectionObserverCallback = (entries, observer) => {
    entries.forEach((entry) => {
      if (entry.isIntersecting) {
        entry.target.classList.add("is-visible");
        observer.unobserve(entry.target);
      }
    });
  };
  sectionObserver = new IntersectionObserver(reveal, { threshold: 0.12 });
  wordObserver = new IntersectionObserver(reveal, {
    rootMargin: "0px 0px -20% 0px",
    threshold: 0.8,
  });
  landing.value?.querySelectorAll(".reveal").forEach((section) => {
    section.classList.add("is-observed");
    sectionObserver?.observe(section);
  });
  landing.value
    ?.querySelectorAll<HTMLElement>(".word")
    .forEach((word, index) => {
      word.classList.add("is-observed");
      word.style.transitionDelay = `${index * 60}ms`;
      wordObserver?.observe(word);
    });
});
onBeforeUnmount(() => {
  sectionObserver?.disconnect();
  wordObserver?.disconnect();
});
</script>

<template>
  <main ref="landing" class="landing-page">
    <div class="landing-wrap">
      <section class="hero">
        <div>
          <p class="eyebrow">생각을 꺼내는 CS 학습, CrackCS</p>
          <h1>알고 있는 CS를,<span>설명할 수 있는 CS로.</span></h1>
          <p class="hero-lede">
            분명 아는 개념인데, 말로 설명하면 막히나요?<br />직접 답하고, 평가
            근거를 확인하고,<br />부족한 개념을 다시 연결하세요.
          </p>
          <RouterLink class="primary-button" to="/questions"
            >문제 풀어보기 <span aria-hidden="true">↗</span></RouterLink
          >
          <p class="hero-note">
            학습 계정으로 시작해요 · 답변은 제출 후 저장돼요
          </p>
        </div>
        <div class="preview-wrap" aria-label="답변 평가 화면 예시">
          <article class="preview-window">
            <header>
              <span class="preview-brand">crackcs.</span
              ><span class="difficulty-pill">답변 평가 예시</span>
            </header>
            <div class="preview-content">
              <p class="eyebrow">운영체제 · 기본</p>
              <h2>프로세스와 스레드는<br />어떻게 다른가요?</h2>
              <p class="preview-answer">
                프로세스는 독립된 메모리 공간을 갖고,<br />스레드는 프로세스
                안에서 자원을 공유합니다.
              </p>
              <div class="preview-feedback">
                <span class="difficulty-pill">개념별 피드백 예시</span>
                <p>
                  자원 공유의 차이를 잘 설명했어요.<br />스레드의 독립적인
                  스택도 연결해 볼까요?
                </p>
              </div>
              <p class="hero-note">
                실제 학습 기록이 아닌 화면 설명용 예시입니다.
              </p>
            </div>
          </article>
          <p class="preview-caption">
            읽는 공부에서, 설명하는 공부로 <span>01 / 03</span>
          </p>
        </div>
      </section>
      <div class="proof-strip">
        <p><strong>서술형 답변</strong>으로 이해 확인</p>
        <p><strong>참고 문서</strong>로 평가 근거 확인</p>
        <p><strong>지식 지도</strong>로 다음 학습 연결</p>
      </div>
      <section id="benefits" class="landing-section reveal">
        <header class="landing-section-heading">
          <div>
            <p class="eyebrow">막연한 이해에서, 분명한 설명으로</p>
            <h2>정답을 읽는 것만으로는<br />알 수 없던 나의 빈틈.</h2>
          </div>
          <p>
            답변 하나를 출발점으로,<br />무엇을 알고 무엇을 더 배워야 할지
            확인해요.
          </p>
        </header>
        <div class="benefit-grid">
          <article class="benefit">
            <span class="feature-number">01</span>
            <h3>설명이 막히는 지점을 발견해요</h3>
            <p>
              보기에서 정답을 찾는 대신, 개념을 내 언어로 풀어내며 이해를
              확인해요.
            </p>
          </article>
          <article class="benefit">
            <span class="feature-number">02</span>
            <h3>평가의 이유까지 확인해요</h3>
            <p>
              맞고 틀렸다는 결과를 넘어, 개념별 피드백과 참고 문서의 근거를 함께
              읽어요.
            </p>
          </article>
          <article class="benefit">
            <span class="feature-number">03</span>
            <h3>다음에 공부할 것이 보여요</h3>
            <p>
              지식 지도에서 부족한 개념을 찾고, 연결된 후속 질문으로 학습을
              이어가요.
            </p>
          </article>
        </div>
      </section>
      <section class="tagline">
        <p class="eyebrow">조금씩, 내 것이 되는 지식</p>
        <h2>
          <span class="word">외운</span> <span class="word">문장보다,</span
          ><span class="tagline-line"
            ><span class="word">내가</span> <span class="word">설명한</span>
            <span class="word">한</span> <span class="word">문장.</span></span
          >
        </h2>
      </section>
      <section id="how" class="reveal">
        <header class="landing-section-heading">
          <div>
            <p class="eyebrow">한 질문에서 시작하는 학습</p>
            <h2>답하고, 확인하고, 연결해요.</h2>
          </div>
        </header>
        <div class="steps-grid">
          <article>
            <p class="eyebrow">STEP 01</p>
            <h3>궁금한 질문을 골라요</h3>
            <p>
              주제와 난이도를 살펴보고,<br />지금 설명해 보고 싶은 질문을
              선택해요.
            </p>
          </article>
          <article>
            <p class="eyebrow">STEP 02</p>
            <h3>내 언어로 설명해요</h3>
            <p>
              완벽하지 않아도 괜찮아요.<br />지금 이해한 만큼, 생각을 글로 꺼내
              보세요.
            </p>
          </article>
          <article>
            <p class="eyebrow">STEP 03</p>
            <h3>근거를 읽고 다시 연결해요</h3>
            <p>
              답변의 빈틈과 참고 근거를 확인하고,<br />다음 질문으로 이해를
              넓혀요.
            </p>
          </article>
        </div>
      </section>
      <section class="landing-section reveal">
        <div class="landing-evidence">
          <div>
            <p class="eyebrow">점수보다 중요한, 그다음 이해</p>
            <h2>“왜 이런 평가인가요?”<br />근거에서 답을 찾아요.</h2>
            <p>
              평가 결과 옆에 참고 문서와 연결된 개념을 함께 보여줘요. 어떤
              설명을 보완하면 좋을지 직접 확인하세요.
            </p>
          </div>
          <article class="source-example">
            <span class="difficulty-pill">참고 근거 예시</span>
            <blockquote>
              “스레드는 코드와 힙을 공유하지만,<br />각자의 스택과 레지스터를
              가집니다.”
            </blockquote>
            <p>운영체제 학습 문서 · 프로세스와 스레드</p>
            <small>화면 이해를 위한 예시 문구입니다.</small>
          </article>
        </div>
      </section>
      <section id="faq" class="landing-section reveal">
        <header class="landing-section-heading">
          <div>
            <p class="eyebrow">시작하기 전에</p>
            <h2>궁금한 점을 모았어요.</h2>
          </div>
        </header>
        <div class="landing-faq">
          <details>
            <summary>누구를 위한 서비스인가요?</summary>
            <p>
              CS 개념을 자기 언어로 설명하는 연습이 필요한 개발 학습자를 위한
              서비스예요. 기술 면접 준비와 개념 복습에 활용할 수 있어요.
            </p>
          </details>
          <details>
            <summary>어떤 방식으로 문제를 푸나요?</summary>
            <p>
              객관식 대신 서술형 답변을 작성해요. 난이도에 맞는 문제를 고른 뒤,
              지금 이해한 내용을 설명해 보세요.
            </p>
          </details>
          <details>
            <summary>답변은 어떻게 평가하나요?</summary>
            <p>
              참고 문서를 바탕으로 개념별 판정과 평가 근거를 제공해요. 평가가
              완료되지 않거나 근거 검토가 필요하면 그 상태를 구분해 알려드려요.
            </p>
          </details>
          <details>
            <summary>부족한 개념은 어떻게 복습하나요?</summary>
            <p>
              평가 근거를 읽고 지식 지도에서 개념별 상태를 확인해요. 관련 질문을
              다시 풀며 설명을 보완할 수 있어요.
            </p>
          </details>
          <details>
            <summary>평가 중에 화면을 닫아도 되나요?</summary>
            <p>
              제출한 답변을 먼저 저장하고 평가를 진행해요. 답변 이력에서 다시
              열면 현재 평가 상태를 확인할 수 있어요.
            </p>
          </details>
          <details>
            <summary>지식 지도의 신뢰도는 무엇인가요?</summary>
            <p>
              개념에 대한 평가가 얼마나 쌓였는지를 나타내는 값이에요. AI 판정이
              정답일 확률을 의미하지 않아요.
            </p>
          </details>
        </div>
      </section>
      <section class="final-cta reveal">
        <p class="eyebrow">내 언어로 쌓는 CS</p>
        <h2>오늘의 한 질문이,<br />내일의 분명한 설명이 되도록.</h2>
        <p>지금 아는 만큼, 첫 문장을 써 보세요.</p>
        <RouterLink class="primary-button" to="/questions"
          >문제 풀어보기 <span aria-hidden="true">↗</span></RouterLink
        >
      </section>
    </div>
  </main>
</template>
