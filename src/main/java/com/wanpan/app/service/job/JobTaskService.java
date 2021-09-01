package com.wanpan.app.service.job;

import com.wanpan.app.entity.Job;
import com.wanpan.app.entity.JobTask;
import com.wanpan.app.repository.JobTaskRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.Optional;

@Service
@AllArgsConstructor
public class JobTaskService {
    private final JobTaskRepository jobTaskRepository;

    public JobTask checkAndChangeJobTask(long jobId, long requestId, Job.JobType jobType, Job.ExcuteStatus excuteStatus,
                                         boolean successFlag, String message) {
        JobTask jobTask = jobTaskRepository.findByJobIdAndRequestId(jobId, requestId);
        if (ObjectUtils.isEmpty(jobTask)) {
            jobTask = new JobTask(jobId, requestId, excuteStatus, jobType, successFlag, message);
        } else {
            jobTask.setExcuteStatus(excuteStatus);
            jobTask.setSuccessFlag(successFlag);
            jobTask.setMessage(message);
        }
        return jobTaskRepository.save(jobTask);
    }

    public JobTask checkAndChangeJobTask(long jobId, long requestId, Job.JobType jobType) {
        return checkAndChangeJobTask(jobId, requestId, jobType, Job.ExcuteStatus.IN_PROGRESS, false, null);
    }

    public void deleteJobTask(JobTask jobTask) {
        jobTaskRepository.delete(jobTask);
    }

}
