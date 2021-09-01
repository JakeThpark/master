package com.wanpan.app.repository;

import com.wanpan.app.entity.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface JobRepository extends JpaRepository<Job, Long>, CustomJobRepository {
    List<Job> findByJobTypeAndExecuteStatus(Job.JobType jobType, Job.ExcuteStatus executeStatus);
    List<Job> findByExecuteStatus(Job.ExcuteStatus executeStatus);
}