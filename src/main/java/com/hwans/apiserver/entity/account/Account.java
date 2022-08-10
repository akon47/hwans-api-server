package com.hwans.apiserver.entity.account;

import com.hwans.apiserver.entity.BaseEntity;
import com.hwans.apiserver.entity.account.role.AccountRole;
import com.hwans.apiserver.entity.account.role.Role;
import com.hwans.apiserver.entity.blog.Comment;
import com.hwans.apiserver.entity.blog.Like;
import com.hwans.apiserver.entity.blog.Post;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Entity
@Table(name = "tb_account")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Account extends BaseEntity {
    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;
    @Column(length = 320, unique = true, nullable = false)
    private String email;
    @Column(length = 64)
    private String password;
    @Column(length = 32)
    private String name;
    @Column(length = 64, unique = true, nullable = false)
    private String blogId;
    @Column(nullable = false)
    private boolean deleted;
    @Column(length = 255)
    @With
    private String refreshToken;
    @OneToMany(mappedBy = "account", cascade = CascadeType.PERSIST)
    @Getter(AccessLevel.NONE)
    private final Set<AccountRole> accountRoles = new HashSet<>();
    @OneToMany(mappedBy = "account")
    private final Set<Post> posts = new HashSet<>();
    @OneToMany(mappedBy = "account")
    private final Set<Comment> comments = new HashSet<>();
    @OneToMany(mappedBy = "account")
    private final Set<Like> likes = new HashSet<>();

    public Set<Role> getRoles() {
        return accountRoles.stream().map(x -> x.getRole()).collect(Collectors.toSet());
    }

    public void addRole(Role role) {
        if (accountRoles.stream().anyMatch(x -> x.getRole().getName().equals(role)) == false) {
            accountRoles.add(AccountRole.builder().account(this).role(role).build());
        }
    }

    public void clearRefreshToken() {
        this.refreshToken = null;
    }

    public boolean validateRefreshToken(String refreshToken) {
        if (this.refreshToken == null)
            return true;

        return this.refreshToken.equals(refreshToken);
    }
}