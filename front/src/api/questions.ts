import { get } from "@/api/client";

export type QuestionDifficulty = "BASIC" | "INTERMEDIATE" | "ADVANCED";

export type PublicQuestion = {
  id: number;
  topic: {
    id: number;
    code: string;
    name: string;
  };
  difficulty: QuestionDifficulty;
  content: string;
};

export type PublicQuestionPage = {
  content: PublicQuestion[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

export function fetchQuestions(options: { page?: number; difficulty?: QuestionDifficulty | undefined } = {}): Promise<PublicQuestionPage> {
  const query = new URLSearchParams({ page: String(options.page ?? 0), size: "20", sort: "id,asc" });
  if (options.difficulty) query.set("difficulty", options.difficulty);
  return get<PublicQuestionPage>(`/api/questions?${query}`);
}

export function fetchQuestion(questionId: string): Promise<PublicQuestion> {
  return get<PublicQuestion>(`/api/questions/${encodeURIComponent(questionId)}`);
}

export function difficultyLabel(difficulty: QuestionDifficulty): string {
  return {
    BASIC: "기본",
    INTERMEDIATE: "중급",
    ADVANCED: "심화",
  }[difficulty];
}
