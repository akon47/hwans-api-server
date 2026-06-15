package com.hwans.apiserver.dto.common;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Getter;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

/**
 * 커서 페이징 조회 응답을 위한 Dto
 *
 * @param <T> Serializable 객체
 */
@Getter
@Builder
@ApiModel(description = "커서 페이징 조회 응답을 위한 Dto")
public class SliceDto<T extends Serializable> {
    @ApiModelProperty(value = "조회된 데이터")
    List<T> data;
    @ApiModelProperty(value = "조회된 데이터 수")
    int size;
    @ApiModelProperty(value = "조회된 데이터가 없는지 여부")
    boolean empty;
    @ApiModelProperty(value = "첫 페이지 여부")
    boolean first;
    @ApiModelProperty(value = "마지막 페이지 여부")
    boolean last;
    @ApiModelProperty(value = "다음 페이지 조회를 위한 커서 Id")
    UUID cursorId;

    /**
     * 커서 페이징 조회 결과로부터 SliceDto를 생성한다.
     * <p>
     * 조회 시 {@code size + 1}개를 가져온 뒤 이 메서드에 전달하면, 마지막 페이지 여부와 다음 커서 Id를 계산하여
     * 최대 {@code size}개의 데이터를 담은 SliceDto를 반환한다.
     *
     * @param found       조회된 엔티티 목록 ({@code size + 1}개까지 조회되었을 수 있음)
     * @param size        한 페이지에 담을 최대 데이터 수
     * @param first       첫 페이지 여부 (일반적으로 cursorId가 없을 때 true)
     * @param mapper      엔티티를 응답 Dto로 변환하는 함수
     * @param idExtractor 다음 페이지 커서로 사용할 엔티티의 Id 추출 함수
     * @param <E>         조회된 엔티티 타입
     * @param <T>         응답 Dto 타입
     * @return 커서 페이징 응답 Dto
     */
    public static <E, T extends Serializable> SliceDto<T> of(
            List<E> found, int size, boolean first,
            Function<E, T> mapper, Function<E, UUID> idExtractor) {
        var last = found.size() <= size;
        var page = found.stream().limit(size).toList();
        return SliceDto.<T>builder()
                .data(page.stream().map(mapper).toList())
                .size(page.size())
                .empty(found.isEmpty())
                .first(first)
                .last(last)
                .cursorId(last || page.isEmpty() ? null : idExtractor.apply(page.get(page.size() - 1)))
                .build();
    }
}
