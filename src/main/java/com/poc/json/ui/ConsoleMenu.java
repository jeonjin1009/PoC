package com.poc.json.ui;

import com.poc.json.model.Address;
import com.poc.json.model.User;
import com.poc.json.service.UserService;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

public class ConsoleMenu {

    private static final String LINE  = "-".repeat(58);
    private static final String DLINE = "=".repeat(58);

    private final UserService service;
    private final Scanner scanner;

    public ConsoleMenu(UserService service) {
        this.service = service;
        this.scanner = new Scanner(new InputStreamReader(System.in, StandardCharsets.UTF_8));
    }

    public void run() {
        println(DLINE);
        println("  사용자 관리 시스템  (JSON CRUD)");
        println(DLINE);

        while (true) {
            printMainMenu();
            String choice = prompt("선택").trim();
            switch (choice) {
                case "1" -> handleReadAll();
                case "2" -> handleReadById();
                case "3" -> handleSearchByName();
                case "4" -> handleCreate();
                case "5" -> handleUpdate();
                case "6" -> handleDelete();
                case "0" -> { println("\n프로그램을 종료합니다."); return; }
                default  -> println("  [!] 올바른 번호를 입력하세요.");
            }
        }
    }

    // ── 메뉴 출력 ──────────────────────────────────────────

    private void printMainMenu() {
        println("\n" + LINE);
        println("  [1] 전체 목록 보기          (Read All)");
        println("  [2] ID로 검색               (Read by ID)");
        println("  [3] 이름으로 검색            (Search)");
        println("  [4] 사용자 추가              (Create)");
        println("  [5] 사용자 수정              (Update)");
        println("  [6] 사용자 삭제              (Delete)");
        println("  [0] 종료");
        println(LINE);
    }

    // ── 1. Read All ────────────────────────────────────────

    private void handleReadAll() {
        try {
            List<User> users = service.findAll();
            println("\n" + LINE);
            if (users.isEmpty()) {
                println("  등록된 사용자가 없습니다.");
            } else {
                println(String.format("  %-4s %-10s %-24s %-4s %-6s %-8s", "ID", "이름", "이메일", "나이", "활성", "도시"));
                println(LINE);
                for (User u : users) {
                    println(formatUserRow(u));
                }
                println(LINE);
                println("  총 " + users.size() + "명");
            }
        } catch (Exception e) {
            printError(e);
        }
    }

    // ── 2. Read by ID ──────────────────────────────────────

    private void handleReadById() {
        try {
            long id = promptLong("검색할 ID");
            Optional<User> opt = service.findById(id);
            println("\n" + LINE);
            if (opt.isPresent()) {
                printUserDetail(opt.get());
            } else {
                println("  ID " + id + " 에 해당하는 사용자가 없습니다.");
            }
        } catch (Exception e) {
            printError(e);
        }
    }

    // ── 3. Search by Name ──────────────────────────────────

    private void handleSearchByName() {
        try {
            String keyword = prompt("검색할 이름 (부분 일치)");
            List<User> results = service.searchByName(keyword);
            println("\n" + LINE);
            if (results.isEmpty()) {
                println("  \"" + keyword + "\" 와 일치하는 사용자가 없습니다.");
            } else {
                println("  검색 결과: " + results.size() + "명");
                println(LINE);
                for (User u : results) {
                    println(formatUserRow(u));
                }
            }
        } catch (Exception e) {
            printError(e);
        }
    }

    // ── 4. Create ──────────────────────────────────────────

    private void handleCreate() {
        println("\n" + LINE);
        println("  새 사용자 정보를 입력하세요.");
        println(LINE);
        try {
            String name     = prompt("이름");
            String email    = prompt("이메일");
            int    age      = promptInt("나이");
            boolean active  = promptBoolean("활성 여부 (y/n)");
            String city     = prompt("도시");
            String district = prompt("구/군");
            String zipCode  = prompt("우편번호");
            String tags     = prompt("태그 (쉼표 구분, 없으면 Enter)");

            User created = service.create(name, email, age, active, city, district, zipCode, tags);
            println("\n  [✓] 사용자가 추가되었습니다. (ID: " + created.getId() + ")");
            printUserDetail(created);
        } catch (Exception e) {
            printError(e);
        }
    }

    // ── 5. Update ──────────────────────────────────────────

    private void handleUpdate() {
        try {
            long id = promptLong("수정할 사용자 ID");
            Optional<User> opt = service.findById(id);
            if (opt.isEmpty()) {
                println("  [!] ID " + id + " 사용자를 찾을 수 없습니다.");
                return;
            }

            println("\n현재 정보:");
            printUserDetail(opt.get());

            println("\n" + LINE);
            println("  수정할 필드를 선택하세요:");
            println("  [1] name     [2] email    [3] age");
            println("  [4] active   [5] city     [6] district");
            println("  [7] zipCode  [8] tags");
            println(LINE);

            String fieldChoice = prompt("선택").trim();
            String field = switch (fieldChoice) {
                case "1" -> "name";
                case "2" -> "email";
                case "3" -> "age";
                case "4" -> "active";
                case "5" -> "city";
                case "6" -> "district";
                case "7" -> "zipCode";
                case "8" -> "tags";
                default  -> null;
            };

            if (field == null) {
                println("  [!] 올바른 필드 번호를 입력하세요.");
                return;
            }

            String value = prompt("새 값" + (field.equals("active") ? " (true/false)" :
                                            field.equals("tags")   ? " (쉼표 구분)" : ""));
            Optional<User> updated = service.updateField(id, field, value);
            if (updated.isPresent()) {
                println("\n  [✓] 수정이 완료되었습니다.");
                printUserDetail(updated.get());
            }
        } catch (IllegalArgumentException e) {
            println("  [!] 입력 오류: " + e.getMessage());
        } catch (Exception e) {
            printError(e);
        }
    }

    // ── 6. Delete ──────────────────────────────────────────

    private void handleDelete() {
        try {
            long id = promptLong("삭제할 사용자 ID");
            Optional<User> opt = service.findById(id);
            if (opt.isEmpty()) {
                println("  [!] ID " + id + " 사용자를 찾을 수 없습니다.");
                return;
            }

            println("\n삭제 대상:");
            printUserDetail(opt.get());

            String confirm = prompt("\n정말 삭제하시겠습니까? (y/n)").trim().toLowerCase();
            if (!confirm.equals("y")) {
                println("  삭제가 취소되었습니다.");
                return;
            }

            service.delete(id);
            println("  [✓] ID " + id + " 사용자가 삭제되었습니다.");
        } catch (Exception e) {
            printError(e);
        }
    }

    // ── 출력 헬퍼 ──────────────────────────────────────────

    private void printUserDetail(User u) {
        Address addr = u.getAddress();
        println(String.format("  ID       : %d", u.getId()));
        println(String.format("  이름     : %s", u.getName()));
        println(String.format("  이메일   : %s", u.getEmail()));
        println(String.format("  나이     : %d", u.getAge()));
        println(String.format("  활성     : %b", u.isActive()));
        println(String.format("  도시     : %s", addr != null ? addr.getCity() : "-"));
        println(String.format("  구/군    : %s", addr != null ? addr.getDistrict() : "-"));
        println(String.format("  우편번호 : %s", addr != null ? addr.getZipCode() : "-"));
        println(String.format("  태그     : %s", u.getTags() != null ? u.getTags() : List.of()));
        println(String.format("  가입일   : %s", u.getCreatedAt() != null ? u.getCreatedAt() : "-"));
    }

    private String formatUserRow(User u) {
        String city = u.getAddress() != null ? u.getAddress().getCity() : "-";
        return String.format("  %-4d %-10s %-24s %-4d %-6b %-8s",
                u.getId(), u.getName(), u.getEmail(), u.getAge(), u.isActive(), city);
    }

    // ── 입력 헬퍼 ──────────────────────────────────────────

    private String prompt(String label) {
        System.out.print("  " + label + " > ");
        return scanner.nextLine();
    }

    private long promptLong(String label) {
        while (true) {
            try {
                return Long.parseLong(prompt(label).trim());
            } catch (NumberFormatException e) {
                println("  [!] 숫자를 입력하세요.");
            }
        }
    }

    private int promptInt(String label) {
        while (true) {
            try {
                return Integer.parseInt(prompt(label).trim());
            } catch (NumberFormatException e) {
                println("  [!] 숫자를 입력하세요.");
            }
        }
    }

    private boolean promptBoolean(String label) {
        String v = prompt(label).trim().toLowerCase();
        return v.equals("y") || v.equals("yes") || v.equals("true");
    }

    private void println(String msg) {
        System.out.println(msg);
    }

    private void printError(Exception e) {
        println("  [!] 오류 발생: " + e.getMessage());
    }
}
