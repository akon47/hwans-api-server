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
- `data.sql`이 로컬에서 시드 데이터를 넣는다.

## 배포 (중요)
- **`master` 브랜치 푸시 = 운영 자동 배포.** Jenkins가 푸시를 감지해 빌드 → Docker 이미지 → 운영 서버(10.10.10.100)에 자동 배포한다.
- CI 빌드는 테스트를 건너뛰므로(`-x test`), 푸시 전 로컬에서 빌드가 통과하는지 반드시 확인한다.
- 문서(이 파일 포함)만 바꿔 푸시해도 운영 재배포가 트리거되니, 가급적 실제 변경과 함께 푸시한다.

## 구조 메모
- 컨트롤러 엔드포인트 prefix는 `/api` (`Constants.API_PREFIX`). 보안 설정상 `GET /api/v1/blog/**` 는 인증 없이 허용된다.
  - 단, 사용자별 GET(예: `.../likes`, `.../bookmarks`, `/v1/blog/me/**`)은 `@CurrentAuthenticationDetails`(필수)를 쓰므로 `WebSecurityConfig`에서 `blog/**` permitAll보다 **먼저** `authenticated()` 매처를 명시해야 한다(순서 중요).
- 커서 페이징 응답은 `SliceDto.of(found, size, first, mapper, idExtractor)` 정적 팩토리로 생성한다.
- 리포지토리 `@Query`의 명명 파라미터는 JPQL 이름과 **Java 파라미터명**이 일치해야 한다(`-parameters` 컴파일로 바인딩됨). `@Param` 임포트는 lettuce/spring 혼재.
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
- 웹소켓 세션 자체는 클라이언트 IP/User-Agent를 보관하지 않으므로 `ClientInfoHandshakeInterceptor`가 핸드셰이크에서 `X-Forwarded-For`(없으면 `X-Real-IP`/원격주소)와 User-Agent를 세션 attributes에 저장한다(`server.forward-headers-strategy: native` 덕에 프록시 뒤에서도 실제 IP). 어드민 실시간 접속자(`GET /v1/admin/active-viewers`, hasRole ADMIN)는 `WebSocketService.getActiveViewerPresences()`로 현재 시청 세션을 받아 `blogService.getPostsByIds`(공개 글만) + 회원 이름과 조인해 반환한다. IP는 민감정보라 공개 `/ws`가 아닌 admin REST로만 노출.
