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

export function fetchQuestions(): Promise<PublicQuestionPage> {
  return get<PublicQuestionPage>("/api/questions?size=20&sort=id,asc");
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
