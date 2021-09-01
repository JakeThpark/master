package com.wanpan.app.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@Entity
public class Job extends BaseEntity{

//    private long shopAccountId;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "ENUM")

    private JobType jobType;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "enum('READY','IN_PROGRESS','END')")
    private ExcuteStatus executeStatus;

    @Column(columnDefinition = "TEXT")
    private String requestData;

    public enum JobType {
        WRITE("WRITE"),
        UPDATE("UPDATE"),
        DELETE_SALE("DELETE_SALE"),
        UPDATE_SALE_STATUS("UPDATE_SALE_STATUS"),
        COLLECT_ORDER("COLLECT_ORDER"),
        UPDATE_ORDER("UPDATE_ORDER"),
        COLLECT_QNA("COLLECT_QNA"),
        POST_QNA_ANSWER("POST_QNA_ANSWER"),
        COLLECT_ORDER_CONVERSATION("COLLECT_ORDER_CONVERSATION"),
        POST_ORDER_CONVERSATION("POST_ORDER_CONVERSATION");



        //Json으로 들어오는 String enum에 대한 자동매핑을 위함
        private static Map<String, JobType> FORMAT_MAP = Stream
                .of(JobType.values())
                .collect(Collectors.toMap(s -> s.formatted, Function.identity()));

        private final String formatted;

        JobType(String formatted) {
            this.formatted = formatted;
        }

        @JsonCreator // This is the factory method and must be static
        public static JobType fromString(String value) {
            return Optional
                    .ofNullable(FORMAT_MAP.get(value))
                    .orElseThrow(() -> new IllegalArgumentException(value));
        }
    }

    public enum ExcuteStatus {
        READY,IN_PROGRESS,END
    }

    /*
     * Job에 대한 실제 정보를 가지고 있는 JSON model
     */
    @Data
    public static class JobRequestData{
        //요청작업 타입
        private JobType jobType;

        private String message;
    }
}
