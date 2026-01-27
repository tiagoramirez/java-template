package com.tiagoramirez.template.health.integration;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class HealthTest {

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
        RestAssured.basePath = "/api"; // si tu API tiene un prefijo
    }

    @Test
    void healthCheck_ShouldReturnOkStatusAndHealthMessage() {
        given()
                .contentType(ContentType.JSON)
                .when()
                .get("/health")
                .then()
                .statusCode(200)
                .body("message", equalTo("I'm alive!"))
                .and()
                .body("argentina_time", notNullValue());
    }
}
