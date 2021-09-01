package com.wanpan.app.repository;

import com.wanpan.app.entity.Job;

import java.util.List;

public interface CustomJobRepository {
    List<Job> findByExecuteStatusGroupByShopAccountIdAndJobType(List<Job.ExcuteStatus> executeStatus);
}
