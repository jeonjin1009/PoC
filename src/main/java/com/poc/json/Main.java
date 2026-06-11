package com.poc.json;

import com.poc.json.repository.UserRepository;
import com.poc.json.service.UserService;
import com.poc.json.ui.ConsoleMenu;

public class Main {

    private static final String DATA_FILE = "data/users.json";

    public static void main(String[] args) throws Exception {
        UserRepository repository = new UserRepository(DATA_FILE);
        UserService    service    = new UserService(repository);
        ConsoleMenu    menu       = new ConsoleMenu(service);
        menu.run();
    }
}
