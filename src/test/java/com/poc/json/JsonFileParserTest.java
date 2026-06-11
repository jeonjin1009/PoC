package com.poc.json;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.poc.json.model.Order;
import com.poc.json.model.User;
import com.poc.json.parser.JsonFileParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JsonFileParserTest {

    private final JsonFileParser parser = new JsonFileParser();

    @Test
    @DisplayName("users.json → List<User> 파싱")
    void parseUserList() throws Exception {
        List<User> users = parser.parseFromResource("users.json", new TypeReference<>() {});

        assertThat(users).hasSize(2);
        assertThat(users.get(0).getName()).isEqualTo("김철수");
        assertThat(users.get(0).getAddress().getCity()).isEqualTo("서울");
        assertThat(users.get(0).getTags()).containsExactly("admin", "developer");
        assertThat(users.get(1).isActive()).isFalse();
    }

    @Test
    @DisplayName("order.json → Order POJO 파싱")
    void parseOrder() throws Exception {
        Order order = parser.parseFromResource("order.json", Order.class);

        assertThat(order.getOrderId()).isEqualTo("ORD-2024-001");
        assertThat(order.getStatus()).isEqualTo("COMPLETED");
        assertThat(order.getItems()).hasSize(2);
        assertThat(order.getMetadata()).containsEntry("source", "mobile");
    }

    @Test
    @DisplayName("주문 항목 합계 계산 검증")
    void orderItemTotalPrice() throws Exception {
        Order order = parser.parseFromResource("order.json", Order.class);

        long calcTotal = order.getItems().stream()
                .mapToLong(item -> (long) item.getQuantity() * item.getUnitPrice())
                .sum();

        assertThat(calcTotal).isEqualTo(order.getTotalAmount());
    }

    @Test
    @DisplayName("JsonNode 트리 모델로 필드 접근")
    void parseAsTree() throws Exception {
        JsonNode root = parser.parseAsTree("order.json");

        assertThat(root.path("orderId").asText()).isEqualTo("ORD-2024-001");
        assertThat(root.path("items").isArray()).isTrue();
        assertThat(root.path("items").size()).isEqualTo(2);
        assertThat(root.path("metadata").path("campaignCode").asText()).isEqualTo("SUMMER2024");
    }

    @Test
    @DisplayName("JSON 문자열 파싱")
    void parseFromString() throws Exception {
        String json = "{\"id\":1,\"name\":\"테스터\",\"email\":\"test@test.com\",\"age\":20,\"active\":true}";
        User user = parser.parseFromString(json, User.class);

        assertThat(user.getName()).isEqualTo("테스터");
        assertThat(user.getAge()).isEqualTo(20);
    }

    @Test
    @DisplayName("중첩 필드 경로 추출")
    void extractNestedField() throws Exception {
        String json = "{\"user\":{\"address\":{\"city\":\"대구\"}}}";
        String city = parser.extractField(json, "user.address.city");

        assertThat(city).isEqualTo("대구");
    }

    @Test
    @DisplayName("POJO를 JSON으로 직렬화 후 재파싱")
    void serializeAndDeserialize() throws Exception {
        List<User> original = parser.parseFromResource("users.json", new TypeReference<>() {});
        String json = parser.toJson(original);
        List<User> restored = parser.parseFromString(json, new TypeReference<>() {});

        assertThat(restored).hasSameSizeAs(original);
        assertThat(restored.get(0).getName()).isEqualTo(original.get(0).getName());
    }

    @Test
    @DisplayName("존재하지 않는 리소스 접근 시 예외 발생")
    void resourceNotFound() {
        assertThatThrownBy(() -> parser.parseFromResource("nonexistent.json", User.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("리소스를 찾을 수 없습니다");
    }
}
