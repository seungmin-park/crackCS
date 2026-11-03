import { clearCsrfToken, get, post } from "@/api/client";

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

export type SignUpResponse = { message: string };

export function signUp(input: SignUpInput): Promise<SignUpResponse> {
  return post<SignUpResponse>("/api/auth/sign-up", input, undefined, { authentication: "anonymous" });
}

export async function login(input: LoginInput): Promise<Member> {
  const member = await post<Member>("/api/auth/login", input, undefined, { authentication: "credentials" });
  // The server rotates the session CSRF token on login, so the pre-login token is no longer valid.
  clearCsrfToken();
  return member;
}

export function logout(): Promise<void> {
  return post<void>("/api/auth/logout", undefined, undefined, { authentication: "credentials" });
}

export function fetchCurrentMember(): Promise<Member> {
  return get<Member>("/api/members/me", { authentication: "anonymous" });
}
