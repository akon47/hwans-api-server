package com.hwans.apiserver.repository.blog;

import com.hwans.apiserver.entity.blog.Post;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PostRepository extends JpaRepository<Post, UUID> {
    @Query("select x from Post as x where x.account.blogId = :blogId and x.postUrl = :postUrl")
    Optional<Post> findByBlogIdAndPostUrl(String blogId, String postUrl);

    @Query("select x from Post as x where x.account.blogId = :blogId and x.postUrl = :postUrl and x.deleted = false")
    Optional<Post> findByBlogIdAndPostUrlAndDeletedIsFalse(String blogId, String postUrl);

    @Query("select x from Post as x where x.deleted = false and x.openType = 'PUBLIC' order by x.createdAt desc, x.id desc")
    List<Post> findAllByOrderByIdDesc(Pageable page);

    @Query("select x from Post as x where x.deleted = false and x.openType = 'PUBLIC' and ((x.createdAt < :createdAt and x.id < :id) or (x.createdAt < :createdAt)) order by x.createdAt desc, x.id desc")
    List<Post> findByIdLessThanOrderByIdDesc(@Param("uuid") UUID id, @Param("createdAt") LocalDateTime createdAt, Pageable page);

    @Query("select distinct post from Post as post left outer join post.postTags as postTag left outer join postTag.tag as tag where post.deleted = false and (:findPublicPostOnly is false or post.openType = 'PUBLIC') and post.account.blogId = :blogId and (:tag is null or tag.name = :tag) order by post.createdAt desc, post.id desc")
    List<Post> findAllByOrderByIdDesc(@Param("blogId") String blogId, @Param("tag") String tag, @Param("findPublicPostOnly") boolean findPublicPostOnly, Pageable page);

    @Query("select distinct post from Post as post left outer join post.postTags as postTag left outer join postTag.tag as tag where post.deleted = false and (:findPublicPostOnly is false or post.openType = 'PUBLIC') and post.account.blogId = :blogId and (:tag is null or tag.name = :tag) and ((post.createdAt < :createdAt and post.id < :id) or (post.createdAt < :createdAt)) order by post.createdAt desc, post.id desc")
    List<Post> findByIdLessThanOrderByIdDesc(@Param("blogId") String blogId, @Param("tag") String tag, @Param("uuid") UUID id, @Param("createdAt") LocalDateTime createdAt, @Param("findPublicPostOnly") boolean findPublicPostOnly, Pageable page);

    @Query("select x from Post as x where x.deleted = false and x.openType = 'PUBLIC' and (x.title like concat('%',:search,'%') or x.content like concat('%',:search,'%')) order by x.createdAt desc, x.id desc")
    List<Post> findAllByOrderByIdDesc(@Param("search") String search, Pageable page);

    @Query("select x from Post as x where x.deleted = false and x.openType = 'PUBLIC' and ((x.createdAt < :createdAt and x.id < :id) or (x.createdAt < :createdAt)) and (x.title like concat('%',:search,'%') or x.content like concat('%',:search,'%')) order by x.createdAt desc, x.id desc")
    List<Post> findByIdLessThanOrderByIdDesc(@Param("uuid") UUID id, @Param("createdAt") LocalDateTime createdAt, @Param("search") String search, Pageable page);

    @Query("select count(*) from Post as x where x.deleted = false and (:findPublicPostOnly is false or x.openType = 'PUBLIC') and x.account.blogId = :blogId")
    int getCountByBlogId(@Param("blogId") String blogId, @Param("findPublicPostOnly") boolean findPublicPostOnly);

    @Query("select x from Post as x where x.deleted = false and (:findPublicPostOnly is false or x.openType = 'PUBLIC') and x.account.blogId = :blogId order by x.createdAt desc, x.id desc")
    List<Post> findAllByBlogId(@Param("blogId") String blogId, @Param("findPublicPostOnly") boolean findPublicPostOnly);

    @Query("select post from Post as post where post.deleted = false and (:findPublicPostOnly is false or post.openType = 'PUBLIC') and post.account.blogId = :blogId and (post.postSeries.series.seriesUrl = :seriesUrl) order by post.createdAt asc")
    List<Post> findByBlogIdAndSeriesUrl(@Param("blogId") String blogId, @Param("seriesUrl") String seriesUrl, @Param("findPublicPostOnly") boolean findPublicPostOnly);

    @Modifying
    @Query("update Post x set x.hits = :hits where x.id = :id")
    Integer updateHits(@Param("uuid") UUID id, @Param("hits") Integer hits);
}
