import { describe, expect, it, vi } from "vitest";

import { fetchAllPages } from "@/views/admin/adminPagination";

describe("관리자 pagination 정책", () => {
  it("관계 후보 페이지가 안전 상한을 넘으면 불완전 목록 대신 오류를 낸다", async () => {
    const fetchPage = vi.fn().mockResolvedValue({
      content: [{ id: 1 }], page: 0, size: 100, totalElements: 100_100, totalPages: 1001,
    });

    await expect(fetchAllPages(fetchPage)).rejects.toThrow("관계 후보 페이지 수가 안전 상한을 초과했습니다.");
    expect(fetchPage).toHaveBeenCalledTimes(1);
  });
});
