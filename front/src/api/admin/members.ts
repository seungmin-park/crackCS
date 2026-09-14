import { get, patch } from "@/api/client";
import { queryString, type PageRequest, type PageResponse } from "@/api/pagination";

export type MemberStatus = "ACTIVE" | "BLOCKED" | "WITHDRAWN";

export type AdminMember = {
  id: number;
  nickname: string;
  role: "USER" | "ADMIN";
  status: MemberStatus;
};

export function fetchAdminMembers(filters: { status?: MemberStatus } & PageRequest = {}): Promise<PageResponse<AdminMember>> {
  return get(`/api/admin/members${queryString(filters)}`);
}

export function updateMemberStatus(id: number, status: MemberStatus): Promise<AdminMember> {
  return patch(`/api/admin/members/${id}/status`, { status });
}
