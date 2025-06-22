package com.cloudpan.service;

import com.google.gson.Gson;
import java.util.UUID;
import com.cloudpan.entity.User;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.security.UserGroupInformation;
import org.springframework.stereotype.Service;
import org.apache.hadoop.fs.FSDataInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.nio.charset.StandardCharsets;

@Service
public class UserService {
    private static final String USER_DIR = "hdfs://mycluster/user_data";
    //注册部分
    public String register(String username, String password) {
        Configuration conf = new Configuration();
        conf.addResource(new Path("src/main/resources/hadoop/core-site.xml"));
        conf.addResource(new Path("src/main/resources/hadoop/hdfs-site.xml"));
        conf.addResource(new Path("src/main/resources/hadoop/mapred-site.xml"));
        conf.addResource(new Path("src/main/resources/hadoop/yarn-site.xml"));
        try (FileSystem fs = FileSystem.get(URI.create("hdfs://mycluster"), conf)) {
            Path userDir = new Path(USER_DIR);
            if (!fs.exists(userDir)) {
                fs.mkdirs(userDir);

            }
            Path userFile = new Path(USER_DIR + "/" + username + ".json");
            if (fs.exists(userFile)) {
                return "用户名已存在";
            }
            // 生成userId
            String userId = UUID.randomUUID().toString();
            User user = new User(username, password, userId);
            Gson gson = new Gson();
            String userJson = gson.toJson(user);
            try (OutputStreamWriter writer = new OutputStreamWriter(fs.create(userFile), StandardCharsets.UTF_8)) {
                writer.write(userJson);
            }
            return "注册成功," + userId;
        } catch (IOException e) {
            System.out.println("测试失败!!!");
            System.out.println(e.getMessage());
            return "注册失败，请检查网络或稍后重试";

        }
    }


    public String login(String username, String password) {
        Configuration conf = new Configuration();
        conf.addResource(new Path("src/main/resources/hadoop/core-site.xml"));
        conf.addResource(new Path("src/main/resources/hadoop/hdfs-site.xml"));
        conf.addResource(new Path("src/main/resources/hadoop/mapred-site.xml"));
        conf.addResource(new Path("src/main/resources/hadoop/yarn-site.xml"));
        try (FileSystem fs = FileSystem.get(URI.create("hdfs://mycluster"), conf)) {
            Path userFile = new Path(USER_DIR + "/" + username + ".json");
            if (!fs.exists(userFile)) {
                return "用户名不存在";
            }

            try (ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                 FSDataInputStream inputStream = fs.open(userFile)) {
                int nRead;
                byte[] data = new byte[1024];
                while ((nRead = inputStream.read(data, 0, data.length))!= -1) {
                    buffer.write(data, 0, nRead);
                }
                byte[] bytes = buffer.toByteArray();
                String content = new String(bytes);
                User storedUser = User.fromJson(content);
                if (storedUser.getPassword().equals(password)) {
                    // 返回userId
                    return "登录成功," + storedUser.getUserId();
                }
                return "密码错误";
            }
        } catch (IOException e) {
            return "登录失败，请检查网络或稍后重试"+ e.getMessage();
        }
    }
}