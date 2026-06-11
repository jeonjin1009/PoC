package com.poc.json;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.poc.json.model.Order;
import com.poc.json.model.OrderItem;
import com.poc.json.model.User;
import com.poc.json.parser.JsonFileParser;

import java.util.List;

public class Main {

    public static void main(String[] args) throws Exception {
        JsonFileParser parser = new JsonFileParser();

        System.out.println("=".repeat(60));
        System.out.println(" Jackson JSON 파싱 PoC");
        System.out.println("=".repeat(60));

        demo1_parseUserList(parser);
        demo2_parseOrder(parser);
        demo3_treeModel(parser);
        demo4_serializeToJson(parser);
        demo5_parseFromString(parser);
    }

    /** 1. JSON 배열 → List<User> 역직렬화 */
    static void demo1_parseUserList(JsonFileParser parser) throws Exception {
        System.out.println("\n[Demo 1] JSON 배열 파싱 → List<User>");
        System.out.println("-".repeat(40));

        List<User> users = parser.parseFromResource("users.json", new TypeReference<>() {});
        System.out.printf("파싱된 사용자 수: %d명%n", users.size());

        for (User user : users) {
            System.out.println("  " + user);
        }

        User first = users.get(0);
        System.out.printf("%n첫 번째 사용자 상세:%n");
        System.out.printf("  이름: %s%n", first.getName());
        System.out.printf("  이메일: %s%n", first.getEmail());
        System.out.printf("  주소: %s%n", first.getAddress());
        System.out.printf("  가입일: %s%n", first.getCreatedAt());
        System.out.printf("  태그: %s%n", first.getTags());
    }

    /** 2. JSON 객체 → Order POJO 역직렬화 + 중첩 컬렉션 */
    static void demo2_parseOrder(JsonFileParser parser) throws Exception {
        System.out.println("\n[Demo 2] 중첩 객체 파싱 → Order");
        System.out.println("-".repeat(40));

        Order order = parser.parseFromResource("order.json", Order.class);
        System.out.println("  " + order);

        System.out.println("\n  주문 항목:");
        long calcTotal = 0;
        for (OrderItem item : order.getItems()) {
            System.out.println("    " + item);
            calcTotal += item.getTotalPrice();
        }
        System.out.printf("  계산된 합계: %,d원 (JSON 값: %,d원)%n", calcTotal, order.getTotalAmount());

        System.out.println("\n  메타데이터:");
        order.getMetadata().forEach((k, v) -> System.out.printf("    %s = %s%n", k, v));
    }

    /** 3. JsonNode (트리 모델) — 스키마를 모를 때 동적 접근 */
    static void demo3_treeModel(JsonFileParser parser) throws Exception {
        System.out.println("\n[Demo 3] JsonNode 트리 모델로 동적 접근");
        System.out.println("-".repeat(40));

        JsonNode root = parser.parseAsTree("order.json");

        System.out.printf("  orderId : %s%n", root.path("orderId").asText());
        System.out.printf("  status  : %s%n", root.path("status").asText());
        System.out.printf("  items 수: %d%n", root.path("items").size());

        System.out.println("\n  items 순회:");
        for (JsonNode item : root.path("items")) {
            System.out.printf("    - %s x%d @ %,d원%n",
                    item.path("name").asText(),
                    item.path("quantity").asInt(),
                    item.path("unitPrice").asLong());
        }

        // 필드 존재 여부 확인
        boolean hasCampaign = root.path("metadata").has("campaignCode");
        System.out.printf("%n  campaignCode 존재 여부: %b%n", hasCampaign);
    }

    /** 4. POJO → JSON 직렬화 */
    static void demo4_serializeToJson(JsonFileParser parser) throws Exception {
        System.out.println("\n[Demo 4] POJO → JSON 직렬화");
        System.out.println("-".repeat(40));

        OrderItem item = new OrderItem();
        item.setProductId("PROD-NEW");
        item.setName("USB-C 허브");
        item.setQuantity(3);
        item.setUnitPrice(59000);

        String json = parser.toJson(item);
        System.out.println("  직렬화 결과:");
        System.out.println(json.indent(4));
    }

    /** 5. JSON 문자열 직접 파싱 */
    static void demo5_parseFromString(JsonFileParser parser) throws Exception {
        System.out.println("[Demo 5] JSON 문자열 파싱");
        System.out.println("-".repeat(40));

        String rawJson = """
                {
                  "id": 99,
                  "name": "박민준",
                  "email": "minjun@example.com",
                  "age": 28,
                  "active": true,
                  "address": { "city": "대전", "district": "유성구", "zipCode": "34130" },
                  "tags": ["tester"]
                }
                """;

        User user = parser.parseFromString(rawJson, User.class);
        System.out.println("  파싱 결과: " + user);

        // 특정 필드만 추출
        String city = parser.extractField(rawJson, "address.city");
        System.out.println("  address.city 추출: " + city);

        System.out.println("\n" + "=".repeat(60));
        System.out.println(" PoC 완료");
        System.out.println("=".repeat(60));
    }
}
