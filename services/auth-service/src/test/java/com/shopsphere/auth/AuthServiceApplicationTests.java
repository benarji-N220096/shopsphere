package com.shopsphere.auth;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false",
        "spring.datasource.url=jdbc:mysql://localhost:3306/shopsphere_auth?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true",
        "spring.datasource.username=root",
        "spring.datasource.password=bunny6281",
        "spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver"
})
class AuthServiceApplicationTests {

    @Test
    void contextLoads() {
    }

}
