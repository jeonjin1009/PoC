package com.poc.json;

import com.poc.json.repository.UserRepository;
import com.poc.json.service.UserService;
import com.poc.json.ui.ConsoleMenu;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

public class Main {

    private static final String DATA_FILE = "data/users.json";

    public static void main(String[] args) throws Exception {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(System.err, true, StandardCharsets.UTF_8));

        UserRepository repository = new UserRepository(DATA_FILE);
        UserService    service    = new UserService(repository);
        ConsoleMenu    menu       = new ConsoleMenu(service);
        menu.run();
    }
}
