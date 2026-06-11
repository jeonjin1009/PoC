package com.poc.json.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.poc.json.model.User;
import com.poc.json.parser.JsonFileParser;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserRepository {

    private final File dataFile;
    private final JsonFileParser parser;

    public UserRepository(String dataFilePath) throws IOException {
        this.dataFile = new File(dataFilePath);
        this.parser = new JsonFileParser();
        initDataFile();
    }

    public List<User> findAll() throws IOException {
        return parser.parseFromFile(dataFile, new TypeReference<>() {});
    }

    public Optional<User> findById(long id) throws IOException {
        return findAll().stream()
                .filter(u -> u.getId() == id)
                .findFirst();
    }

    public List<User> findByNameContaining(String keyword) throws IOException {
        if (keyword == null) return List.of();
        return findAll().stream()
                .filter(u -> u.getName() != null && u.getName().contains(keyword))
                .toList();
    }

    /** id 자동 채번 후 저장 */
    public User save(User user) throws IOException {
        List<User> users = findAll();
        long nextId = users.stream().mapToLong(User::getId).max().orElse(0L) + 1;
        user.setId(nextId);
        users.add(user);
        flush(users);
        return user;
    }

    /** id 기준으로 교체 */
    public Optional<User> update(User updated) throws IOException {
        List<User> users = findAll();
        boolean found = false;
        for (int i = 0; i < users.size(); i++) {
            if (users.get(i).getId() == updated.getId()) {
                users.set(i, updated);
                found = true;
                break;
            }
        }
        if (!found) return Optional.empty();
        flush(users);
        return Optional.of(updated);
    }

    public boolean deleteById(long id) throws IOException {
        List<User> users = findAll();
        boolean removed = users.removeIf(u -> u.getId() == id);
        if (removed) flush(users);
        return removed;
    }

    private void flush(List<User> users) throws IOException {
        parser.toJsonFile(users, dataFile);
    }

    private void initDataFile() throws IOException {
        if (!dataFile.exists()) {
            dataFile.getParentFile().mkdirs();
            flush(new ArrayList<>());
        }
    }
}
