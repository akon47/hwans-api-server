package com.hwans.apiserver.repository.account;

import com.hwans.apiserver.entity.account.Account;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
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
     * 이메일 목록에 해당하는 삭제되지 않은 계정들을 한 번에 조회한다. (N+1 방지)
     *
     * @param emails 조회할 이메일 목록
     * @return 찾은 계정 엔티티 목록
     */
    List<Account> findAllByEmailInAndDeletedIsFalse(Collection<String> emails);

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

    // === 관리자용 조회 (손님 계정 제외, 정지된 계정 포함) ===

    @Query("select a from Account as a where not exists (select ar from AccountRole as ar where ar.account = a and ar.role.name = 'ROLE_GUEST') and (:search is null or a.email like concat('%',:search,'%') or a.name like concat('%',:search,'%') or a.blogId like concat('%',:search,'%')) order by a.createdAt desc, a.id desc")
    List<Account> findAllForAdmin(@Param("search") String search, Pageable page);

    @Query("select a from Account as a where not exists (select ar from AccountRole as ar where ar.account = a and ar.role.name = 'ROLE_GUEST') and (:search is null or a.email like concat('%',:search,'%') or a.name like concat('%',:search,'%') or a.blogId like concat('%',:search,'%')) and ((a.createdAt < :createdAt and a.id < :id) or (a.createdAt < :createdAt)) order by a.createdAt desc, a.id desc")
    List<Account> findAllForAdminByCursor(@Param("search") String search, @Param("id") UUID id, @Param("createdAt") LocalDateTime createdAt, Pageable page);

    // 손님이 아닌 활성 회원 수
    @Query("select count(a) from Account as a where a.deleted = false and not exists (select ar from AccountRole as ar where ar.account = a and ar.role.name = 'ROLE_GUEST')")
    long countActiveMembers();
}
