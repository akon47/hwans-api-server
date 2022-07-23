package com.hwans.apiserver.repository.role;

import com.hwans.apiserver.entity.account.role.Role;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.Optional;

@Repository
public class RoleRepositorySupportImpl implements RoleRepositorySupport {
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Role saveIfNotExist(String name) {
        return Optional.ofNullable(entityManager.find(Role.class, name))
                .orElseGet(() ->
                {
                    var role = new Role(name);
                    entityManager.persist(role);
                    return role;
                });
    }
}
