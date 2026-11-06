package com.hwans.apiserver.dto.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.io.Serializable;
import java.util.List;
import java.util.function.Function;

/**
 * 페이지 번호 기반 조회 응답 Dto (관리자 화면처럼 전체 개수와 페이지 이동이 필요한 곳에 쓴다)
 */
@Getter
@Builder
@Schema(description = "페이지 번호 기반 조회 응답 Dto")
public class PageDto<T> implements Serializable {
    @Schema(description = "조회된 데이터", requiredMode = Schema.RequiredMode.REQUIRED)
    List<T> data;
    @Schema(description = "현재 페이지 (0부터)", requiredMode = Schema.RequiredMode.REQUIRED)
    int page;
    @Schema(description = "페이지 크기", requiredMode = Schema.RequiredMode.REQUIRED)
    int size;
    @Schema(description = "전체 데이터 수", requiredMode = Schema.RequiredMode.REQUIRED)
    long totalElements;
    @Schema(description = "전체 페이지 수", requiredMode = Schema.RequiredMode.REQUIRED)
    int totalPages;

    public static <E, T extends Serializable> PageDto<T> of(Page<E> page, Function<E, T> mapper) {
        return PageDto.<T>builder()
                .data(page.getContent().stream().map(mapper).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();
    }
}
