package com.hwans.apiserver.service.blog;

import com.hwans.apiserver.dto.blog.*;
import com.hwans.apiserver.dto.common.SliceDto;

import java.util.Optional;
import java.util.UUID;

/**
 * 블로그 서비스 인터페이스
 */
public interface BlogService {
    /**
     * 블로그 Id를 이용하여 해당 블로그의 상세 정보를 조회합니다.
     *
     * @param blogId 블로그 Id
     * @return 블로그 상세정보
     */
    BlogDetailsDto getBlogDetails(String blogId, boolean findPublicPostOnly);

    /**
     * 모든 블로거의 Public 상태의 게시글을 조회한다.
     *
     * @param search 조회 시 사용할 search keyword
     * @param cursorId 페이징 조회를 위한 기준 cursorId
     * @param size 조회를 원하는 최대 size
     * @return 조회된 게시글 목록 (페이징)
     */
    SliceDto<SimplePostDto> getAllPosts(String search, Optional<UUID> cursorId, int size);

    /**
     * 게시글을 생성(작성)합니다.
     *
     * @param authorAccountId 게시글 작성자 계정 Id
     * @param postRequestDto 생성(작성)할 게시글의 데이터 모델
     * @return 생성(작성)된 게시글의 데이터 모델
     */
    PostDto createPost(UUID authorAccountId, PostRequestDto postRequestDto);

    /**
     * 게시글을 수정합니다.
     *
     * @param blogId 수정할 게시글의 blogId
     * @param postUrl 수정할 게시글의 postUrl
     * @param postRequestDto 수정할 게시글의 데이터 모델
     * @return 수정된 게시글의 데이터 모델
     */
    PostDto modifyPost(String blogId, String postUrl, PostRequestDto postRequestDto);

    /**
     * 게시글을 삭제합니다.
     *
     * @param blogId 삭제할 게시글의 blogId
     * @param postUrl 삭제할 게시글의 postUrl
     */
    void deletePost(String blogId, String postUrl);

    /**
     * 해당 blogId의 주인이 쓴 게시글을 조회합니다.
     *
     * @param blogId 조회할 대상의 blogId
     * @param cursorId 페이징 조회를 위한 기준 cursorId
     * @param size 조회를 원하는 최대 size
     * @param findPublicPostOnly Public 게시글만 조회할지 여부
     * @return 조회된 게시글 목록 (페이징)
     */
    SliceDto<SimplePostDto> getBlogPosts(String blogId, String tag, Optional<UUID> cursorId, int size, boolean findPublicPostOnly);

    /**
     * 해당 blogId의 주인이 좋아요 한 게시글을 조회합니다.
     *
     * @param blogId 조회할 대상의 blogId
     * @param cursorId 페이징 조회를 위한 기준 cursorId
     * @param size 조회를 원하는 최대 size
     * @return 조회된 게시글 목록 (페이징)
     */
    SliceDto<SimplePostDto> getBloggerLikePosts(String blogId, Optional<UUID> cursorId, int size);

    /**
     * 게시글을 조회합니다.
     *
     * @param blogId  조회를 원하는 게시글의 blogId
     * @param postUrl 조회를 원하는 게시글의 postUrl
     * @return 게시글 데이터 모델
     */
    PostDto getPost(String blogId, String postUrl);

    /**
     * 게시글 작성자의 Id를 조회합니다.
     *
     * @param postId 게시글 Id
     * @return 작성자 Id
     */
    UUID getPostAuthorId(UUID postId);

    /**
     * 게시글 작성자의 Id를 조회합니다.
     *
     * @param blogId 조회를 원하는 게시글의 blogId
     * @param postUrl 조회를 원하는 게시글의 postUrl
     * @return 작성자 Id
     */
    UUID getPostAuthorId(String blogId, String postUrl);

    /**
     * 게시글에 좋아요를 합니다.
     *
     * @param actorAccountId 좋아요를 원하는 대상 계정 Id
     * @param blogId 좋아요를 원하는 대상 blogId
     * @param postUrl 좋아요를 원하는 대상 postUrl
     */
    void likePost(UUID actorAccountId, String blogId, String postUrl);

    /**
     * 게시글의 좋아요를 취소합니다.
     *
     * @param actorAccountId 좋아요 취소를 원하는 대상 계정 Id
     * @param blogId 좋아요 취소를 원하는 대상 blogId
     * @param postUrl 좋아요 취소를 원하는 대상 postUrl
     */
    void unlikePost(UUID actorAccountId, String blogId, String postUrl);

    /**
     * 현재 해당 게시글을 좋아요 했는지 여부를 반환한다.
     *
     * @param accountId 좋아요 여부 확인을 위한 대상 계정 Id
     * @param blogId 좋아요 여부 확인을 위한 대상 blogId
     * @param postUrl 좋아요 여부 확인을 위한 대상 postUrl
     * @return 좋아요 여부
     */
    boolean isLikePost(UUID accountId, String blogId, String postUrl);

    /**
     * 댓글을 생성합니다
     *
     * @param authorAccountId 댓글 작성자 계정 Id
     * @param blogId 댓글을 달려고 하는 blogId
     * @param postUrl 댓글을 달려고 하는 postUrl
     * @param commentRequestDto 생성할 댓글 데이터 모델
     * @return 생성된 댓글 데이터 모델
     */
    CommentDto createComment(UUID authorAccountId, String blogId, String postUrl, CommentRequestDto commentRequestDto);

    /**
     * 비회원 댓글을 생성합니다
     *
     * @param blogId 댓글을 달려고 하는 blogId
     * @param postUrl 댓글을 달려고 하는 postUrl
     * @param guestCommentRequestDto 생성할 비회원 댓글 데이터 모델
     * @return 생성된 댓글 데이터 모델
     */
    CommentDto createGuestComment(String blogId, String postUrl, GuestCommentRequestDto guestCommentRequestDto);

    /**
     * 대댓글을 생성합니다.
     *
     * @param authorAccountId 댓글 작성자 계정 Id
     * @param commentId 대댓글 작성을 위한 부모 댓글 Id
     * @param commentRequestDto 생성할 댓글 데이터 모델
     * @return 생성된 댓글 데이터 모델
     */
    CommentDto createComment(UUID authorAccountId, UUID commentId, CommentRequestDto commentRequestDto);

    /**
     * 비회원 대댓글을 생성합니다.
     *
     * @param commentId 대댓글 작성을 위한 부모 댓글 Id
     * @param guestCommentRequestDto 생성할 비회원 댓글 데이터 모델
     * @return 생성된 댓글 데이터 모델
     */
    CommentDto createGuestComment(UUID commentId, GuestCommentRequestDto guestCommentRequestDto);

    /**
     * 댓글을 수정합니다
     *
     * @param commentId 댓글 Id
     * @param commentRequestDto 수정을 위한 댓글 요청 데이터 모델
     * @return 수정된 댓글 데이터 모델
     */
    CommentDto modifyComment(UUID commentId, CommentRequestDto commentRequestDto);

    /**
     * 댓글을 조회합니다.
     *
     * @param commentId 댓글 Id
     * @return 조회된 댓글 데이터 모델
     */
    CommentDto getComment(UUID commentId);

    /**
     * 댓글을 삭제합니다.
     *
     * @param commentId 댓글 Id
     */
    void deleteComment(UUID commentId);

    /**
     * 댓글 작성자의 Id를 조회합니다.
     *
     * @param commentId 댓글 Id
     * @return 작성자 Id
     */
    UUID getCommentAuthorId(UUID commentId);

    /**
     * 댓글 작성자의 비밀번호가 맞는지 검사합니다.
     *
     * @param commentId 댓글 Id
     * @param password 댓글 작성자 비밀번호
     * @return 일치하는지 여부
     */
    boolean matchCommentAuthorPassword(UUID commentId, String password);

    /**
     * 현재 캐시되어 있는 게시글 조회수를 DB에 반영합니다.
     */
    void updatePostHitsFromCache();
}
