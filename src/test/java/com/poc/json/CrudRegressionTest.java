package com.poc.json;

import com.poc.json.model.User;
import com.poc.json.repository.UserRepository;
import com.poc.json.service.UserService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * CRUD 콘솔 애플리케이션 Regression Test
 *
 * 기존 동작이 변경/추가 코드로 인해 깨지지 않는지 검증한다.
 * 각 @Nested 클래스는 독립된 TempDir 파일을 사용해 서로 간섭하지 않는다.
 */
class CrudRegressionTest {

    // ══════════════════════════════════════════════════════
    // 1. ID 채번 회귀
    // ══════════════════════════════════════════════════════
    @Nested
    @DisplayName("ID 채번 회귀")
    class IdSequenceRegression {

        @TempDir Path tempDir;
        UserService service;

        @BeforeEach
        void setUp() throws Exception {
            service = newService(tempDir);
        }

        @Test
        @DisplayName("최초 사용자는 ID=1 부터 시작한다")
        void firstUserGetsId1() throws Exception {
            User u = service.create("A", "a@t.com", 20, true, "서울", "강남구", "00001", "");
            assertThat(u.getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("연속 추가 시 ID가 순차 증가한다")
        void sequentialIds() throws Exception {
            User u1 = service.create("A", "a@t.com", 20, true, "서울", "강남구", "00001", "");
            User u2 = service.create("B", "b@t.com", 21, true, "서울", "강남구", "00001", "");
            User u3 = service.create("C", "c@t.com", 22, true, "서울", "강남구", "00001", "");

            assertThat(u1.getId()).isEqualTo(1L);
            assertThat(u2.getId()).isEqualTo(2L);
            assertThat(u3.getId()).isEqualTo(3L);
        }

        @Test
        @DisplayName("중간 ID 삭제 후 신규 추가 시 ID를 재사용하지 않는다")
        void noIdReuseAfterDelete() throws Exception {
            service.create("A", "a@t.com", 20, true, "서울", "강남구", "00001", "");
            service.create("B", "b@t.com", 21, true, "서울", "강남구", "00001", "");
            service.delete(1L);

            User newUser = service.create("C", "c@t.com", 22, true, "서울", "강남구", "00001", "");
            assertThat(newUser.getId()).isEqualTo(3L);
        }

        @Test
        @DisplayName("전체 삭제 후 재추가 시 ID는 1부터 다시 시작한다 (현재 파일 기반 채번)")
        void idAfterFullDelete() throws Exception {
            service.create("A", "a@t.com", 20, true, "서울", "강남구", "00001", "");
            service.create("B", "b@t.com", 21, true, "서울", "강남구", "00001", "");
            service.delete(1L);
            service.delete(2L);

            // 빈 목록 → max 없음 → 0+1 = 1 로 채번
            User after = service.create("C", "c@t.com", 22, true, "서울", "강남구", "00001", "");
            assertThat(after.getId()).isEqualTo(1L);
        }
    }

    // ══════════════════════════════════════════════════════
    // 2. 데이터 영속성 회귀
    // ══════════════════════════════════════════════════════
    @Nested
    @DisplayName("데이터 영속성 회귀")
    class PersistenceRegression {

        @TempDir Path tempDir;

        @Test
        @DisplayName("Repository 재생성 후에도 저장된 데이터가 유지된다")
        void dataSurvivesRepositoryRestart() throws Exception {
            UserService s1 = newService(tempDir);
            s1.create("김철수", "kim@t.com", 30, true, "서울", "강남구", "00001", "admin");

            UserService s2 = newService(tempDir);
            List<User> users = s2.findAll();
            assertThat(users).hasSize(1);
            assertThat(users.get(0).getName()).isEqualTo("김철수");
            assertThat(users.get(0).getTags()).containsExactly("admin");
        }

        @Test
        @DisplayName("수정 후 Repository 재생성 시 수정 내용이 유지된다")
        void updateSurvivesRepositoryRestart() throws Exception {
            UserService s1 = newService(tempDir);
            s1.create("홍길동", "hong@t.com", 25, true, "부산", "해운대구", "00002", "");
            s1.updateField(1L, "email", "updated@t.com");

            UserService s2 = newService(tempDir);
            User user = s2.findById(1L).orElseThrow();
            assertThat(user.getEmail()).isEqualTo("updated@t.com");
        }

        @Test
        @DisplayName("삭제 후 Repository 재생성 시 삭제가 반영된다")
        void deleteSurvivesRepositoryRestart() throws Exception {
            UserService s1 = newService(tempDir);
            s1.create("A", "a@t.com", 20, true, "서울", "강남구", "00001", "");
            s1.delete(1L);

            UserService s2 = newService(tempDir);
            assertThat(s2.findAll()).isEmpty();
        }

        @Test
        @DisplayName("데이터 파일이 없으면 자동 생성되고 빈 목록을 반환한다")
        void autoCreateDataFile() throws Exception {
            Path newPath = tempDir.resolve("sub/auto/users.json");
            UserService s = new UserService(new UserRepository(newPath.toString()));
            assertThat(s.findAll()).isEmpty();
            assertThat(newPath.toFile()).exists();
        }
    }

    // ══════════════════════════════════════════════════════
    // 3. 전체 CRUD 흐름 회귀
    // ══════════════════════════════════════════════════════
    @Nested
    @DisplayName("전체 CRUD 흐름 회귀")
    class FullFlowRegression {

        @TempDir Path tempDir;
        UserService service;

        @BeforeEach
        void setUp() throws Exception {
            service = newService(tempDir);
        }

        @Test
        @DisplayName("Create → Read → Update → Delete 전체 흐름이 정상 동작한다")
        void fullCrudCycle() throws Exception {
            // Create
            User created = service.create("박민준", "park@t.com", 28, true, "대구", "수성구", "42000", "user");
            assertThat(created.getId()).isEqualTo(1L);

            // Read
            User found = service.findById(1L).orElseThrow();
            assertThat(found.getName()).isEqualTo("박민준");

            // Update
            service.updateField(1L, "age", "29");
            service.updateField(1L, "city", "대전");
            User updated = service.findById(1L).orElseThrow();
            assertThat(updated.getAge()).isEqualTo(29);
            assertThat(updated.getAddress().getCity()).isEqualTo("대전");

            // Delete
            assertThat(service.delete(1L)).isTrue();
            assertThat(service.findById(1L)).isEmpty();
            assertThat(service.findAll()).isEmpty();
        }

        @Test
        @DisplayName("여러 사용자 추가 후 일부 수정/삭제해도 나머지 데이터는 변하지 않는다")
        void partialUpdateDeleteDoesNotAffectOthers() throws Exception {
            service.create("A", "a@t.com", 20, true, "서울", "강남구", "00001", "");
            service.create("B", "b@t.com", 21, true, "부산", "해운대구", "00002", "");
            service.create("C", "c@t.com", 22, true, "대구", "수성구", "00003", "");

            service.updateField(2L, "name", "수정된B");
            service.delete(3L);

            User a = service.findById(1L).orElseThrow();
            User b = service.findById(2L).orElseThrow();

            assertThat(a.getName()).isEqualTo("A");
            assertThat(b.getName()).isEqualTo("수정된B");
            assertThat(service.findAll()).hasSize(2);
        }
    }

    // ══════════════════════════════════════════════════════
    // 4. 검색 회귀
    // ══════════════════════════════════════════════════════
    @Nested
    @DisplayName("검색 회귀")
    class SearchRegression {

        @TempDir Path tempDir;
        UserService service;

        @BeforeEach
        void setUp() throws Exception {
            service = newService(tempDir);
            service.create("김철수", "kim@t.com", 30, true, "서울", "강남구", "00001", "");
            service.create("김영수", "kim2@t.com", 25, true, "부산", "해운대구", "00002", "");
            service.create("이민호", "lee@t.com", 28, true, "대구", "수성구", "00003", "");
        }

        @Test
        @DisplayName("이름 전체 일치로 1건 검색된다")
        void exactNameSearch() throws Exception {
            List<User> result = service.searchByName("이민호");
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getEmail()).isEqualTo("lee@t.com");
        }

        @Test
        @DisplayName("이름 부분 일치로 다중 검색된다")
        void partialNameSearchMultipleResults() throws Exception {
            List<User> result = service.searchByName("김");
            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("일치하는 이름 없으면 빈 목록을 반환한다")
        void noMatchReturnsEmpty() throws Exception {
            List<User> result = service.searchByName("존재하지않음");
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("빈 목록에서 검색하면 빈 결과를 반환한다")
        void searchOnEmptyList() throws Exception {
            UserService emptyService = newService(tempDir.resolve("empty"));
            List<User> result = emptyService.searchByName("김");
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("ID로 조회 시 없는 ID는 empty를 반환한다")
        void findByIdNotFound() throws Exception {
            assertThat(service.findById(999L)).isEmpty();
        }
    }

    // ══════════════════════════════════════════════════════
    // 5. 필드 수정 회귀
    // ══════════════════════════════════════════════════════
    @Nested
    @DisplayName("필드 수정 회귀")
    class UpdateFieldRegression {

        @TempDir Path tempDir;
        UserService service;

        @BeforeEach
        void setUp() throws Exception {
            service = newService(tempDir);
            service.create("홍길동", "hong@t.com", 25, false, "서울", "강남구", "06234", "user");
        }

        @Test
        @DisplayName("name 필드 수정")
        void updateName() throws Exception {
            service.updateField(1L, "name", "홍길순");
            assertThat(service.findById(1L).orElseThrow().getName()).isEqualTo("홍길순");
        }

        @Test
        @DisplayName("email 필드 수정")
        void updateEmail() throws Exception {
            service.updateField(1L, "email", "new@t.com");
            assertThat(service.findById(1L).orElseThrow().getEmail()).isEqualTo("new@t.com");
        }

        @Test
        @DisplayName("age 필드 수정")
        void updateAge() throws Exception {
            service.updateField(1L, "age", "40");
            assertThat(service.findById(1L).orElseThrow().getAge()).isEqualTo(40);
        }

        @Test
        @DisplayName("active 필드 수정 - true/false")
        void updateActive() throws Exception {
            service.updateField(1L, "active", "true");
            assertThat(service.findById(1L).orElseThrow().isActive()).isTrue();
            service.updateField(1L, "active", "false");
            assertThat(service.findById(1L).orElseThrow().isActive()).isFalse();
        }

        @Test
        @DisplayName("city / district / zipCode 필드 수정")
        void updateAddressFields() throws Exception {
            service.updateField(1L, "city", "부산");
            service.updateField(1L, "district", "해운대구");
            service.updateField(1L, "zipCode", "48095");

            User user = service.findById(1L).orElseThrow();
            assertThat(user.getAddress().getCity()).isEqualTo("부산");
            assertThat(user.getAddress().getDistrict()).isEqualTo("해운대구");
            assertThat(user.getAddress().getZipCode()).isEqualTo("48095");
        }

        @Test
        @DisplayName("tags 필드 수정")
        void updateTags() throws Exception {
            service.updateField(1L, "tags", "admin, dev, ops");
            List<String> tags = service.findById(1L).orElseThrow().getTags();
            assertThat(tags).containsExactly("admin", "dev", "ops");
        }

        @Test
        @DisplayName("address가 null인 사용자의 address 필드를 수정하면 자동 생성된다")
        void updateAddressWhenNull() throws Exception {
            // address 없이 직접 repository에 저장
            UserRepository repo = new UserRepository(tempDir.resolve("null-addr.json").toString());
            User bare = new User();
            bare.setName("주소없음");
            bare.setEmail("bare@t.com");
            bare.setAge(20);
            repo.save(bare);

            UserService svc = new UserService(repo);
            svc.updateField(1L, "city", "인천");
            assertThat(svc.findById(1L).orElseThrow().getAddress().getCity()).isEqualTo("인천");
        }

        @Test
        @DisplayName("지원하지 않는 필드명은 IllegalArgumentException을 던진다")
        void invalidFieldThrows() {
            assertThatThrownBy(() -> service.updateField(1L, "unknown", "value"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("수정 불가 필드");
        }

        @Test
        @DisplayName("age에 숫자가 아닌 값 입력 시 NumberFormatException을 던진다")
        void ageNonNumericThrows() {
            assertThatThrownBy(() -> service.updateField(1L, "age", "abc"))
                    .isInstanceOf(NumberFormatException.class);
        }
    }

    // ══════════════════════════════════════════════════════
    // 6. 태그 처리 회귀
    // ══════════════════════════════════════════════════════
    @Nested
    @DisplayName("태그 처리 회귀")
    class TagRegression {

        @TempDir Path tempDir;
        UserService service;

        @BeforeEach
        void setUp() throws Exception {
            service = newService(tempDir);
        }

        @Test
        @DisplayName("빈 문자열 태그는 빈 리스트로 저장된다")
        void emptyTagsBecomesEmptyList() throws Exception {
            User u = service.create("A", "a@t.com", 20, true, "서울", "강남구", "00001", "");
            assertThat(u.getTags()).isEmpty();
        }

        @Test
        @DisplayName("단일 태그는 크기 1인 리스트로 저장된다")
        void singleTag() throws Exception {
            User u = service.create("A", "a@t.com", 20, true, "서울", "강남구", "00001", "admin");
            assertThat(u.getTags()).containsExactly("admin");
        }

        @Test
        @DisplayName("쉼표 주변 공백이 trim된다")
        void tagWhitespaceTrimmed() throws Exception {
            User u = service.create("A", "a@t.com", 20, true, "서울", "강남구", "00001", "  admin , dev , ops  ");
            assertThat(u.getTags()).containsExactly("admin", "dev", "ops");
        }

        @Test
        @DisplayName("updateField로 태그 수정 시에도 공백이 trim된다")
        void updateTagWhitespaceTrimmed() throws Exception {
            service.create("A", "a@t.com", 20, true, "서울", "강남구", "00001", "old");
            service.updateField(1L, "tags", "  new1 , new2 ");
            assertThat(service.findById(1L).orElseThrow().getTags()).containsExactly("new1", "new2");
        }

        @Test
        @DisplayName("태그를 빈 문자열로 수정하면 빈 리스트가 된다")
        void updateTagsToEmpty() throws Exception {
            service.create("A", "a@t.com", 20, true, "서울", "강남구", "00001", "admin");
            service.updateField(1L, "tags", "");
            assertThat(service.findById(1L).orElseThrow().getTags()).isEmpty();
        }
    }

    // ══════════════════════════════════════════════════════
    // 7. 경계값 회귀
    // ══════════════════════════════════════════════════════
    @Nested
    @DisplayName("경계값 회귀")
    class BoundaryRegression {

        @TempDir Path tempDir;
        UserService service;

        @BeforeEach
        void setUp() throws Exception {
            service = newService(tempDir);
        }

        @Test
        @DisplayName("age = 0 저장 및 조회가 정상 동작한다")
        void ageZero() throws Exception {
            User u = service.create("A", "a@t.com", 0, true, "서울", "강남구", "00001", "");
            assertThat(service.findById(u.getId()).orElseThrow().getAge()).isZero();
        }

        @Test
        @DisplayName("빈 목록에서 findAll은 빈 리스트를 반환한다")
        void findAllOnEmpty() throws Exception {
            assertThat(service.findAll()).isEmpty();
        }

        @Test
        @DisplayName("빈 목록에서 findById는 empty를 반환한다")
        void findByIdOnEmpty() throws Exception {
            assertThat(service.findById(1L)).isEmpty();
        }

        @Test
        @DisplayName("빈 목록에서 delete는 false를 반환한다")
        void deleteOnEmpty() throws Exception {
            assertThat(service.delete(1L)).isFalse();
        }

        @Test
        @DisplayName("같은 이메일로 여러 사용자를 생성할 수 있다 (중복 허용)")
        void duplicateEmailAllowed() throws Exception {
            service.create("A", "dup@t.com", 20, true, "서울", "강남구", "00001", "");
            service.create("B", "dup@t.com", 21, true, "서울", "강남구", "00001", "");
            assertThat(service.findAll()).hasSize(2);
        }

        @Test
        @DisplayName("createdAt이 자동으로 설정된다")
        void createdAtAutoSet() throws Exception {
            User u = service.create("A", "a@t.com", 20, true, "서울", "강남구", "00001", "");
            assertThat(u.getCreatedAt()).isNotNull();
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
