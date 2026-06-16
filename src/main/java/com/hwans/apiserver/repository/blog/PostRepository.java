package com.hwans.apiserver.repository.blog;

import com.hwans.apiserver.entity.blog.Post;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PostRepository extends JpaRepository<Post, UUID> {
    @Query("select x from Post as x where x.account.blogId = :blogId and x.postUrl = :postUrl")
    Optional<Post> findByBlogIdAndPostUrl(String blogId, String postUrl);

    @Query("select x from Post as x where x.account.blogId = :blogId and x.postUrl = :postUrl and x.deleted = false")
    Optional<Post> findByBlogIdAndPostUrlAndDeletedIsFalse(String blogId, String postUrl);

    @Query("select x from Post as x where x.deleted = false and x.openType = 'PUBLIC' order by x.createdAt desc, x.id desc")
    List<Post> findAllByOrderByCreatedAtDesc(Pageable page);

    @Query("select x from Post as x where x.deleted = false and x.openType = 'PUBLIC' order by x.hits desc, x.createdAt desc, x.id desc")
    List<Post> findAllByOrderByHitsDesc(Pageable page);

    @Query("select x from Post as x where x.deleted = false and x.openType = 'PUBLIC' and ((x.createdAt < :createdAt and x.id < :id) or (x.createdAt < :createdAt)) order by x.createdAt desc, x.id desc")
    List<Post> findByCursorLessThanOrderByCreatedAtDesc(@Param("id") UUID id, @Param("createdAt") LocalDateTime createdAt, Pageable page);

    @Query("select x from Post as x where x.deleted = false and x.openType = 'PUBLIC' and ((x.hits < :hits) or (x.hits = :hits and x.createdAt < :createdAt) or (x.hits = :hits and x.createdAt = :createdAt and x.id < :id)) order by x.hits desc, x.createdAt desc, x.id desc")
    List<Post> findByCursorLessThanOrderByHitsDesc(@Param("id") UUID id, @Param("createdAt") LocalDateTime createdAt, @Param("hits") Integer hits, Pageable page);

    @Query("select distinct post from Post as post left outer join post.postTags as postTag left outer join postTag.tag as tag where post.deleted = false and (:findPublicPostOnly is false or post.openType = 'PUBLIC') and post.account.blogId = :blogId and (:tag is null or tag.name = :tag) order by post.createdAt desc, post.id desc")
    List<Post> findAllByOrderByIdDesc(@Param("blogId") String blogId, @Param("tag") String tag, @Param("findPublicPostOnly") boolean findPublicPostOnly, Pageable page);

    @Query("select distinct post from Post as post left outer join post.postTags as postTag left outer join postTag.tag as tag where post.deleted = false and (:findPublicPostOnly is false or post.openType = 'PUBLIC') and post.account.blogId = :blogId and (:tag is null or tag.name = :tag) and ((post.createdAt < :createdAt and post.id < :id) or (post.createdAt < :createdAt)) order by post.createdAt desc, post.id desc")
    List<Post> findByIdLessThanOrderByIdDesc(@Param("blogId") String blogId, @Param("tag") String tag, @Param("id") UUID id, @Param("createdAt") LocalDateTime createdAt, @Param("findPublicPostOnly") boolean findPublicPostOnly, Pageable page);

    @Query("select x from Post as x where x.deleted = false and x.openType = 'PUBLIC' and (x.title like concat('%',:search,'%') or x.content like concat('%',:search,'%')) order by x.createdAt desc, x.id desc")
    List<Post> findAllByOrderByCreatedAtDesc(@Param("search") String search, Pageable page);

    @Query("select x from Post as x where x.deleted = false and x.openType = 'PUBLIC' and (x.title like concat('%',:search,'%') or x.content like concat('%',:search,'%')) order by x.hits desc, x.createdAt desc, x.id desc")
    List<Post> findAllByOrderByHitsDesc(@Param("search") String search, Pageable page);

    @Query("select x from Post as x where x.deleted = false and x.openType = 'PUBLIC' and ((x.createdAt < :createdAt and x.id < :id) or (x.createdAt < :createdAt)) and (x.title like concat('%',:search,'%') or x.content like concat('%',:search,'%')) order by x.createdAt desc, x.id desc")
    List<Post> findByCursorLessThanOrderByCreatedAtDesc(@Param("id") UUID id, @Param("createdAt") LocalDateTime createdAt, @Param("search") String search, Pageable page);

    @Query("select x from Post as x where x.deleted = false and x.openType = 'PUBLIC' and ((x.hits < :hits) or (x.hits = :hits and x.createdAt < :createdAt) or (x.hits = :hits and x.createdAt = :createdAt and x.id < :id)) and (x.title like concat('%',:search,'%') or x.content like concat('%',:search,'%')) order by x.hits desc, x.createdAt desc, x.id desc")
    List<Post> findByCursorLessThanOrderByHitsDesc(@Param("id") UUID id, @Param("createdAt") LocalDateTime createdAt, @Param("hits") Integer hits, @Param("search") String search, Pageable page);

    @Query("select count(*) from Post as x where x.deleted = false and (:findPublicPostOnly is false or x.openType = 'PUBLIC') and x.account.blogId = :blogId")
    int getCountByBlogId(@Param("blogId") String blogId, @Param("findPublicPostOnly") boolean findPublicPostOnly);

    @Query("select x from Post as x where x.deleted = false and (:findPublicPostOnly is false or x.openType = 'PUBLIC') and x.account.blogId = :blogId order by x.createdAt desc, x.id desc")
    List<Post> findAllByBlogId(@Param("blogId") String blogId, @Param("findPublicPostOnly") boolean findPublicPostOnly);

    @Query("select post from Post as post where post.deleted = false and (:findPublicPostOnly is false or post.openType = 'PUBLIC') and post.account.blogId = :blogId and (post.postSeries.series.seriesUrl = :seriesUrl) order by post.postSeries.createdAt")
    List<Post> findByBlogIdAndSeriesUrl(@Param("blogId") String blogId, @Param("seriesUrl") String seriesUrl, @Param("findPublicPostOnly") boolean findPublicPostOnly);

    @Query("select distinct post from Post as post left outer join post.postTags as postTag left outer join postTag.tag as tag where post.deleted = false and post.openType = 'PUBLIC' and post.id <> :postId and tag.name in :tagNames order by post.createdAt desc, post.id desc")
    List<Post> findRelatedPosts(@Param("postId") UUID postId, @Param("tagNames") Collection<String> tagNames, Pageable page);

    @Query("select post from Post as post where post.deleted = false and post.openType = 'PUBLIC' and post.id in :ids")
    List<Post> findPublicPostsByIds(@Param("ids") Collection<UUID> ids);

    // 특정 사용자가 팔로우하는 사람들의 공개 게시글 목록 (최신순)
    @Query("select x from Post as x where x.deleted = false and x.openType = 'PUBLIC' and x.account.id in (select f.following.id from Follow as f where f.follower.id = :followerId) order by x.createdAt desc, x.id desc")
    List<Post> findFollowingPostsByOrderByCreatedAtDesc(@Param("followerId") UUID followerId, Pageable page);

    @Query("select x from Post as x where x.deleted = false and x.openType = 'PUBLIC' and x.account.id in (select f.following.id from Follow as f where f.follower.id = :followerId) and ((x.createdAt < :createdAt and x.id < :id) or (x.createdAt < :createdAt)) order by x.createdAt desc, x.id desc")
    List<Post> findFollowingPostsByCursorLessThanOrderByCreatedAtDesc(@Param("followerId") UUID followerId, @Param("id") UUID id, @Param("createdAt") LocalDateTime createdAt, Pageable page);

    @Modifying
    @Query("update Post x set x.hits = :hits where x.id = :id")
    Integer updateHits(@Param("id") UUID id, @Param("hits") Integer hits);

    // 관리자 통계용: 삭제되지 않은 전체 게시글 수
    long countByDeletedIsFalse();

    // 예약 발행 대상: 발행 시각이 지난 SCHEDULED 게시글
    @Query("select x from Post as x where x.deleted = false and x.openType = 'SCHEDULED' and x.scheduledAt is not null and x.scheduledAt <= :now")
    List<Post> findScheduledPostsToPublish(@Param("now") LocalDateTime now);

    // 작성자 통계용
    @Query("select count(x) from Post as x where x.deleted = false and x.account.blogId = :blogId")
    long countAllByBlogId(@Param("blogId") String blogId);

    @Query("select count(x) from Post as x where x.deleted = false and x.openType = 'PUBLIC' and x.account.blogId = :blogId")
    long countPublicByBlogId(@Param("blogId") String blogId);

    @Query("select coalesce(sum(x.hits), 0) from Post as x where x.deleted = false and x.account.blogId = :blogId")
    long sumHitsByBlogId(@Param("blogId") String blogId);

    @Query("select x from Post as x where x.deleted = false and x.openType = 'PUBLIC' and x.account.blogId = :blogId order by x.hits desc, x.createdAt desc, x.id desc")
    List<Post> findTopByBlogIdOrderByHitsDesc(@Param("blogId") String blogId, Pageable page);
}
