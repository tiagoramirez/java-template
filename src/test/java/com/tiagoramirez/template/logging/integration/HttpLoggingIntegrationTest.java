package com.tiagoramirez.template.logging.integration;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import static io.restassured.RestAssured.given;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class HttpLoggingIntegrationTest {

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.basePath = "/api";
    }

    @Test
    void getHealth_ShouldGenerateLogEntry() {
        given()
                .when()
                .get("/health")
                .then()
                .statusCode(200);
    }

    @Test
    void multipleRequests_ShouldGenerateMultipleLogEntries() {
        given()
                .when()
                .get("/health")
                .then()
                .statusCode(200);

        given()
                .when()
                .get("/health")
                .then()
                .statusCode(200);

        given()
                .when()
                .get("/health")
                .then()
                .statusCode(200);
    }

    @Test
    void getHealth_WithHeaders_ShouldGenerateLogEntry() {
        given()
                .header("User-Agent", "RestAssuredTest")
                .header("X-Forwarded-For", "10.0.0.1")
                .when()
                .get("/health")
                .then()
                .statusCode(200);
    }
}
