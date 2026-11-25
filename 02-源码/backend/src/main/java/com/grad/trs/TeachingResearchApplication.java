package com.grad.trs;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 教师教研活动与成果管理系统启动类
 */
@SpringBootApplication
@MapperScan("com.grad.trs.mapper")
public class TeachingResearchApplication {

    public static void main(String[] args) {
        SpringApplication.run(TeachingResearchApplication.class, args);
    }
}
