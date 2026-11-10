# hwans-api-server

Hwan'Story 블로그 서비스의 백엔드 API 서버. (프론트엔드는 별도 저장소 `hwanstory`)

## 기술 스택
- Spring Boot 3.5.x / Java 21 / Gradle 8.14 (2026-11-06 에 2.7.1 / Java 17 / Gradle 7.4.1 에서 올림)
- JPA(Hibernate 6) + MySQL(운영) / H2(로컬), Redis, Spring Security 6 + JWT(JJWT 0.13) + OAuth2, WebSocket(SockJS)
- MapStruct(매퍼), Lombok, springdoc-openapi(Swagger UI `/swagger-ui/index.html`, 명세 `/v3/api-docs`)
- 패키지는 `jakarta.*`(javax 아님). 문서 애너테이션은 `io.swagger.v3.oas.annotations.*`(`@Tag`, `@Operation`, `@Parameter`, `@Schema`, `@Hidden`).

## 커밋 규칙
- 커밋 작성자는 반드시 `Kim, Hwan <akon47@naver.com>` 로 한다.
  (기본 git 설정과 다를 수 있으니 `git commit --author="Kim, Hwan <akon47@naver.com>"` 또는 repo-local `git config user.name/user.email` 로 지정)

## 빌드
- JDK 는 **Gradle 툴체인(21)** 으로 고정된다(`build.gradle`). 로컬·Jenkins 에 21 이 없으면 `settings.gradle` 의 foojay 플러그인이 내려받으므로, Gradle 을 돌리는 JDK 는 17 이상이면 된다(Jenkins 는 `JAVA 17` 툴로 Gradle 을 돌린다).
- 빌드: `./gradlew clean build -x test` (Jenkins CI와 동일하게 테스트는 건너뛴다)
- Docker 기반 이미지는 `eclipse-temurin:21-jre`(`openjdk:*` 공식 이미지는 Docker Hub 에서 사라졌다).
- 운영 VM 의 `application-secret.yml` 은 아직 Boot 2 시절 키(`spring.redis.*`)를 쓸 수 있어 `spring-boot-properties-migrator` 를 runtime 에 넣어 두었다. 기동 로그의 migrator 경고를 보고 그 파일을 새 키로 고친 뒤 의존성을 빼면 된다.

## 로컬 실행
- 로컬 프로파일은 H2(인메모리) + 임베디드 Redis를 사용한다: `SPRING_PROFILES_ACTIVE=local`
- 로컬 기동 시 `SPRING_MAIL_HOST` 환경변수가 있어야 `JavaMailSender` 빈 누락 오류 없이 시작된다.
- **메일 발송은 Resend HTTP API**(`ResendMailClient`, `mail.resend.api-key`/`mail.resend.from`). 키는 운영 VM `/home/kimhwan/blog-config/application-secret.yml` 의 `mail.resend.api-key` 로 주고(환경변수 `RESEND_API_KEY` 도 됨), 비어 있으면 예전 `spring.mail` SMTP 로 보낸다(자체 메일 VM 103 은 제거 예정). 보내는 주소는 `noreply@hwanstory.kr`(Resend 에 hwanstory.kr 도메인이 인증돼 있고 Cloudflare 에 `resend._domainkey` TXT·`send`/`rsend` CNAME 이 있다). 운영 API 키는 hwanstory.kr 발송 전용(sending-only, 도메인 제한)이라 다른 도메인으로 보내면 403 이 난다.
- `data.sql`이 로컬에서 시드 데이터를 넣는다.

## 배포 (중요)
- **`master` 브랜치 푸시 = 운영 자동 배포.** Jenkins가 푸시를 감지해 빌드 → Docker 이미지 → 운영 서버(10.10.10.100)에 자동 배포한다.
- CI 빌드는 테스트를 건너뛰므로(`-x test`), 푸시 전 로컬에서 빌드가 통과하는지 반드시 확인한다.
- 문서(이 파일 포함)만 바꿔 푸시해도 운영 재배포가 트리거되니, 가급적 실제 변경과 함께 푸시한다.

## 구조 메모
- 컨트롤러 엔드포인트 prefix는 `/api` (`Constants.API_PREFIX`). 보안 설정상 `GET /api/v1/blog/**` 는 인증 없이 허용된다.
  - 단, 사용자별 GET(예: `.../likes`, `.../bookmarks`, `/v1/blog/me/**`)은 `@CurrentAuthenticationDetails`(필수)를 쓰므로 `WebSecurityConfig`에서 `blog/**` permitAll보다 **먼저** `authenticated()` 매처를 명시해야 한다(순서 중요).
- 커서 페이징 응답은 `SliceDto.of(found, size, first, mapper, idExtractor)` 정적 팩토리로 생성한다.
- `@GeneratedValue` 가 붙은 id 는 엔티티를 만들 때 직접 채우지 않는다. Hibernate 6 는 id 가 있는 새 엔티티를 분리된(detached) 것으로 보고 `save()` 에서 merge 를 시도해 `StaleObjectStateException`(Row was updated or deleted…)을 던진다. 손님 댓글의 `Account.createGuestAccount` 가 id 를 미리 넣어 Boot 3.5 전환 뒤 손님 댓글이 500 을 내던 것을 2026-11-06 에 고쳤다.
- 리포지토리 `@Query`의 명명 파라미터: `@Param("x")`가 있으면 그 이름이, 없으면 **Java 파라미터명**이 JPQL `:x`와 일치해야 한다(`-parameters` 컴파일). `@Param`은 반드시 `org.springframework.data.repository.query.Param` 이다 — 예전에 lettuce 의 `@Param`을 잘못 import 해서 애너테이션이 무시되고 Java 이름으로 바인딩되던 것을 2026-11-06 Boot 3 전환 때 바로잡았고, 그때 드러난 `@Param("uuid")` 오타 5곳도 고쳤다. 새 쿼리를 추가하면 `scratchpad/check_params.py` 같은 식으로 JPQL 이름과 바인딩 이름을 대조한다.
- RSS 피드는 `/rss`, `/rss/{blogId}`, sitemap은 `/sitemap.xml` (모두 인증 불필요, `WebSecurityConfig`에 permitAll 등록). 링크 생성용 프론트 주소는 `blog.web-base-url` 설정값.
- 게시글 공개유형 `OpenType`: `PUBLIC/PRIVATE/DRAFT/SCHEDULED`. 공개 목록 쿼리는 `openType='PUBLIC'`만 노출. 예약 발행은 `Post.scheduledAt` + `PublishScheduler`(매분)가 시각 도달 시 `publish()`로 PUBLIC 전환.
- 이모지 반응(`PostReaction`)은 DB에 이모지 문자가 아니라 **ASCII 키**(thumbsup 등)로 저장한다(utf8mb4 미보장 대비). 이모지↔키 매핑은 `BlogServiceImpl.EMOJI_TO_KEY`.
- 추가된 소셜 기능: 북마크(`Bookmark`), 댓글 좋아요(`CommentLike`), 작성자 통계(`/v1/blog/me/statistics`). 모두 `Like` 패턴을 따른다.
- 신규 엔티티는 운영 `ddl-auto: update`로 테이블 자동 생성됨. 기존 테이블엔 nullable 컬럼만 추가할 것(NOT NULL 추가 금지).
- **AI 에이전트용 개인 액세스 토큰(PAT)**: `hsp_<16hex id>_<43자 비밀>` 형식, SHA-256만 저장(`tb_access_token`). 발급 경로는 웹(`POST /v1/tokens`, JWT 필요)과 **기기 로그인**(RFC 8628 형태, `tb_device_authorization`): 에이전트 `POST /v1/authentication/device/code` → 사람이 프론트 `/device?code=`에서 승인(`POST …/device/{userCode}/approve`) → 에이전트 `POST …/device/token` 폴링. 둘 다 `WebSecurityConfig`에 permitAll 등록, 코드 발급은 IP당 분당 10회(`RedisRateLimiter`).
  - 인증은 `AccessTokenAuthenticationFilter`(JwtFilter 앞에 등록, `JwtFilter`는 `hsp_` 토큰을 건너뜀)가 처리하고, 토큰으로 할 수 있는 요청은 `AccessTokenAccessRules`가 **메서드+경로 → ALLOW/DENY/필요 scope**로 정한다(읽기는 모두 허용, `posts`=게시글·시리즈·첨부, `social`=댓글·좋아요·북마크·반응·팔로우·알림, 계정·토큰·인증·관리자 API는 DENY). 토큰 인증 시 `ROLE_ADMIN`은 제외되고, `Authentication.getDetails()`에 `AccessTokenDetails`가 들어간다(`/v1/tokens/current`가 사용).
  - 에이전트 가이드는 `resources/docs/agents.md`(`GET /agents.md`), `llms.txt`, 도우미 `resources/cli/login.sh|ps1`(`GET /cli/**`)이며 `AgentDocsService`가 `{{SITE_URL}}`/`{{API_URL}}`(`blog.web-base-url`/`blog.api-base-url`)을 채운다. API 경로·필드를 바꾸면 **agents.md도 같이 고친다**.
  - `LoggingFilter`는 `Authorization`/`X-Auth-Refresh-Token`/쿠키 헤더를 마스킹하고 `/v1/authentication/**`, `/v1/tokens*`, `/v1/accounts/password`의 요청·응답 본문을 남기지 않는다(토큰·비밀번호 유출 방지).
  - 계정 삭제 시 토큰·기기 로그인 요청도 함께 지운다(`AccountServiceImpl.deleteAccount`, FK는 `@OnDelete(CASCADE)`).
- **검색엔진용 서버 렌더(dynamic rendering)**: `GET /prerender/<SPA 경로>`(인증 불필요, `PrerenderController`/`PrerenderService`)가 홈·`/top`·`/agents`·`/@blog`·`/@blog/posts[?tag=]`·`/@blog/posts/{postUrl}`에 대해 본문(markdown→HTML: commonmark + jsoup 정화 `MarkdownRenderer`)·canonical·OG·JSON-LD가 든 HTML을 만든다. 비공개 글·없는 경로는 **진짜 404**. 프론트 nginx가 크롤러 UA의 페이지 요청만 여기로 프록시한다(사람은 SPA). `/sitemap.xml`·`/rss`도 프론트가 이 서버로 프록시하므로 VM의 별도 SEO 생성 스크립트는 필요 없다.
- **관리자 API**(`AdminController`, ROLE_ADMIN): 대시보드 `GET /v1/admin/dashboard`(요약·최근 30일 일별 집계·최근 활동; 일별 집계는 createdAt 목록을 가져와 Java 에서 날짜별로 센다), `GET /v1/admin/system`(버전은 `springBoot { buildInfo() }` 의 BuildProperties, 힙·가동 시간·DB/Redis ping·첨부 디스크 여유·예약 글·대기 기기 로그인·살아있는 토큰 수), 글 `GET /v1/admin/posts?search&openType&page&size`(페이지 번호 기반 `PageDto`)·`PUT …/{id}/open-type`·`DELETE …/{id}`, 댓글 `GET /v1/admin/comments`·`DELETE`, 토큰 `GET /v1/admin/tokens`·`DELETE …/{id}`, 권한 `PUT /v1/admin/accounts/{id}/roles {admin}`(자기 자신 회수 불가; `Account.removeRole` 은 컬렉션만 빼므로 `AccountRoleRepository` 로 행을 지운다).
- **운영 기능(2026-11-10)**: `AdminAuditService.record(action, targetType, targetId, summary)` 가 관리자의 모든 변경(회원 정지·해제, 권한, 글 공개 유형·삭제, 댓글 삭제, 토큰 폐기, 공지, 신고 처리)을 `tb_admin_audit_log` 에 남긴다(`GET /v1/admin/audit-logs`). 새 관리자 변경 기능을 만들면 **반드시 record 를 부른다**. `SiteService`: 공지는 `tb_site_setting`(key=`announcement`, 값은 JSON) — 공개 `GET /v1/site/announcement`, 관리자 `GET/PUT /v1/admin/announcement`(링크는 `/경로` 또는 `https://` 만). 신고는 `tb_report` — 공개 `POST /v1/site/reports`(손님 가능, IP 당 분당 5회, 같은 IP·대상의 대기 중 신고는 중복 생성 안 함, 신고 시점의 제목/댓글 앞부분과 경로를 함께 저장), 관리자 `GET /v1/admin/reports?status`, `PUT /v1/admin/reports/{id} {status, deleteTarget}`(deleteTarget 이면 대상 삭제 + 같은 대상의 다른 대기 신고도 처리 완료). 두 공개 경로는 `WebSecurityConfig` 에 permitAll 로 등록.
- **운영 기능 2차(2026-11-10)**: ① 차단 — `ModerationService`(IP 는 `tb_blocked_ip`, 금칙어는 `tb_site_setting` 의 `banned-words` JSON, 30초 메모리 캐시·변경 시 즉시 무효화). `assertNotBlocked(ip)` 는 손님 댓글·신고에서, `assertCleanText(...)` 는 모든 댓글 작성·수정(손님 이름 포함)에서 `BlogController`/`SiteController` 가 부른다(대소문자·공백 무시, 오류 코드 `Forbidden.BLOCKED_IP`/`BadRequest.BANNED_WORD`). 손님 댓글은 작성 IP 를 `tb_comment.author_ip` 에 남긴다(`blogService.setCommentAuthorIp`, 관리자 댓글 Dto 로만 노출). 관리자 API: `GET/POST /v1/admin/blocked-ips`, `DELETE …/{id}`, `GET/PUT /v1/admin/banned-words`. ② 통계 — `StatsService`: 글 조회마다 `tb_post_daily_view`(글·날짜별 조회수), 첫 페이지 검색마다 `tb_search_keyword`(검색어별 횟수, 누가 검색했는지는 남기지 않음)를 올린다. 기록은 `StatsRecorder` 의 REQUIRES_NEW 트랜잭션에서 하고 실패해도 본 요청에는 영향이 없다. 작성자용 `GET /v1/blog/me/statistics/posts?days`(WebSecurityConfig 에 authenticated 로 등록), 관리자용 `GET /v1/admin/insights?days`. ③ 회원 상세 `GET /v1/admin/accounts/{id}/detail`(신고된 횟수는 `tb_report.target_author_email` 로 센다). ④ 언급 알림 — `NotificationService.createMentionNotifications(comment)` 가 댓글의 `@블로그Id`(댓글당 10명까지)마다 `MentionNotification`(`tb_mention_notification`, 댓글과 다대일)을 만든다. 본인과, 글에 바로 단 댓글에서의 글 작성자(이미 댓글 알림을 받는다)는 건너뛴다.
- **함께 읽는 사람 커서**(`WebSocketServiceImpl`): 클라이언트 `CURSOR_MOVE {x,y,name,color}`(세션당 40ms 미만은 버림)·`CURSOR_CHAT {message}`(150ms) 를 글 Id 별 맵에 두고 `CursorFlushScheduler`(100ms)가 `flushCursors()` 로 그 글을 보는 세션들에게 `CURSOR_BATCH {postId, cursors(최대 40, 최근 이동 순), left, disabled}` 를 한 번에 보낸다. 읽는 사람이 120명을 넘는 글은 커서를 받지 않고 보낸 사람에게만 `disabled` 를 한 번 알린다(방송 안 함). 90초 안 움직인 커서는 제거. 첫 커서 때 `CURSOR_SELF` 로 세션 Id 를 알려 준다. 아무것도 저장하지 않는다.
- **함께 읽는 사람 목록**(`WebSocketServiceImpl`): `VIEW_POST` payload 는 글 Id 문자열(예전 클라이언트, 이름은 "익명") 또는 `{postId, name, color}`. 세션의 이름·색·들어온 시각을 `sessionViewerIdentity` 에 글을 보는 동안만 두고, 누가 들어오거나 나갈 때마다(이름·색만 바뀌어도) 그 글을 읽는 세션들에게만 `POST_VIEWERS {postId, viewers(먼저 들어온 순, 최대 30), total}` 를 보낸다. 자기 자신을 알아보도록 첫 `VIEW_POST` 때 `CURSOR_SELF` 를 먼저 보낸다. 연결 직후에는 지금 읽히는 글들의 시청자 수 스냅샷(`POST_VIEWER_COUNTS`)도 한 번 보낸다.
- **읽는 위치**(`WebSocketServiceImpl`): 클라이언트 `READ_POSITION`(payload 숫자 0~1, 세션당 100ms 미만은 버림)을 `sessionReadPosition` 에 두고, `CursorFlushScheduler` 가 부르는 `flushReadPositions()` 가 300ms 간격으로 바뀐 글만 `READ_POSITIONS {postId, positions[{id, progress}]}`(읽는 사람 목록에 실리는 최대 30명분)로 그 글을 읽는 세션들에게 보낸다. 혼자 읽는 글과 120명을 넘는 글은 보내지 않는다. 누가 들어오면 그 글을 다음 flush 대상에 넣어 새로 온 사람이 기존 위치를 받게 한다.
- **글 화면 실시간 신호**(`WebSocketServiceImpl`, 아무것도 저장하지 않는다): `REACTION_BURST`(payload 이모지, `BlogServiceImpl.isAllowedReactionEmoji` 로 허용 목록 확인, 세션당 250ms) 는 같은 글을 읽는 모두에게 `{postId, emoji, id}` 로, `COMMENT_TYPING`(세션당 1초) 은 보낸 사람을 뺀 나머지에게 `{postId, id, name}` 으로 전한다. `notifyCommentsChanged(postId)` 는 `BlogController` 가 댓글 작성·수정·삭제 뒤에 불러 `COMMENTS_CHANGED {postId}` 를 보낸다(클라이언트는 `GET /v1/blog/{blogId}/posts/{postUrl}/comments` 로 댓글만 다시 받는다 — 이 API 는 조회수를 올리지 않는다). 공통 전송은 `sendToPostViewers`.
- `/@blog/cards` 는 프리렌더에서 글 목록과 같은 페이지(canonical 은 /posts), `/@blog/series[/x]` 는 noindex 스텁.
- 웹소켓 세션 자체는 클라이언트 IP/User-Agent를 보관하지 않으므로 `ClientInfoHandshakeInterceptor`가 핸드셰이크에서 `X-Forwarded-For`(없으면 `X-Real-IP`/원격주소)와 User-Agent를 세션 attributes에 저장한다(`server.forward-headers-strategy: native` 덕에 프록시 뒤에서도 실제 IP). 어드민 실시간 접속자(`GET /v1/admin/active-viewers`, hasRole ADMIN)는 `WebSocketService.getActiveViewerPresences()`로 현재 시청 세션을 받아 `blogService.getPostsByIds`(공개 글만) + 회원 이름과 조인해 반환한다. IP는 민감정보라 공개 `/ws`가 아닌 admin REST로만 노출.
