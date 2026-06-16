package com.hwans.apiserver.controller;

import com.hwans.apiserver.common.Constants;
import com.hwans.apiserver.dto.account.SimpleAccountDto;
import com.hwans.apiserver.dto.blog.SimplePostDto;
import com.hwans.apiserver.dto.common.SliceDto;
import com.hwans.apiserver.service.authentication.CurrentAuthenticationDetails;
import com.hwans.apiserver.service.authentication.UserAuthenticationDetails;
import com.hwans.apiserver.service.follow.FollowService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@RestController
@Api(tags = "팔로우")
@RequestMapping(value = Constants.API_PREFIX)
@RequiredArgsConstructor
public class FollowController {
    private final FollowService followService;

    @ApiOperation(value = "팔로우 하기", notes = "대상 블로그(사용자)를 팔로우한다.", tags = "팔로우")
    @PostMapping(value = "/v1/follow/{blogId}")
    public void follow(@CurrentAuthenticationDetails UserAuthenticationDetails userAuthenticationDetails,
                       @ApiParam(value = "블로그 Id") @PathVariable String blogId) {
        followService.follow(userAuthenticationDetails.getId(), blogId);
    }

    @ApiOperation(value = "팔로우 취소", notes = "대상 블로그(사용자)의 팔로우를 취소한다.", tags = "팔로우")
    @DeleteMapping(value = "/v1/follow/{blogId}")
    public void unfollow(@CurrentAuthenticationDetails UserAuthenticationDetails userAuthenticationDetails,
                         @ApiParam(value = "블로그 Id") @PathVariable String blogId) {
        followService.unfollow(userAuthenticationDetails.getId(), blogId);
    }

    @ApiOperation(value = "팔로우 여부 조회", notes = "대상 블로그(사용자)를 팔로우 중인지 여부를 조회한다.", tags = "팔로우")
    @GetMapping(value = "/v1/follow/{blogId}")
    public ResponseEntity isFollowing(@CurrentAuthenticationDetails UserAuthenticationDetails userAuthenticationDetails,
                                      @ApiParam(value = "블로그 Id") @PathVariable String blogId) {
        var following = followService.isFollowing(userAuthenticationDetails.getId(), blogId);
        return following ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @ApiOperation(value = "팔로워 목록 조회", notes = "대상 블로그(사용자)를 팔로우하는 사용자 목록을 조회한다.", tags = "팔로우")
    @GetMapping(value = "/v1/blog/{blogId}/followers")
    public SliceDto<SimpleAccountDto> getFollowers(@ApiParam(value = "블로그 Id") @PathVariable String blogId,
                                                   @ApiParam(value = "페이징 조회를 위한 CursorId") @RequestParam(required = false) Optional<UUID> cursorId,
                                                   @ApiParam(value = "조회할 최대 페이지 수") @RequestParam(required = false, defaultValue = "20") int size) {
        return followService.getFollowers(blogId, cursorId, size);
    }

    @ApiOperation(value = "팔로잉 목록 조회", notes = "대상 블로그(사용자)가 팔로우하는 사용자 목록을 조회한다.", tags = "팔로우")
    @GetMapping(value = "/v1/blog/{blogId}/followings")
    public SliceDto<SimpleAccountDto> getFollowings(@ApiParam(value = "블로그 Id") @PathVariable String blogId,
                                                    @ApiParam(value = "페이징 조회를 위한 CursorId") @RequestParam(required = false) Optional<UUID> cursorId,
                                                    @ApiParam(value = "조회할 최대 페이지 수") @RequestParam(required = false, defaultValue = "20") int size) {
        return followService.getFollowings(blogId, cursorId, size);
    }

    @ApiOperation(value = "팔로잉 피드 조회", notes = "내가 팔로우하는 사용자들의 공개 게시글 피드를 조회한다.", tags = "팔로우")
    @GetMapping(value = "/v1/feed/following")
    public SliceDto<SimplePostDto> getFollowingPosts(@CurrentAuthenticationDetails UserAuthenticationDetails userAuthenticationDetails,
                                                     @ApiParam(value = "페이징 조회를 위한 CursorId") @RequestParam(required = false) Optional<UUID> cursorId,
                                                     @ApiParam(value = "조회할 최대 페이지 수") @RequestParam(required = false, defaultValue = "20") int size) {
        return followService.getFollowingPosts(userAuthenticationDetails.getId(), cursorId, size);
    }
}
