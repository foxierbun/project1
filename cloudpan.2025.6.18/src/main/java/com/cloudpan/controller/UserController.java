package com.cloudpan.controller;

import com.cloudpan.entity.User;
import com.google.gson.Gson;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.security.UserGroupInformation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.cloudpan.service.UserService;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;

@RestController
@RequestMapping("/user")
public class UserController {
    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        try {
            String result = userService.register(user.getUsername(), user.getPassword());
            if (result.startsWith("注册成功")) {
                // 从返回结果中提取userId
                String[] parts = result.split(",");
                return new ResponseEntity<>(new RegisterResponse("注册成功",parts[1]), HttpStatus.CREATED);
            }
            return new ResponseEntity<>(new RegisterResponse(result, null), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return new ResponseEntity<>(new RegisterResponse("注册失败", null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody User user) {
        try {
            String result = userService.login(user.getUsername(), user.getPassword());
            if (result.startsWith("登录成功")) {
                // 从返回结果中提取userId
                String[] parts = result.split(",");
                return new ResponseEntity<>(new LoginResponse("登录成功", parts[1]), HttpStatus.OK);
            } else if (result.startsWith("密码错误")) {
                return new ResponseEntity<>(new LoginResponse("密码错误", null), HttpStatus.UNAUTHORIZED);
            }
            return new ResponseEntity<>(new LoginResponse(result, null), HttpStatus.UNAUTHORIZED);
        } catch (Exception e) {
            return new ResponseEntity<>(new LoginResponse("登录失败", null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // 通用响应包装类
    private static class UserResponse {
        private String status;
        private String userId;

        public UserResponse(String status, String userId) {
            this.status = status;
            this.userId = userId;
        }

        public String getStatus() {
            return status;
        }

        public String getUserId() {
            return userId;
        }
    }

    private static class LoginResponse extends UserResponse {
        public LoginResponse(String status, String userId) {
            super(status, userId);
        }
    }

    private static class RegisterResponse extends UserResponse {
        public RegisterResponse(String status, String userId) {
            super(status, userId);
        }
    }
}