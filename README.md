# ktypo

Kotlin DSL 하나로 **OpenAPI 3.1 스펙**과 **Spring RestDocs Asciidoc 스니펫**을 동시에 생성하는 라이브러리.

Kotlin 리플렉션(`typeOf<T>()`)을 활용해 `ApiResponse<PageResponse<UserDto>>` 같은 **중첩 제너릭 타입을 자동으로 해석**하므로, 스키마를 수동으로 작성할 필요가 없다.

## 빠르게 시작하기

### 1. 의존성 추가

```kotlin
// build.gradle.kts
dependencies {
    implementation("com.yunhwan:ktypo:0.0.1-SNAPSHOT")
}
```

### 2. 도메인 모델 정의

```kotlin
data class CreateUserRequest(
    val name: String,
    val email: String,
    val age: Int,
)

data class UserDto(
    val id: Long,
    val name: String,
    val email: String,
)

data class ApiResponse<T>(
    val code: Int,
    val message: String,
    val data: T,
)

data class PageResponse<T>(
    val items: List<T>,
    val totalCount: Long,
    val page: Int,
)
```

### 3. DSL로 API 문서 정의 & 생성

```kotlin
import com.yunhwan.ktypo.dsl.ktypo

val docs = ktypo {
    info {
        title("User Management API")
        version("1.0.0")
        description("사용자 관리 API")
    }

    document("create-user") {
        post("/api/users") {
            summary("사용자 생성")
            tags("Users")

            requestBody<CreateUserRequest> {
                field(CreateUserRequest::name) {
                    description("사용자 이름")
                    example("홍길동")
                    minLength(2)
                    maxLength(50)
                }
                field(CreateUserRequest::email) {
                    description("이메일")
                    example("hong@example.com")
                }
            }

            responseBody<ApiResponse<UserDto>> {
                field("code") { description("응답 코드") }
                field("data.id") { description("사용자 ID") }
                field("data.name") { description("사용자 이름") }
            }
        }
    }

    document("list-users") {
        get("/api/users") {
            summary("사용자 목록 조회")
            tags("Users")

            queryParameter("page") {
                description("페이지 번호")
                required(false)
                example(0)
            }

            responseBody<ApiResponse<PageResponse<UserDto>>>()
        }
    }

    document("get-user") {
        get("/api/users/{id}") {
            summary("사용자 단건 조회")
            tags("Users")

            pathParameter("id") {
                description("사용자 ID")
                example(1)
            }

            responseBody<ApiResponse<UserDto>>()
        }
    }
}

docs.generate() // 파일 출력
```

`generate()`를 호출하면 아래 파일이 생성된다:

```
build/generated-docs/
  openapi.json
  openapi.yaml

build/generated-snippets/
  create-user/
    request-fields.adoc
    response-fields.adoc
  list-users/
    query-parameters.adoc
    response-fields.adoc
  get-user/
    path-parameters.adoc
    response-fields.adoc
```

---

## DSL 레퍼런스

### info

```kotlin
ktypo {
    info {
        title("My API")
        version("1.0.0")
        description("API 설명")      // 선택
    }
}
```

### config

출력 경로와 포맷을 변경할 수 있다. 생략하면 기본값이 적용된다.

```kotlin
ktypo {
    config {
        outputDir("docs/openapi")                       // 기본값: build/generated-docs
        snippetDir("docs/snippets")                     // 기본값: build/generated-snippets
        format(KtypoConfig.OutputFormat.YAML)           // JSON | YAML | BOTH (기본값)
        generateRestDocs(false)                         // RestDocs 스니펫 생성 여부 (기본값: true)
    }
}
```

### document

`document("식별자")` 블록 안에서 HTTP 메서드를 선택한다. 하나의 document에는 하나의 operation만 정의할 수 있다.

```kotlin
document("create-user") {
    post("/api/users") { /* ... */ }
}

document("get-user") {
    get("/api/users/{id}") { /* ... */ }
}
```

지원 메서드: `get`, `post`, `put`, `delete`, `patch`

### operation

```kotlin
post("/api/users") {
    summary("사용자 생성")
    description("사용자를 생성합니다")   // 선택
    tags("Users", "Admin")             // 선택, 여러 개 가능
    operationId("createUser")          // 선택
    deprecated()                       // 선택
}
```

### requestBody / responseBody

`inline reified` 제너릭으로 타입 정보를 캡처한다. 중첩 제너릭도 그대로 사용하면 된다.

```kotlin
requestBody<CreateUserRequest> { /* 필드 설명 */ }

responseBody<ApiResponse<PageResponse<UserDto>>> { /* 필드 설명 */ }
```

`responseBody`는 `statusCode`와 `description`을 지정할 수 있다:

```kotlin
responseBody<ErrorResponse> {
    statusCode(400)
    description("잘못된 요청")
    field("message") { description("에러 메시지") }
}
```

### field

필드 설명은 두 가지 방식으로 지정한다:

```kotlin
// 1. KProperty1 참조 (타입 안전, 최상위 필드만)
field(CreateUserRequest::name) {
    description("이름")
    example("홍길동")
}

// 2. 문자열 경로 (중첩 필드 접근 가능)
field("data.id") { description("사용자 ID") }
```

필드에 설정 가능한 속성:

| 메서드 | 타입 | 설명 |
|--------|------|------|
| `description()` | `String` | 필드 설명 |
| `example()` | `Any` | 예시 값 |
| `format()` | `String` | OpenAPI format (예: `"email"`, `"uri"`) |
| `pattern()` | `String` | 정규식 패턴 |
| `minLength()` | `Int` | 최소 길이 |
| `maxLength()` | `Int` | 최대 길이 |
| `minimum()` | `Number` | 최솟값 |
| `maximum()` | `Number` | 최댓값 |
| `deprecated()` | `Boolean` | 사용 중단 여부 |

### parameters

```kotlin
get("/api/users/{id}") {
    pathParameter("id") {
        description("사용자 ID")
        example(1)
    }

    queryParameter("page") {
        description("페이지 번호")
        required(false)          // 기본값: query=false, path=true
        example(0)
    }

    headerParameter("X-Request-Id") {
        description("요청 추적 ID")
        required(false)
    }
}
```

### 파일 출력 없이 문자열로 받기

```kotlin
val json = docs.toJson()   // OpenAPI JSON 문자열
val yaml = docs.toYaml()   // OpenAPI YAML 문자열
```

---

## 지원 타입

| Kotlin 타입 | OpenAPI 변환 |
|-------------|-------------|
| `String` | `type: string` |
| `Int`, `Short`, `Byte` | `type: integer, format: int32` |
| `Long` | `type: integer, format: int64` |
| `Float` | `type: number, format: float` |
| `Double` | `type: number, format: double` |
| `Boolean` | `type: boolean` |
| `BigDecimal` | `type: number` |
| `BigInteger` | `type: integer` |
| `LocalDate` | `type: string, format: date` |
| `LocalDateTime`, `OffsetDateTime`, `ZonedDateTime`, `Instant` | `type: string, format: date-time` |
| `UUID` | `type: string, format: uuid` |
| `URI` | `type: string, format: uri` |
| `Enum` | `type: string, enum: [...]` |
| `List<T>`, `Set<T>` | `type: array, items: <T>` |
| `Map<String, V>` | `type: object, additionalProperties: <V>` |
| `data class` | `type: object` (`$ref`로 등록) |
| `sealed class` | `oneOf` + `discriminator` |
| `T?` (nullable) | `nullable: true` |

---

## 주의사항

### data class만 스키마로 등록된다

`$ref`를 통한 스키마 등록은 **data class**(또는 primary constructor가 있는 클래스)에 대해서만 동작한다. 일반 클래스나 인터페이스는 `type: object`로 처리된다.

### 프로퍼티 순서는 primary constructor 기준

생성된 스키마의 필드 순서는 `primaryConstructor`의 파라미터 선언 순서를 따른다. body에 정의되지 않은 프로퍼티가 있다면 뒤쪽에 추가된다.

### 제너릭 타입과 스키마 이름 규칙

제너릭 타입은 타입 인자를 `_`로 연결하여 스키마 이름을 생성한다:

```
ApiResponse<UserDto>                     → ApiResponse_UserDto
ApiResponse<PageResponse<UserDto>>       → ApiResponse_PageResponse_UserDto
PageResponse<UserDto>                    → PageResponse_UserDto
```

`PageResponse<T>`에서 `items: List<T>`는 **프로퍼티의 타입 해석** 단계에서 `List<UserDto>`로 치환되므로, 스키마 이름에 `List`가 포함되지 않는다. 직접 `PageResponse<List<UserDto>>`를 전달하면 `items`는 `List<List<UserDto>>`가 되므로 주의해야 한다.

### 순환 참조

자기 자신을 참조하는 타입(예: 트리 구조)은 자동으로 `$ref`로 처리된다. 무한 루프에 빠지지 않는다.

```kotlin
data class TreeNode(val value: String, val children: List<TreeNode>)
// children → array of $ref TreeNode
```

### sealed class와 discriminator

sealed class는 `oneOf`로 변환되며, 기본 discriminator 속성은 `"type"`이다. 현재는 discriminator 속성명을 커스터마이즈하는 기능은 제공하지 않는다.

### document 식별자

`document("create-user")`의 식별자는 RestDocs 스니펫 디렉토리명으로 사용된다. Spring RestDocs와 통합할 경우, 테스트에서 사용하는 identifier와 동일하게 맞추면 된다.

### field 경로에서의 dot-notation

`field("data.id")`처럼 dot-notation으로 중첩 필드를 지정할 수 있다. 이때 RestDocs 스니펫에는 `data.id`가 그대로 경로로 출력된다. 배열 내부 필드는 `items[].name` 형태로 자동 표기된다.

---

## 생성 결과 예시

### OpenAPI YAML

```yaml
openapi: 3.1.0
info:
  title: User Management API
  version: 1.0.0
paths:
  /api/users:
    post:
      summary: 사용자 생성
      tags:
        - Users
      requestBody:
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/CreateUserRequest'
        required: true
      responses:
        "200":
          description: OK
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ApiResponse_UserDto'
components:
  schemas:
    CreateUserRequest:
      type: object
      properties:
        name:
          type: string
        email:
          type: string
        age:
          type: integer
          format: int32
      required:
        - name
        - email
        - age
    UserDto:
      type: object
      properties:
        id:
          type: integer
          format: int64
        name:
          type: string
        email:
          type: string
      required:
        - id
        - name
        - email
    ApiResponse_UserDto:
      type: object
      properties:
        code:
          type: integer
          format: int32
        message:
          type: string
        data:
          $ref: '#/components/schemas/UserDto'
      required:
        - code
        - message
        - data
```

### RestDocs Asciidoc (request-fields.adoc)

```asciidoc
.Request Fields
|===
|Path|Type|Description|Optional

|`name`
|`String`
|사용자 이름
|false

|`email`
|`String`
|이메일
|false

|`age`
|`Integer`
|
|false

|===
```

---

## 기술 스택

- Kotlin 2.2.21
- Java 24
- Jackson 2.18.3 (JSON/YAML 직렬화)
- Swagger Models 2.2.28 (OpenAPI 모델)
- Kotlin Reflect (타입 해석)

## 라이센스

MIT
