package com.cloudpan;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CloudpanApplication {
    public static void main(String[] args) {
    	   //跳过加载本地库，配置太麻烦了
    	  System.setProperty("HADOOP_OPTS", "-Djava.library.path=");
        SpringApplication.run(CloudpanApplication.class, args);
    }
}