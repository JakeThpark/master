package com.wanpan.app.repository;

import com.wanpan.app.entity.JobTask;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobTaskRepository extends JpaRepository<JobTask, Long> {
    JobTask findByJobIdAndRequestId(long jobId, long requestId);
}
