package com.hwans.apiserver.entity;

import javax.persistence.*;

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
