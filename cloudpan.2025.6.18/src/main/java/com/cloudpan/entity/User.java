package com.cloudpan.entity;

import com.google.gson.Gson;

public class User {
    private String username;
    private String password;
    private String userId;

    // 添加无参构造函数
    public User() {
    }

    // 保留原有的构造函数，用于传递完整参数
    public User(String username, String password) {
        this(username, password, null);
    }

    public User(String username, String password, String userId) {
        if (username == null) {
            throw new IllegalArgumentException("用户名不能为 null");
        }
        if (password == null) {
            throw new IllegalArgumentException("密码不能为 null");
        }
        this.username = username;
        this.password = password;
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getUserId() {
        return userId;
    }

    public void setUsername(String username) {this.username = username;}

    public void setPassword(String password) {this.password = password;}

    public void setUserId(String userId) {this.userId = userId;}

    public String toJson() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    public static User fromJson(String json) {
        Gson gson = new Gson();
        return gson.fromJson(json, User.class);
    }
}