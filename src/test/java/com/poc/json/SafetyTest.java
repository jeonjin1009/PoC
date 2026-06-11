package com.poc.json;

import com.poc.json.model.User;
import com.poc.json.repository.UserRepository;
import com.poc.json.service.UserService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.FileWriter;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Safety Test — 이상한 값, 잘못된 입력, 악의적 사용자 시나리오 검증
 *
 * 목표: 예외가 발생하더라도 데이터 파일이 손상되지 않고,
 *       정상 흐름에는 영향을 주지 않음을 확인한다.
 */
class SafetyTest {

    // ══════════════════════════════════════════════════════
    // 1. Null 입력 안전성
    // ══════════════════════════════════════════════════════
    @Nested
    @DisplayName("Null 입력 안전성")
    class NullInputSafety {

        @TempDir Path tempDir;
        UserService service;

        @BeforeEach
        void setUp() throws Exception {
            service = newService(tempDir);
        }

        @Test
        @DisplayName("null name으로 create해도 저장되고 조회된다")
        void nullNameCreate() throws Exception {
            User u = service.create(null, "a@t.com", 20, true, "서울", "강남구", "00001", "");
            assertThat(service.findById(u.getId())).isPresent();
        }

        @Test
        @DisplayName("null name 사용자가 있어도 이름 검색 시 NPE가 발생하지 않는다")
        void nullNameDoesNotCauseNpeOnSearch() throws Exception {
            service.create(null, "a@t.com", 20, true, "서울", "강남구", "00001", "");
            service.create("정상이름", "b@t.com", 25, true, "서울", "강남구", "00001", "");

            assertThatCode(() -> service.searchByName("정상이름"))
                    .doesNotThrowAnyException();

            List<User> results = service.searchByName("정상이름");
            assertThat(results).hasSize(1);
        }

        @Test
        @DisplayName("null rawTags로 create해도 NPE가 발생하지 않는다")
        void nullRawTagsCreate() {
            assertThatCode(() -> service.create("테스터", "t@t.com", 20, true, "서울", "강남구", "00001", null))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("null email로 create해도 저장된다")
        void nullEmailCreate() throws Exception {
            User u = service.create("홍길동", null, 20, true, "서울", "강남구", "00001", "");
            assertThat(service.findById(u.getId()).orElseThrow().getEmail()).isNull();
        }
    }

    // ══════════════════════════════════════════════════════
    // 2. 빈 문자열 입력 안전성
    // ══════════════════════════════════════════════════════
    @Nested
    @DisplayName("빈 문자열 입력 안전성")
    class EmptyStringInputSafety {

        @TempDir Path tempDir;
        UserService service;

        @BeforeEach
        void setUp() throws Exception {
            service = newService(tempDir);
            service.create("A", "a@t.com", 20, true, "서울", "강남구", "00001", "");
            service.create("B", "b@t.com", 21, true, "서울", "강남구", "00001", "");
        }

        @Test
        @DisplayName("빈 문자열로 이름 검색 시 모든 사용자가 반환된다 (contains 특성)")
        void emptyKeywordMatchesAll() throws Exception {
            List<User> results = service.searchByName("");
            assertThat(results).hasSize(2);
        }

        @Test
        @DisplayName("빈 name으로 create해도 저장된다")
        void emptyNameCreate() throws Exception {
            User u = service.create("", "c@t.com", 22, true, "서울", "강남구", "00001", "");
            assertThat(service.findById(u.getId())).isPresent();
        }

        @Test
        @DisplayName("name을 빈 문자열로 수정해도 저장된다")
        void updateNameToEmpty() throws Exception {
            service.updateField(1L, "name", "");
            assertThat(service.findById(1L).orElseThrow().getName()).isEmpty();
        }

        @Test
        @DisplayName("email을 빈 문자열로 수정해도 저장된다")
        void updateEmailToEmpty() throws Exception {
            service.updateField(1L, "email", "");
            assertThat(service.findById(1L).orElseThrow().getEmail()).isEmpty();
        }
    }

    // ══════════════════════════════════════════════════════
    // 3. age 잘못된 입력 안전성
    // ══════════════════════════════════════════════════════
    @Nested
    @DisplayName("age 잘못된 입력 안전성")
    class InvalidAgeSafety {

        @TempDir Path tempDir;
        UserService service;

        @BeforeEach
        void setUp() throws Exception {
            service = newService(tempDir);
            service.create("테스터", "t@t.com", 25, true, "서울", "강남구", "00001", "");
        }

        @ParameterizedTest(name = "age = \"{0}\" → NumberFormatException")
        @ValueSource(strings = {"abc", "25.5", "1e5", "", "  ", "2147483648", "-2147483649", "99999999999", "일이삼"})
        @DisplayName("숫자가 아닌 age 수정 시 NumberFormatException 발생")
        void invalidAgeStringThrowsNfe(String invalid) {
            assertThatThrownBy(() -> service.updateField(1L, "age", invalid))
                    .isInstanceOf(NumberFormatException.class);
        }

        @Test
        @DisplayName("age 예외 발생 후에도 기존 데이터가 유지된다")
        void dataIntactAfterAgeException() throws Exception {
            assertThatThrownBy(() -> service.updateField(1L, "age", "abc"))
                    .isInstanceOf(NumberFormatException.class);

            User user = service.findById(1L).orElseThrow();
            assertThat(user.getAge()).isEqualTo(25);
            assertThat(user.getName()).isEqualTo("테스터");
        }

        @Test
        @DisplayName("음수 age는 유효성 검사 없이 저장된다 (현재 설계)")
        void negativeAgeIsSaved() throws Exception {
            service.updateField(1L, "age", "-1");
            assertThat(service.findById(1L).orElseThrow().getAge()).isEqualTo(-1);
        }

        @Test
        @DisplayName("Integer.MAX_VALUE age는 저장된다")
        void maxIntAgeIsSaved() throws Exception {
            service.updateField(1L, "age", String.valueOf(Integer.MAX_VALUE));
            assertThat(service.findById(1L).orElseThrow().getAge()).isEqualTo(Integer.MAX_VALUE);
        }
    }

    // ══════════════════════════════════════════════════════
    // 4. 악의적 문자열 입력 안전성 (JSON 인젝션, XSS, 경로조작)
    // ══════════════════════════════════════════════════════
    @Nested
    @DisplayName("악의적 문자열 입력 안전성")
    class MaliciousInputSafety {

        @TempDir Path tempDir;
        UserService service;

        @BeforeEach
        void setUp() throws Exception {
            service = newService(tempDir);
        }

        @Test
        @DisplayName("JSON 인젝션 시도 - 데이터 무결성이 유지된다")
        void jsonInjectionAttempt() throws Exception {
            // Jackson이 이중 인용부호, 역슬래시를 이스케이프해서 저장
            String malicious = "\",\"id\":9999,\"name\":\"injected";
            User u = service.create(malicious, "x@t.com", 20, true, "서울", "강남구", "00001", "");

            User loaded = service.findById(u.getId()).orElseThrow();
            assertThat(loaded.getName()).isEqualTo(malicious);
            assertThat(loaded.getId()).isNotEqualTo(9999L);
            assertThat(service.findAll()).hasSize(1);
        }

        @Test
        @DisplayName("XSS 시도 문자열이 그대로 저장되고 파싱된다")
        void xssInputStoredAsPlainString() throws Exception {
            String xss = "<script>alert('xss')</script>";
            User u = service.create(xss, "x@t.com", 20, true, "서울", "강남구", "00001", "");

            assertThat(service.findById(u.getId()).orElseThrow().getName()).isEqualTo(xss);
        }

        @Test
        @DisplayName("SQL 인젝션 시도 문자열이 그대로 저장된다")
        void sqlInjectionStoredAsPlainString() throws Exception {
            String sql = "' OR '1'='1'; DROP TABLE users; --";
            User u = service.create(sql, "x@t.com", 20, true, "서울", "강남구", "00001", "");

            assertThat(service.findById(u.getId()).orElseThrow().getName()).isEqualTo(sql);
        }

        @Test
        @DisplayName("줄바꿈/탭 문자가 포함된 이름이 저장되고 정확히 복원된다")
        void nameWithNewlineAndTab() throws Exception {
            String tricky = "홍\n길\t동\r";
            User u = service.create(tricky, "x@t.com", 20, true, "서울", "강남구", "00001", "");

            assertThat(service.findById(u.getId()).orElseThrow().getName()).isEqualTo(tricky);
        }

        @Test
        @DisplayName("이모지/유니코드 문자가 저장되고 정확히 복원된다")
        void emojiAndUnicode() throws Exception {
            String emoji = "테스터🎉한자漢字العربية";
            User u = service.create(emoji, "x@t.com", 20, true, "서울", "강남구", "00001", "");

            assertThat(service.findById(u.getId()).orElseThrow().getName()).isEqualTo(emoji);
        }

        @Test
        @DisplayName("경로 조작 시도 문자열이 이름 필드에 그냥 저장된다")
        void pathTraversalInNameField() throws Exception {
            String path = "../../../etc/passwd";
            User u = service.create(path, "x@t.com", 20, true, "서울", "강남구", "00001", "");

            assertThat(service.findById(u.getId()).orElseThrow().getName()).isEqualTo(path);
        }

        @Test
        @DisplayName("역슬래시 포함 문자열이 저장되고 정확히 복원된다")
        void backslashInName() throws Exception {
            String bs = "홍\\길동\\\\test";
            User u = service.create(bs, "x@t.com", 20, true, "서울", "강남구", "00001", "");

            assertThat(service.findById(u.getId()).orElseThrow().getName()).isEqualTo(bs);
        }
    }

    // ══════════════════════════════════════════════════════
    // 5. 극단값 안전성
    // ══════════════════════════════════════════════════════
    @Nested
    @DisplayName("극단값 안전성")
    class ExtremeValueSafety {

        @TempDir Path tempDir;
        UserService service;

        @BeforeEach
        void setUp() throws Exception {
            service = newService(tempDir);
        }

        @Test
        @DisplayName("매우 긴 이름(10,000자)이 저장되고 복원된다")
        void veryLongName() throws Exception {
            String longName = "가".repeat(10_000);
            User u = service.create(longName, "x@t.com", 20, true, "서울", "강남구", "00001", "");

            assertThat(service.findById(u.getId()).orElseThrow().getName()).isEqualTo(longName);
        }

        @Test
        @DisplayName("태그 500개가 저장되고 복원된다")
        void manyTags() throws Exception {
            String rawTags = String.join(",", java.util.Collections.nCopies(500, "tag"));
            User u = service.create("A", "a@t.com", 20, true, "서울", "강남구", "00001", rawTags);

            assertThat(service.findById(u.getId()).orElseThrow().getTags()).hasSize(500);
        }

        @Test
        @DisplayName("age = Integer.MIN_VALUE가 저장된다 (현재 설계: 음수 허용)")
        void ageMinValue() throws Exception {
            User u = service.create("A", "a@t.com", Integer.MIN_VALUE, true, "서울", "강남구", "00001", "");
            assertThat(service.findById(u.getId()).orElseThrow().getAge()).isEqualTo(Integer.MIN_VALUE);
        }

        @Test
        @DisplayName("사용자 1,000명 연속 추가 후 전체 조회가 정상 동작한다")
        void bulkInsert1000Users() throws Exception {
            for (int i = 1; i <= 1_000; i++) {
                service.create("사용자" + i, "user" + i + "@t.com", i % 100, true, "서울", "강남구", "00001", "");
            }
            assertThat(service.findAll()).hasSize(1_000);
            assertThat(service.findById(1_000L)).isPresent();
        }
    }

    // ══════════════════════════════════════════════════════
    // 6. 잘못된 필드명 / 지원하지 않는 연산 안전성
    // ══════════════════════════════════════════════════════
    @Nested
    @DisplayName("잘못된 필드명 / 지원하지 않는 연산 안전성")
    class InvalidOperationSafety {

        @TempDir Path tempDir;
        UserService service;

        @BeforeEach
        void setUp() throws Exception {
            service = newService(tempDir);
            service.create("홍길동", "hong@t.com", 25, true, "서울", "강남구", "00001", "user");
        }

        @ParameterizedTest(name = "필드명 \"{0}\" → IllegalArgumentException")
        @ValueSource(strings = {"id", "createdAt", "unknown", "NAME", "Age", " name", "name ", ""})
        @DisplayName("지원하지 않는 필드명으로 수정 시 IllegalArgumentException 발생")
        void unsupportedFieldThrows(String field) {
            assertThatThrownBy(() -> service.updateField(1L, field, "value"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("잘못된 필드명 예외 후에도 데이터가 변경되지 않는다")
        void dataIntactAfterInvalidField() throws Exception {
            assertThatThrownBy(() -> service.updateField(1L, "unknown", "value"))
                    .isInstanceOf(IllegalArgumentException.class);

            User user = service.findById(1L).orElseThrow();
            assertThat(user.getName()).isEqualTo("홍길동");
            assertThat(user.getAge()).isEqualTo(25);
        }

        @Test
        @DisplayName("음수 ID로 조회해도 예외 없이 empty를 반환한다")
        void negativeIdReturnsEmpty() throws Exception {
            assertThat(service.findById(-1L)).isEmpty();
        }

        @Test
        @DisplayName("Long.MAX_VALUE ID로 조회해도 예외 없이 empty를 반환한다")
        void maxLongIdReturnsEmpty() throws Exception {
            assertThat(service.findById(Long.MAX_VALUE)).isEmpty();
        }

        @Test
        @DisplayName("존재하지 않는 ID에 반복 수정 시도해도 데이터가 손상되지 않는다")
        void repeatedInvalidUpdateDoesNotCorruptData() throws Exception {
            for (int i = 0; i < 100; i++) {
                service.updateField(999L, "name", "ghost");
            }
            assertThat(service.findAll()).hasSize(1);
            assertThat(service.findById(1L).orElseThrow().getName()).isEqualTo("홍길동");
        }
    }

    // ══════════════════════════════════════════════════════
    // 7. 파일 손상 시나리오 안전성
    // ══════════════════════════════════════════════════════
    @Nested
    @DisplayName("파일 손상 시나리오 안전성")
    class FileCorruptionSafety {

        @TempDir Path tempDir;

        @Test
        @DisplayName("JSON 문법이 깨진 파일을 읽으면 IOException이 발생한다")
        void corruptJsonThrowsException() throws Exception {
            Path dataFile = tempDir.resolve("corrupt.json");
            try (FileWriter fw = new FileWriter(dataFile.toFile())) {
                fw.write("{ NOT VALID JSON [[[");
            }
            UserService service = new UserService(new UserRepository(dataFile.toString()));
            assertThatThrownBy(() -> service.findAll())
                    .isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("빈 파일을 읽으면 IOException이 발생한다")
        void emptyFileThrowsException() throws Exception {
            Path dataFile = tempDir.resolve("empty.json");
            dataFile.toFile().createNewFile();
            UserService service = new UserService(new UserRepository(dataFile.toString()));
            assertThatThrownBy(() -> service.findAll())
                    .isInstanceOf(Exception.class);
        }
    }

    // ══════════════════════════════════════════════════════
    // 8. 못된 사용자 시나리오 (Adversarial)
    // ══════════════════════════════════════════════════════
    @Nested
    @DisplayName("못된 사용자 시나리오")
    class AdversarialUserScenarios {

        @TempDir Path tempDir;
        UserService service;

        @BeforeEach
        void setUp() throws Exception {
            service = newService(tempDir);
            service.create("A", "a@t.com", 20, true, "서울", "강남구", "00001", "");
            service.create("B", "b@t.com", 21, true, "서울", "강남구", "00001", "");
        }

        @Test
        @DisplayName("같은 ID를 여러 번 삭제 - 두 번째부터 false 반환, 목록 손상 없음")
        void doubleDeleteReturnsFalse() throws Exception {
            assertThat(service.delete(1L)).isTrue();
            assertThat(service.delete(1L)).isFalse();
            assertThat(service.delete(1L)).isFalse();
            assertThat(service.findAll()).hasSize(1);
        }

        @Test
        @DisplayName("동일 데이터를 100번 중복 추가 - 각각 독립 ID로 저장된다")
        void duplicateDataCreatesIndependentRecords() throws Exception {
            for (int i = 0; i < 100; i++) {
                service.create("중복사용자", "dup@t.com", 20, true, "서울", "강남구", "00001", "");
            }
            List<User> dups = service.searchByName("중복사용자");
            assertThat(dups).hasSize(100);
            long distinctIds = dups.stream().mapToLong(User::getId).distinct().count();
            assertThat(distinctIds).isEqualTo(100);
        }

        @Test
        @DisplayName("존재하지 않는 ID를 100번 삭제 시도해도 기존 데이터가 유지된다")
        void repeatedDeleteNotFoundDoesNotCorrupt() throws Exception {
            for (int i = 0; i < 100; i++) {
                service.delete(999L);
            }
            assertThat(service.findAll()).hasSize(2);
        }

        @Test
        @DisplayName("active 필드에 true/false 외 값 입력 시 false로 저장된다 (Boolean.parseBoolean 동작)")
        void invalidActiveFallsToFalse() throws Exception {
            service.updateField(1L, "active", "yes");
            assertThat(service.findById(1L).orElseThrow().isActive()).isFalse();

            service.updateField(1L, "active", "1");
            assertThat(service.findById(1L).orElseThrow().isActive()).isFalse();

            service.updateField(1L, "active", "TRUE");
            assertThat(service.findById(1L).orElseThrow().isActive()).isTrue();
        }
    }

    // ── 공통 헬퍼 ──────────────────────────────────────────

    private UserService newService(Path dir) throws Exception {
        Path filePath = dir.toFile().isDirectory()
                ? dir.resolve("users.json")
                : dir;
        return new UserService(new UserRepository(filePath.toString()));
    }
}
