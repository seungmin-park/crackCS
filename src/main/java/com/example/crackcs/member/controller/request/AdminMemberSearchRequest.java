package com.example.crackcs.member.controller.request;

import com.example.crackcs.common.web.PageRequestFactory;
import com.example.crackcs.member.domain.MemberRole;
import com.example.crackcs.member.domain.MemberStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Set;

public record AdminMemberSearchRequest(
        MemberRole role,
        MemberStatus status,
        @Min(value = 0, message = "page는 0 이상이어야 합니다.") Integer page,
        @Min(value = 1, message = "size는 1 이상이어야 합니다.")
        @Max(value = 100, message = "size는 100 이하여야 합니다.") Integer size,
        List<String> sort
) {
    private static final Set<String> SORTABLE_PROPERTIES = Set.of(
            "id", "nickname", "role", "status", "createdAt", "updatedAt"
    );

    public Pageable toPageable() {
        return PageRequestFactory.create(page, size, sort, SORTABLE_PROPERTIES);
    }
}
