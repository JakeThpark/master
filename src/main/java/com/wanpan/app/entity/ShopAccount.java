package com.wanpan.app.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;

@EqualsAndHashCode(callSuper = true)
@Data
@Entity
public class ShopAccount extends BaseEntity{
    private long companyId;

    @ManyToOne
    @JoinColumn(name = "shop_id")
    private Shop shop;

    private String accountId;
    private String password;
    private String alias;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "ENUM('READY','CONNECTED','DISCONNECTED','FAILED')")
    private Status status; //타입별 의미가 있는 이유

    public enum Status {
        READY,CONNECTED,DISCONNECTED,FAILED
    }
}
