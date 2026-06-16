# hwans-api-server

Hwan'Story 블로그 서비스의 백엔드 API 서버. (프론트엔드는 별도 저장소 `hwanstory`)

## 기술 스택
- Spring Boot 2.7.1 / Java 17 / Gradle 7.4.1
- JPA(Hibernate) + MySQL(운영) / H2(로컬), Redis, Spring Security + JWT + OAuth2, WebSocket(STOMP)
- MapStruct(매퍼), Lombok, Springfox Swagger

## 커밋 규칙
- 커밋 작성자는 반드시 `Kim, Hwan <akon47@naver.com>` 로 한다.
  (기본 git 설정과 다를 수 있으니 `git commit --author="Kim, Hwan <akon47@naver.com>"` 또는 repo-local `git config user.name/user.email` 로 지정)

## 빌드
- **JDK 17이 필요하다.** Gradle 7.4.1은 더 높은 JDK(예: 21)에서 실행되지 않는다.
- 빌드: `./gradlew clean build -x test` (Jenkins CI와 동일하게 테스트는 건너뛴다)

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
