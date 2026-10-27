package com.risk.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/** Read-only access to verified aggregate public-data snapshots. */
@Mapper
public interface OfficialDataSnapshotMapper {

    @Select("<script>" +
            "SELECT id, source_name, source_url, dataset_code, metric_key, country_code, country_name, " +
            "reference_period, metric_value, unit, data_status, retrieved_at, content_sha256, provenance_note " +
            "FROM t_public_data_snapshot WHERE data_status = 'official_public' " +
            "<if test='datasetCode != null and datasetCode != \"\"'> AND dataset_code = #{datasetCode}</if> " +
            "<if test='countryCode != null and countryCode != \"\"'> AND country_code = #{countryCode}</if> " +
            "ORDER BY reference_period DESC, dataset_code, country_code LIMIT #{limit}" +
            "</script>")
    List<Map<String, Object>> findSnapshots(@Param("datasetCode") String datasetCode,
                                             @Param("countryCode") String countryCode,
                                             @Param("limit") int limit);

    @Select("SELECT dataset_code, source_name, COUNT(*) AS record_count, MAX(retrieved_at) AS last_retrieved_at " +
            "FROM t_public_data_snapshot WHERE data_status = 'official_public' " +
            "GROUP BY dataset_code, source_name ORDER BY dataset_code")
    List<Map<String, Object>> summarizeSources();
}
