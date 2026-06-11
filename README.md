# Jackson JSON 파싱 PoC

Java + Jackson 라이브러리를 활용한 JSON 파싱 Proof of Concept입니다.

## 사전 요구사항

- **JDK 17+** (https://adoptium.net 또는 https://www.oracle.com/java/)
- **Gradle 8.8** (`gradlew.bat` 으로 자동 다운로드)

> Gradle은 `gradlew.bat`을 처음 실행할 때 자동으로 설치됩니다.

## 실행 방법

```bash
# 의존성 다운로드 + 빌드
gradlew.bat build

# 메인 실행
gradlew.bat run

# 테스트만 실행
gradlew.bat test
```

## 프로젝트 구조

```
json-poc/
├── build.gradle
├── src/
│   ├── main/
│   │   ├── java/com/poc/json/
│   │   │   ├── Main.java               # 데모 실행 진입점
│   │   │   ├── model/
│   │   │   │   ├── User.java           # 사용자 POJO
│   │   │   │   ├── Address.java        # 주소 POJO
│   │   │   │   ├── Order.java          # 주문 POJO
│   │   │   │   └── OrderItem.java      # 주문 항목 POJO
│   │   │   └── parser/
│   │   │       └── JsonFileParser.java # Jackson 파싱 유틸리티
│   │   └── resources/
│   │       ├── users.json              # 샘플 사용자 데이터 (배열)
│   │       └── order.json             # 샘플 주문 데이터 (중첩 객체)
│   └── test/
│       └── java/com/poc/json/
│           └── JsonFileParserTest.java # JUnit 5 테스트
```

## 주요 기능 데모

| Demo | 내용 |
|------|------|
| Demo 1 | JSON 배열 → `List<User>` 역직렬화 |
| Demo 2 | 중첩 JSON 객체 → `Order` POJO 역직렬화 |
| Demo 3 | `JsonNode` 트리 모델로 동적 필드 접근 |
| Demo 4 | POJO → JSON 직렬화 |
| Demo 5 | JSON 문자열 파싱 + 중첩 필드 경로 추출 |

## 핵심 Jackson 어노테이션

| 어노테이션 | 용도 |
|-----------|------|
| `@JsonIgnoreProperties(ignoreUnknown = true)` | 알 수 없는 필드 무시 |
| `@JsonProperty("fieldName")` | JSON 키 이름 매핑 |
| `@JsonDeserialize` / `@JsonSerialize` | 커스텀 직렬화/역직렬화 |
