package com.hwans.apiserver.repository.account;

import com.hwans.apiserver.entity.account.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * 사용자 계정 Repository
 */
@Repository
public interface AccountRepository extends JpaRepository<Account, UUID>, AccountRepositorySupport {
    /**
     * 이메일로 가입되어 있는 계정이 있다면 반환한다.
     *
     * @param email 이메일
     * @return 찾은 경우 계정 엔티티
     */
    Optional<Account> findByEmailAndDeletedIsFalse(String email);

    /**
     * 삭제되지 않은 계정을 Id로 찾아 반환한다.
     *
     * @param id 계정 Id
     * @return 찾은 경우 계정 엔티티
     */
    Optional<Account> findByIdAndDeletedIsFalse(UUID id);

    /**
     * 이메일로 계정이 존재하는지 여부를 반환한다.
     *
     * @param email 이메일
     * @return 이미 해당 이메일로 계정이 생성되어 있는지 여부
     */
    boolean existsByEmail(String email);

    /**
     * 이메일로 탈퇴되어 있지 않은 계정이 존재하는지 여부를 반환한다.
     *
     * @param email 이메일
     * @return 이미 해당 이메일로 유효한 계정이 생성되어 있는지 여부
     */
    boolean existsByEmailAndDeletedIsFalse(String email);

    /**
     * BlogId가 존재하는지 여부를 반환한다.
     *
     * @param blogId BlogId
     * @return 해당 BlogId가 이미 존재하는지 여부
     */
    boolean existsByBlogId(String blogId);

    /**
     * BlogId의 주인 계정을 찾는다.
     *
     * @param blogId BlogId
     * @return 존재하는 경우 계정 엔티티
     */
    Optional<Account> findByBlogId(String blogId);
}
