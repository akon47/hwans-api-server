package com.hwans.apiserver.mapper;

import com.hwans.apiserver.dto.blog.SeriesDto;
import com.hwans.apiserver.dto.blog.SeriesRequestDto;
import com.hwans.apiserver.dto.blog.SimpleSeriesDto;
import com.hwans.apiserver.entity.blog.Series;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * 시리즈 엔티티와 시리즈 데이터 모델 사이의 변환을 제공한다.
 */
@Mapper(componentModel = "spring")
public interface SeriesMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "account", ignore = true)
    Series SeriesRequestDtoToEntity(SeriesRequestDto seriesRequestDto);

    SeriesDto EntityToSeriesDto(Series series);

    SimpleSeriesDto EntityToSimpleSeriesDto(Series series);
}
