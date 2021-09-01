package com.wanpan.app.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import javax.persistence.*;
import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Data
@Entity
public class ShopAccountToken extends BaseEntity {

    @Column(nullable = false)
    private String shopId;

    @Column(nullable = false)
    private String accountId;

    private String token;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "ENUM('SESSION','BEARER')")
    private Type type;

    public enum Type {
        SESSION,BEARER
    }

}
