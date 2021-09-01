package com.wanpan.app.repository;

import com.wanpan.app.entity.Job;
import com.wanpan.app.entity.QJob;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class CustomJobRepositoryImpl extends QuerydslRepositorySupport implements CustomJobRepository {
    QJob job = QJob.job;
//
    public CustomJobRepositoryImpl() {
        super(Job.class);
    }
//
    @Override
    public List<Job> findByExecuteStatusGroupByShopAccountIdAndJobType(List<Job.ExcuteStatus> executeStatus) {
        return from(job)
                .where(job.executeStatus.in(executeStatus))
//                .groupBy(job.shopAccountId)
                .fetch();
    }

}
