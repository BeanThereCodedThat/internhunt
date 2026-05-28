package com.internhunt.internhunt.service;

import com.internhunt.internhunt.entity.User;
import com.internhunt.internhunt.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class UserService
{
    @Autowired
    private UserRepository userRepository;

    public User createUser(User user)
    {
        return userRepository.save(user);
    }

    public Optional<User> getUserById(Integer id)
    {
        return userRepository.findById(id);
    }

    public User updateUser(User user)
    {
        return userRepository.save(user);
    }

    public void deleteUser(Integer id)
    {
        userRepository.deleteById(id);
    }

    public List<User> getAllUsers()
    {
        return userRepository.findAll();
    }
}