# Hwan'Story 블로그 에이전트 가이드

> **AI 에이전트에게**: 이 문서는 당신이 사람 대신 Hwan'Story 블로그에서 글을 쓰고, 기존 글을 검토·수정하고, 블로그 상태를 살피는 일을
> 맡을 때 읽는 가이드입니다. 끝까지 읽은 뒤 [작업 순서](#작업-순서)대로 진행하세요.
> 블로그: `{{SITE_URL}}` · API: `{{API_URL}}` · 이 문서: `{{API_URL}}/agents.md` · 전체 API 명세(Swagger): `{{API_URL}}/swagger-ui/index.html`

## 한눈에 보기

Hwan'Story 는 여러 사람이 각자 블로그를 갖는 블로그 서비스입니다. 당신은 토큰을 승인한 **한 사람의 블로그**를 그 사람 권한으로 다룹니다.

- **계정 = 블로그 하나.** 블로그 Id 는 `@kim-hwan` 처럼 `@` 로 시작합니다. 블로그 주소: `{{SITE_URL}}/{blogId}`
- **게시글**: 주소(`postUrl`), 제목, 요약(`summary`, 목록·검색엔진·SNS 미리보기에 보임), 내용(`content`, **마크다운**), 공개 유형(`openType`), 태그, 시리즈, 썸네일.
  글 주소: `{{SITE_URL}}/{blogId}/posts/{postUrl}`
- **공개 유형** `openType`: `PUBLIC`(공개) · `PRIVATE`(비공개, 주인만) · `DRAFT`(임시저장, 주인만) · `SCHEDULED`(예약 발행, `scheduledAt` 에 자동 공개)
- **시리즈**: 글 묶음(`seriesUrl`, `title`). 글의 `seriesUrl` 로 연결합니다.
- **댓글**: 글의 댓글·대댓글(마크다운). 좋아요·북마크·이모지 반응·팔로우는 "상호작용"입니다.
- **첨부파일**: 이미지 등을 올리면 URL 을 받고, 그 URL 을 마크다운에 넣습니다.

## 지켜야 할 규칙

1. **비밀을 출력하지 마세요.** 명령 출력은 대화 기록에 남습니다. 토큰은 `echo`/`print`/로그로 내보내지 말고 **파일에 바로 저장**하세요(아래 예시가 모두 그렇게 되어 있습니다). 사람에게 토큰을 채팅으로 달라고 하지도 마세요.
2. **비밀을 저장소에 커밋하지 마세요.** 토큰은 `~/.config/hwanstory/` 에만 둡니다.
3. **공개(`PUBLIC`) 게시는 사람이 분명히 시켰을 때만.** 새 글은 기본으로 `DRAFT`(임시저장)로 올리고 주소를 알려 검토받으세요. 사람이 "공개해 줘" 라고 하면 그때 `openType` 을 바꿉니다.
4. **기존 글을 고칠 때는 먼저 읽고(GET), 바꿀 부분만 바꾸고, 나머지는 그대로 다시 보내세요.** 수정(PUT)은 전체 치환입니다 — 태그·시리즈·요약·공개 유형을 빠뜨리면 지워집니다. 큰 변경(글 전체 재작성)은 사람에게 먼저 보여 주세요.
5. **삭제는 사람이 분명히 시켰을 때만.** 되돌릴 수 없습니다.
6. **다른 사람 글에 댓글·좋아요 등 상호작용은 사람이 시켰을 때만.** (`social` 권한)
7. **토큰으로 못 하는 일**: 계정 정보·비밀번호 변경, 탈퇴, 토큰 추가 발급·다른 토큰 폐기, 기기 로그인 승인, 관리자 기능. 필요하면 사람에게 웹에서 하도록 안내하세요.
8. **끝나면 토큰을 폐기하세요** ([마무리](#마무리)) — 사람이 계속 쓰라고 하지 않았다면.

## 작업 순서

1. [인증](#인증-기기-로그인) — 기기 로그인으로 액세스 토큰을 받는다 (사람이 브라우저에서 한 번 승인)
2. [내 정보 확인](#내-정보-확인) — 토큰 주인의 블로그 Id 와 권한을 확인한다
3. 작업 — [게시글](#게시글-다루기) · [기존 글 검토](#기존-글-검토하기) · [시리즈](#시리즈) · [이미지](#이미지-첨부) · [상호작용](#상호작용-social-권한)
4. [마무리](#마무리) — 사람에게 보고, 토큰 폐기

## 인증: 기기 로그인

에이전트는 **기기 로그인**(OAuth device flow 와 같은 방식)으로 토큰을 받습니다. 비밀번호나 토큰을 채팅으로 주고받지 않습니다.

1. 에이전트가 코드를 요청한다 → 8자리 코드(예: `BCDF-GHJK`)와 승인 주소를 받는다
2. 사람에게 주소와 코드를 알려 준다 → 사람이 블로그에 로그인한 브라우저에서 클라이언트 이름·IP·권한을 확인하고 승인한다
3. 에이전트가 기다렸다가 토큰을 받아 `~/.config/hwanstory/auth-header` 에 저장한다 (화면에는 토큰 앞부분만 나옴)

토큰 형식은 `hsp_<16자 id>_<비밀>`, 만료는 승인하는 사람이 정합니다(기본 30일, 무기한도 가능). 권한(scope):

| 권한 | 할 수 있는 일 |
|---|---|
| (토큰이 유효하면) | **읽기 전부** — 내 글(비공개·임시저장 포함), 시리즈, 댓글, 통계, 북마크, 알림 |
| `posts` | 게시글·시리즈 작성/수정/삭제, 파일(이미지) 업로드 |
| `social` | 댓글 작성/수정/삭제, 좋아요·북마크·이모지 반응, 팔로우, 알림 읽음/삭제 |

글만 다룬다면 `posts` 만 요청하세요. 토큰은 승인한 사람의 권한 안에서만 동작하고, 관리자 기능은 토큰으로 쓸 수 없습니다.

### 이미 로그인돼 있는지 확인

```bash
H="$HOME/.config/hwanstory/auth-header"
[ -s "$H" ] && curl -fsS -H @"$H" {{API_URL}}/api/v1/tokens/current
```

`200` 이면 그대로 쓰세요. `401` 이거나 파일이 없으면 아래로 로그인합니다.

### 도우미 스크립트로 로그인 (권장)

짧은 스크립트이니 원하면 먼저 읽어 보세요. 두 단계로 나뉘어 있어서 첫 단계는 바로 끝나고, 그 사이에 사람에게 코드를 전할 수 있습니다.

```bash
curl -fsSL {{API_URL}}/cli/login.sh -o "${TMPDIR:-/tmp}/hs-login.sh"
bash "${TMPDIR:-/tmp}/hs-login.sh" start "Claude Code - 블로그 글쓰기" posts   # 주소와 코드를 출력하고 끝남
# → 사람에게: "브라우저에서 <주소> 를 열고 코드 <코드> 를 승인해 주세요"
bash "${TMPDIR:-/tmp}/hs-login.sh" wait                                        # 승인될 때까지 기다림 (최대 10분)
```

`wait` 는 최대 10분 동안 5초 간격으로 확인하므로 **명령 타임아웃을 10분 이상**으로 주거나 백그라운드로 실행하세요. `status` 는 현재 토큰 확인, `logout` 은 폐기입니다.
세 번째 인자(권한)를 생략하면 `posts social` 모두 요청합니다.

PowerShell(Windows):

```powershell
irm {{API_URL}}/cli/login.ps1 -OutFile "$env:TEMP\hs-login.ps1"
powershell -ExecutionPolicy Bypass -File "$env:TEMP\hs-login.ps1" start -ClientName "Claude Code - 블로그 글쓰기" -Scope posts
powershell -ExecutionPolicy Bypass -File "$env:TEMP\hs-login.ps1" wait
```

### curl 만으로 로그인

도우미를 쓸 수 없을 때. `device_code` 도 비밀이니 출력하지 말고 변수에만 둡니다.

```bash
API={{API_URL}}; H="$HOME/.config/hwanstory/auth-header"; mkdir -p "$(dirname "$H")"
R=$(curl -fsS -X POST "$API/api/v1/authentication/device/code" --data-urlencode "client_name=Claude Code - 블로그 글쓰기" --data-urlencode "scope=posts")
DC=$(printf %s "$R" | sed -E 's/.*"device_code":"([^"]+)".*/\1/')
printf %s "$R" | sed -E 's/.*"verification_uri_complete":"([^"]+)".*/승인 주소: \1/'
# 사람이 승인할 때까지 5초 간격으로:
R=$(curl -sS -X POST "$API/api/v1/authentication/device/token" --data-urlencode "device_code=$DC")
case "$R" in
  *access_token*) (umask 077; printf 'Authorization: Bearer %s\n' "$(printf %s "$R" | sed -E 's/.*"access_token":"([^"]+)".*/\1/')" > "$H"); echo saved ;;
  *) printf %s "$R" | sed -E 's/.*"error":"([^"]+)".*/\1/' ;;   # authorization_pending → 다시, slow_down → 간격 늘리기
esac
```

| 응답 `error` | 뜻 |
|---|---|
| `authorization_pending` | 아직 승인 전 — `interval`(5초) 뒤 다시 |
| `slow_down` | 너무 자주 물음 — 간격을 5초 늘리기 |
| `access_denied` | 사람이 거부함 — 멈추고 이유를 물어보기 |
| `expired_token` | 코드 만료(10분) — 처음부터 |
| `invalid_grant` | 모르는 코드이거나 이미 토큰을 받은 코드 |

### 토큰 쓰기

```bash
curl -fsS -H @"$HOME/.config/hwanstory/auth-header" {{API_URL}}/api/v1/tokens/current
```

```powershell
$auth = (Get-Content "$HOME\.config\hwanstory\auth-header" -Raw).Trim() -replace '^Authorization:\s*', ''
Invoke-RestMethod {{API_URL}}/api/v1/tokens/current -Headers @{ Authorization = $auth }
```

사람이 웹(설정 → 액세스 토큰)에서 직접 만든 토큰을 같은 파일에 넣어 줄 수도 있습니다. 그때도 채팅으로 받지 말고, 웹이 보여 주는 저장 명령을 사람이 자기 터미널에서 실행하게 하세요.

## 내 정보 확인

```bash
H="$HOME/.config/hwanstory/auth-header"; API={{API_URL}}/api/v1
curl -fsS -H @"$H" "$API/tokens/current"
```

응답의 `account.blogId`(예: `@kim-hwan`)가 **내 블로그 Id** 입니다. 아래 예시의 `@me` 자리에 이 값을 넣으세요. `token.scopes` 가 토큰 권한, `token.expiresAt` 이 만료 시각입니다.

## 게시글 다루기

### 내 글 목록 (비공개·임시저장 포함)

```bash
curl -fsS -H @"$H" "$API/blog/@me/posts?size=20"                 # 최신순 20개
curl -fsS -H @"$H" "$API/blog/@me/posts?size=20&cursorId=<마지막 글의 id>"   # 다음 페이지 (응답 cursorId 사용)
curl -fsS -H @"$H" "$API/blog/@me/posts?tag=WPF"                 # 태그로
```

응답은 `{data: [게시글…], size, empty, first, last, cursorId}` 입니다. `last` 가 `true` 가 될 때까지 `cursorId` 로 넘기면 전부 읽습니다.
목록의 글에는 `postUrl, title, summary, openType, tags[{name}], seriesUrl, createdAt, lastModifiedAt, hits, likeCount, commentCount, thumbnailImageUrl` 이 있고 **본문은 없습니다**.
토큰 없이(또는 남의 블로그를) 조회하면 공개 글만 나옵니다.

### 글 하나 읽기 (본문 포함)

```bash
curl -fsS -H @"$H" "$API/blog/@me/posts/<postUrl>"
```

`content` 가 마크다운 본문입니다. **이 요청은 조회수를 1 올립니다** — 전체 글을 훑을 때는 그 점을 사람에게 알려 주세요.

### 새 글 쓰기

```bash
cat > /tmp/post.json <<'EOF'
{
  "postUrl": "my-first-post",
  "title": "제목",
  "summary": "목록과 검색 결과, SNS 미리보기에 보일 한두 문장 (255자 이내)",
  "content": "# 본문\n\n마크다운으로 씁니다.\n\n```csharp\nConsole.WriteLine(\"hi\");\n```\n",
  "openType": "DRAFT",
  "tags": [{"name": "C#"}, {"name": "WPF"}],
  "seriesUrl": null,
  "thumbnailFileId": null,
  "scheduledAt": null
}
EOF
curl -fsS -H @"$H" -H 'Content-Type: application/json' --data-binary @/tmp/post.json "$API/blog/posts"
```

`201`/`200` 응답이 만들어진 글입니다. 사람에게 `{{SITE_URL}}/@me/posts/<postUrl>` 을 알려 주세요(임시저장·비공개 글은 로그인한 주인만 볼 수 있습니다).

| 필드 | 설명 |
|---|---|
| `postUrl` | 글 주소. 영문·숫자·`-`·`_` 만, 320자 이내, 블로그 안에서 유일. 생략하면 서버가 만듭니다. 이미 있으면 `409`. 짧고 내용을 설명하는 영문 소문자-하이픈 형태를 권장합니다 |
| `title` | 제목, 2000자 이내 (필수) |
| `summary` | 요약, 255자 이내 (필수). 검색 결과·SNS 미리보기의 설명문이 되므로 본문 첫 줄 복사가 아니라 **글의 핵심을 한두 문장으로** |
| `content` | 마크다운 본문 (필수). GFM: 제목 `#`, 코드 블록 ```` ```lang ````, 표, 체크리스트, 이미지 `![alt](url)` |
| `openType` | `DRAFT` · `PRIVATE` · `PUBLIC` · `SCHEDULED` (필수) |
| `tags` | `[{"name": "태그"}]`. 생략·빈 배열 가능 |
| `seriesUrl` | 시리즈 주소. 먼저 [시리즈](#시리즈)가 있어야 합니다(없으면 `404`). `null` 이면 시리즈 없음 |
| `thumbnailFileId` | [이미지 첨부](#이미지-첨부)로 올린 파일의 `id`. `null` 이면 썸네일 없음 |
| `scheduledAt` | `SCHEDULED` 일 때 공개 시각, 서버 시간(KST) `2026-11-07T09:00:00` 형식 |

> **Windows 주의**: Windows 의 `curl.exe` 는 명령줄 인자를 ANSI 코드 페이지(한국어 Windows 는 CP949)로 받아서, `-d '{"title":"한글"}'` 처럼 인자에 넣은 한글이 깨집니다.
> 위 예시처럼 JSON 은 **파일로 써서 `--data-binary @파일`** 로 보내세요(파일은 UTF-8, BOM 없이).

### 글 수정

```bash
curl -fsS -H @"$H" "$API/blog/@me/posts/<postUrl>" > /tmp/current.json   # 1. 현재 내용 읽기
# 2. 필요한 필드만 바꿔 /tmp/post.json 을 만든다 (postUrl, title, summary, content, openType, tags, seriesUrl, thumbnailFileId, scheduledAt 전부 포함)
curl -fsS -X PUT -H @"$H" -H 'Content-Type: application/json' --data-binary @/tmp/post.json "$API/blog/posts/<postUrl>"
```

- PUT 본문은 새 글 쓰기와 같은 형식이며 **전체 치환**입니다. 읽은 응답에서 `tags`(`[{name}]`), `seriesUrl`, `openType`, `summary` 를 그대로 옮겨 담으세요. 썸네일을 유지하려면 응답의 `thumbnailImageUrl`(`/attachments/<id>/…`)에서 `<id>` 를 꺼내 `thumbnailFileId` 에 넣습니다.
- 경로의 `<postUrl>` 은 **지금** 주소, 본문의 `postUrl` 은 **바꿀** 주소입니다(같게 두면 유지). 주소를 바꾸면 기존 링크가 깨지니 사람이 시킨 경우에만.
- 읽은 응답의 `content` 는 JSON 문자열이므로 `jq` 가 있으면 안전합니다:
  ```bash
  jq '{postUrl, title, summary, content, openType, seriesUrl, scheduledAt, tags: [.tags[] | {name}], thumbnailFileId: (.thumbnailImageUrl // "" | capture("/attachments/(?<id>[^/]+)")?.id)}' /tmp/current.json > /tmp/post.json
  ```
  그다음 `jq '.summary = "새 요약"' /tmp/post.json` 처럼 고쳐서 보냅니다.

### 글 삭제

```bash
curl -fsS -X DELETE -H @"$H" "$API/blog/posts/<postUrl>"
```

## 기존 글 검토하기

사람이 "내 글들 검토해 줘", "오타 잡아 줘", "SEO 점검해 줘" 같은 일을 시키면:

1. [내 글 목록](#내-글-목록-비공개임시저장-포함)을 끝까지 읽어 `postUrl`·`title`·`summary`·`tags`·`openType` 을 모읍니다 (본문 없이도 요약·태그 점검은 가능).
2. 본문이 필요한 글만 [하나씩 읽습니다](#글-하나-읽기-본문-포함) (조회수가 오르니 양이 많으면 사람에게 알리기).
3. 점검 항목 예:
   - **요약(`summary`)**: 비어 있거나 본문 첫 줄 복사이거나 너무 짧은가(검색 결과 설명문이 됩니다, 80~160자 권장)
   - **제목**: 내용을 설명하는가, 너무 긴가(60자 안팎 권장)
   - **태그**: 없거나 표기가 들쭉날쭉한가(`C#`/`csharp`), 다른 글과 통일
   - **본문**: 오타·깨진 마크다운·깨진 이미지 링크(`![…](…)` 의 주소가 `{{API_URL}}/api/v1/attachments/…` 가 아니면 외부 링크), 코드 블록에 언어 표시가 없는 것, 제목 계층(`#` 다음에 `###`)
   - **시리즈**: 같은 주제의 글이 시리즈로 묶여 있지 않은가
   - **공개 유형**: 오래된 `DRAFT` 가 남아 있지는 않은가
4. 결과를 **표로 정리해 사람에게 먼저 보여 주고**, 사람이 고르면 [글 수정](#글-수정)으로 반영합니다. 한 번에 여러 글을 고칠 때도 글마다 "읽기 → 바꾸기 → PUT" 을 반복하고, 실패한 글은 건너뛰고 보고합니다.

통계도 도움이 됩니다:

```bash
curl -fsS -H @"$H" "$API/blog/me/statistics"   # 글 수, 총 조회수·좋아요·댓글, 인기 글
curl -fsS -H @"$H" "$API/blog/@me"             # 블로그 정보: 주인, 글 수, 팔로워, 태그별 글 수
```

## 시리즈

```bash
curl -fsS "$API/blog/@me/series"                                   # 목록 (seriesUrl, title, postCount)
curl -fsS "$API/blog/@me/series/<seriesUrl>"                       # 상세 (글 목록 포함)
printf '%s' '{"seriesUrl":"wpf-basics","title":"WPF 기초"}' > /tmp/series.json
curl -fsS -H @"$H" -H 'Content-Type: application/json' --data-binary @/tmp/series.json "$API/blog/series"            # 만들기
curl -fsS -X PUT -H @"$H" -H 'Content-Type: application/json' --data-binary @/tmp/series.json "$API/blog/series/<seriesUrl>"   # 수정
curl -fsS -X DELETE -H @"$H" "$API/blog/series/<seriesUrl>"        # 삭제 (글은 남고 시리즈 연결만 풀림)
```

`seriesUrl` 규칙은 `postUrl` 과 같습니다. 글을 시리즈에 넣으려면 글의 `seriesUrl` 을 [수정](#글-수정)합니다.

## 이미지 첨부

```bash
curl -fsS -H @"$H" -F "file=@./diagram.png" "$API/attachments"
# → {"id":"<uuid>","url":"/attachments/<uuid>/image.png","fileName":"diagram.png"}
```

- 마크다운에는 `![설명]({{API_URL}}/api/v1<url>)` 처럼 **`{{API_URL}}/api/v1` + `url`** 로 넣습니다. 예: `![구조도]({{API_URL}}/api/v1/attachments/<uuid>/image.png)`
- 썸네일로 쓰려면 글의 `thumbnailFileId` 에 `id` 를 넣습니다.
- 이미지(`image/*`)·mp4/webm·pdf·zip·exe, 100 MB 까지. 외부 URL 의 파일을 가져오려면 `POST $API/attachments?fileUrl=<url>` (본문 없이).

## 상호작용 (`social` 권한)

사람이 시킨 경우에만. `<blogId>` 는 글 주인의 블로그 Id.

```
POST   /api/v1/blog/<blogId>/posts/<postUrl>/comments      {"content":"마크다운"}   댓글
POST   /api/v1/blog/comments/<commentId>                   {"content":"…"}          대댓글
PUT    /api/v1/blog/comments/<commentId>                   {"content":"…"}          내 댓글 수정
DELETE /api/v1/blog/comments/<commentId>                                            내 댓글(또는 내 글의 댓글) 삭제
POST   /api/v1/blog/<blogId>/posts/<postUrl>/likes       · DELETE 같은 주소          좋아요 · 취소
POST   /api/v1/blog/<blogId>/posts/<postUrl>/bookmarks   · DELETE 같은 주소          북마크 · 취소
POST   /api/v1/blog/<blogId>/posts/<postUrl>/reactions     {"emoji":"👍"}           이모지 반응 (👍 ❤️ 🎉 🤔 👀 😂)
DELETE /api/v1/blog/<blogId>/posts/<postUrl>/reactions?emoji=👍                      반응 취소
POST   /api/v1/follow/<blogId>                           · DELETE 같은 주소          팔로우 · 취소
GET    /api/v1/notifications?unreadOnly=true                                        내 알림 (댓글·팔로우)
PUT    /api/v1/notifications/<id>/mark-as-read                                      알림 읽음
```

댓글은 글의 `comments` 배열(`GET …/posts/<postUrl>`)에, 대댓글은 각 댓글의 `children` 에 있습니다.

## 마무리

```bash
curl -fsS -X DELETE -H @"$HOME/.config/hwanstory/auth-header" {{API_URL}}/api/v1/tokens/current && rm -f "$HOME/.config/hwanstory/auth-header"
```

사람에게 보고할 것: 만들거나 고친 글의 주소와 공개 유형, 고치지 않고 남겨 둔 제안, 실패한 요청과 이유, 토큰을 폐기했는지.

## API 레퍼런스

기본 주소 `{{API_URL}}/api/v1`. 인증은 `Authorization: Bearer hsp_…`. 요청 본문은 `Content-Type: application/json`(UTF-8).
오류는 `{"name": "<오류 코드>", "message": "<설명>"}` JSON 입니다. 시간은 서버 시간(KST)의 ISO-8601(`2026-11-06T10:30:00`).
전체 명세(모든 필드·응답 모델)는 `{{API_URL}}/swagger-ui/index.html` · `{{API_URL}}/v3/api-docs`.

### 인증·토큰

| 요청 | 설명 |
|---|---|
| `POST /authentication/device/code` | form: `client_name`, `scope`(기본 `posts social`) → `device_code`, `user_code`, `verification_uri`, `verification_uri_complete`, `expires_in`, `interval` |
| `POST /authentication/device/token` | form: `device_code` → `200 {access_token, token_type, scope, expires_at, token_id}` 또는 `400 {error, error_description}` |
| `GET /tokens/current` | 이 토큰의 주인 계정(`account.blogId` 등)과 토큰 정보(권한·만료) |
| `DELETE /tokens/current` | 이 토큰을 폐기 |

### 게시글 (읽기는 토큰 없이도 공개 글만, 쓰기는 `posts`)

| 요청 | 설명 |
|---|---|
| `GET /blog/posts?size=&cursorId=&search=&sortBy=createdAt\|hits` | 전체 공개 글 (검색어 가능) |
| `GET /blog/{blogId}` | 블로그 정보: 주인, 글 수, 팔로워·팔로잉 수, 태그별 글 수 |
| `GET /blog/{blogId}/posts?size=&cursorId=&tag=` | 블로그의 글 목록 (주인 토큰이면 비공개·임시저장 포함) |
| `GET /blog/{blogId}/posts/{postUrl}` | 글 하나 (본문·댓글 포함, 조회수 +1) |
| `GET /blog/{blogId}/posts/{postUrl}/related` | 태그가 겹치는 관련 글 |
| `POST /blog/posts` | 새 글 (JSON, 위 필드 표) |
| `PUT /blog/posts/{postUrl}` | 글 수정 (전체 치환) |
| `DELETE /blog/posts/{postUrl}` | 글 삭제 |
| `GET /blog/me/statistics` | 내 통계: 글 수, 총 조회수·좋아요·댓글, 인기 글 |
| `GET /blog/me/bookmarks` | 내 북마크 |
| `GET /blog/{blogId}/series` · `GET …/series/{seriesUrl}` · `GET …/series/{seriesUrl}/posts` | 시리즈 |
| `POST /blog/series` · `PUT /blog/series/{seriesUrl}` · `DELETE /blog/series/{seriesUrl}` | 시리즈 만들기·수정·삭제 (`{seriesUrl, title}`) |
| `POST /attachments` (multipart `file`) · `POST /attachments?fileUrl=` | 파일 업로드 → `{id, url, fileName}` |

### 상호작용 (`social`)

[위 표](#상호작용-social-권한) 참고. 댓글 조회 `GET /blog/comments/{commentId}`, 좋아요·북마크·팔로우 여부는 같은 주소 `GET`(`204` = 예, `404` = 아니오).

## 문제 해결

| 상태 | 흔한 원인 |
|---|---|
| `401 invalid_access_token` | 토큰이 없거나 만료·폐기됨 → 다시 [로그인](#인증-기기-로그인) |
| `401 unauthorized` | 토큰 없이 로그인이 필요한 요청을 함 (`-H @"$H"` 빠짐) |
| `403 access_token_scope_required` | 토큰 권한 부족 (`posts`/`social`) → 필요한 권한으로 다시 로그인 |
| `403 access_token_not_allowed` | 토큰으로는 할 수 없는 일(계정·토큰 관리·관리자) → 사람에게 웹에서 하도록 안내 |
| `404 not_found_post` / `not_found_series` | 주소 오타, 남의 비공개 글, 시리즈가 아직 없음 |
| `409 already_exists_post_url` | 같은 주소의 글이 이미 있음 → 다른 `postUrl` |
| `400 invalid_parameter` | 필수 필드 누락(`title`, `summary`, `content`, `openType`), 주소에 허용되지 않는 글자, JSON 오류(Windows 에서 인자로 넘긴 한글 — 파일로) |
| `429 too_many_requests` | 기기 로그인 코드를 너무 자주 요청함 (IP 당 분당 10회) |
