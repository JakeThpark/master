package com.wanpan.app.dto.job;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.wanpan.app.entity.Job;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;

@Data
@NoArgsConstructor
public class JobBaseRequest<T> {
    @JsonProperty("shop_account_id")
    private String shopAccountId;

    @JsonProperty("job_type")
    @Enumerated(EnumType.STRING)
    private Job.JobType jobType;

    @JsonProperty("excute_status")
    @Enumerated(EnumType.STRING)
    private Job.ExcuteStatus excuteStatus;

    @JsonProperty("request_data")
    private T requestData;
}
