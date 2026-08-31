import { get, post } from "@/api/client";

export type Member = {
  id: number;
  nickname: string;
  role: "USER" | "ADMIN";
  status: "ACTIVE" | "BLOCKED" | "WITHDRAWN";
};

export type SignUpInput = {
  email: string;
  password: string;
  nickname: string;
};

export type LoginInput = {
  email: string;
  password: string;
};

export function signUp(input: SignUpInput): Promise<Member> {
  return post<Member>("/api/auth/sign-up", input);
}

export function login(input: LoginInput): Promise<Member> {
  return post<Member>("/api/auth/login", input);
}

export function logout(): Promise<void> {
  return post<void>("/api/auth/logout");
}

export function fetchCurrentMember(): Promise<Member> {
  return get<Member>("/api/members/me");
}
