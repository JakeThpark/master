package com.wanpan.app.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Data
@Entity
public class ShopNotice extends BaseEntity {
    @ManyToOne
    @JoinColumn(name = "shop_id")
    private Shop shop;
    //    private long shopId;
    private String shopNoticeId;
    private String subject;
    private String writer;
    private LocalDateTime registeredDate;
    @Column(columnDefinition = "LONGTEXT")
    private String contents;


}
