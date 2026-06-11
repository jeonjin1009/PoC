package com.poc.json.service;

import com.poc.json.model.Address;
import com.poc.json.model.User;
import com.poc.json.repository.UserRepository;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class UserService {

    private final UserRepository repository;

    public UserService(UserRepository repository) {
        this.repository = repository;
    }

    public List<User> findAll() throws IOException {
        return repository.findAll();
    }

    public Optional<User> findById(long id) throws IOException {
        return repository.findById(id);
    }

    public List<User> searchByName(String keyword) throws IOException {
        return repository.findByNameContaining(keyword);
    }

    public User create(String name, String email, int age, boolean active,
                       String city, String district, String zipCode,
                       String rawTags) throws IOException {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setAge(age);
        user.setActive(active);
        user.setCreatedAt(LocalDateTime.now());

        Address address = new Address();
        address.setCity(city);
        address.setDistrict(district);
        address.setZipCode(zipCode);
        user.setAddress(address);

        List<String> tags = rawTags.isBlank()
                ? List.of()
                : Arrays.stream(rawTags.split(",")).map(String::trim).toList();
        user.setTags(tags);

        return repository.save(user);
    }

    /**
     * 단일 필드 수정.
     * 지원 필드: name, email, age, active, city, district, zipCode, tags
     */
    public Optional<User> updateField(long id, String field, String value) throws IOException {
        Optional<User> opt = repository.findById(id);
        if (opt.isEmpty()) return Optional.empty();

        User user = opt.get();
        switch (field) {
            case "name"     -> user.setName(value);
            case "email"    -> user.setEmail(value);
            case "age"      -> user.setAge(Integer.parseInt(value));
            case "active"   -> user.setActive(Boolean.parseBoolean(value));
            case "city"     -> ensureAddress(user).setCity(value);
            case "district" -> ensureAddress(user).setDistrict(value);
            case "zipCode"  -> ensureAddress(user).setZipCode(value);
            case "tags"     -> user.setTags(
                    value.isBlank() ? List.of()
                            : Arrays.stream(value.split(",")).map(String::trim).toList()
            );
            default -> throw new IllegalArgumentException("수정 불가 필드: " + field);
        }
        return repository.update(user);
    }

    public boolean delete(long id) throws IOException {
        return repository.deleteById(id);
    }

    private Address ensureAddress(User user) {
        if (user.getAddress() == null) user.setAddress(new Address());
        return user.getAddress();
    }
}
