package com.hwans.apiserver.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.persistence.*;
import java.util.UUID;


@Data
@Accessors(chain = true)
@Entity
@Table(name = "tb_user")
public class User {
    @Id
    @Column
    private String id;
    @Column
    private String password;
    @Column
    private String name;
}
