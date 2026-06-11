package com.poc.json;

import com.poc.json.model.User;
import com.poc.json.repository.UserRepository;
import com.poc.json.service.UserService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UserCrudTest {

    @TempDir
    static Path tempDir;

    static UserService service;

    @BeforeAll
    static void setUp() throws Exception {
        String dataFile = tempDir.resolve("users.json").toString();
        service = new UserService(new UserRepository(dataFile));
    }

    @Test
    @Order(1)
    @DisplayName("Create - 사용자 추가 및 ID 자동 채번")
    void create() throws Exception {
        User u1 = service.create("김철수", "kim@test.com", 30, true, "서울", "강남구", "06234", "admin,dev");
        User u2 = service.create("이영희", "lee@test.com", 25, false, "부산", "해운대구", "48095", "user");

        assertThat(u1.getId()).isEqualTo(1L);
        assertThat(u2.getId()).isEqualTo(2L);
        assertThat(u1.getTags()).containsExactly("admin", "dev");
        assertThat(u1.getCreatedAt()).isNotNull();
    }

    @Test
    @Order(2)
    @DisplayName("Read All - 전체 목록 조회")
    void readAll() throws Exception {
        List<User> users = service.findAll();
        assertThat(users).hasSize(2);
    }

    @Test
    @Order(3)
    @DisplayName("Read by ID - ID로 단건 조회")
    void readById() throws Exception {
        Optional<User> found = service.findById(1L);
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("김철수");
    }

    @Test
    @Order(4)
    @DisplayName("Search - 이름 부분 일치 검색")
    void searchByName() throws Exception {
        List<User> results = service.searchByName("영희");
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getEmail()).isEqualTo("lee@test.com");
    }

    @Test
    @Order(5)
    @DisplayName("Update - 단일 필드 수정")
    void update() throws Exception {
        service.updateField(1L, "email", "new@test.com");
        service.updateField(1L, "age", "35");
        service.updateField(1L, "city", "대전");

        User updated = service.findById(1L).orElseThrow();
        assertThat(updated.getEmail()).isEqualTo("new@test.com");
        assertThat(updated.getAge()).isEqualTo(35);
        assertThat(updated.getAddress().getCity()).isEqualTo("대전");
    }

    @Test
    @Order(6)
    @DisplayName("Update - 존재하지 않는 ID는 empty 반환")
    void updateNotFound() throws Exception {
        Optional<User> result = service.updateField(999L, "name", "ghost");
        assertThat(result).isEmpty();
    }

    @Test
    @Order(7)
    @DisplayName("Delete - 사용자 삭제")
    void delete() throws Exception {
        boolean deleted = service.delete(2L);
        assertThat(deleted).isTrue();
        assertThat(service.findAll()).hasSize(1);
        assertThat(service.findById(2L)).isEmpty();
    }

    @Test
    @Order(8)
    @DisplayName("Delete - 존재하지 않는 ID는 false 반환")
    void deleteNotFound() throws Exception {
        boolean result = service.delete(999L);
        assertThat(result).isFalse();
    }
}
