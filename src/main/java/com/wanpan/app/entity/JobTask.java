package com.wanpan.app.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@Entity
public class JobTask extends BaseEntity{
    private long jobId;

    private long requestId;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "ENUM")
    private Job.ExcuteStatus excuteStatus;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "ENUM")
    private Job.JobType jobType;

    private boolean successFlag;
    private String message;


    public JobTask(long jobId, long requestId, Job.ExcuteStatus executeStatus, Job.JobType jobType, boolean successFlag, String message){
        this.requestId = requestId;
        this.excuteStatus = executeStatus;
        this.jobId = jobId;
        this.jobType = jobType;
        this.successFlag = successFlag;
        this.message = message;
    }
}
