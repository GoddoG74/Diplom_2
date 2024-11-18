import com.github.javafaker.Faker;
import io.qameta.allure.Step;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class UserLoginTest {

    private final String BASE_URI = "https://stellarburgers.nomoreparties.site";
    private final String REGISTER_ENDPOINT = "/api/auth/register";
    private final String LOGIN_ENDPOINT = "/api/auth/login";
    private final String DELETE_ENDPOINT = "/api/auth/user";
    private Faker faker;
    private String createdUserToken;
    private String email;
    private String password;

    @Before
    public void setup() {
        RestAssured.baseURI = BASE_URI;
        faker = new Faker();
    }

    @After
    @Step("Удаление созданного пользователя после теста")
    public void tearDown() {
        if (createdUserToken != null) {
            deleteUser(createdUserToken);
            createdUserToken = null;
        }
    }

    @Test
    public void loginWithExistingUser() {
        email = generateUniqueEmail();
        password = generateUniquePassword();
        String name = generateUniqueName();

        // Регистрируем нового пользователя
        Response registrationResponse = registerUser(email, password, name);
        validateFullSuccessfulRegistration(registrationResponse, email, name);

        createdUserToken = registrationResponse.jsonPath().getString("accessToken");

        // Логинимся с корректными данными
        Response loginResponse = loginUser(email, password);
        validateFullSuccessfulLogin(loginResponse, email, name);
    }

    @Test
    public void loginWithCorrectEmailAndIncorrectPassword() {
        email = generateUniqueEmail();
        password = generateUniquePassword();
        String name = generateUniqueName();

        // Регистрируем нового пользователя
        Response registrationResponse = registerUser(email, password, name);
        validateFullSuccessfulRegistration(registrationResponse, email, name);

        createdUserToken = registrationResponse.jsonPath().getString("accessToken");

        // Логинимся с правильным email и неправильным паролем
        String invalidPassword = generateUniquePassword();
        Response loginResponse = loginUser(email, invalidPassword);
        validateLoginError(loginResponse, 401, "email or password are incorrect");
    }

    @Test
    public void loginWithIncorrectEmailAndCorrectPassword() {
        email = generateUniqueEmail();
        password = generateUniquePassword();
        String name = generateUniqueName();

        // Регистрируем нового пользователя
        Response registrationResponse = registerUser(email, password, name);
        validateFullSuccessfulRegistration(registrationResponse, email, name);

        createdUserToken = registrationResponse.jsonPath().getString("accessToken");

        // Логинимся с неправильным email и правильным паролем
        String invalidEmail = generateUniqueEmail();
        Response loginResponse = loginUser(invalidEmail, password);
        validateLoginError(loginResponse, 401, "email or password are incorrect");
    }

    // Методы

    @Step("Генерация уникального email")
    private String generateUniqueEmail() {
        return faker.internet().emailAddress();
    }

    @Step("Генерация уникального пароля")
    private String generateUniquePassword() {
        return faker.internet().password(8, 16);
    }

    @Step("Генерация уникального имени")
    private String generateUniqueName() {
        return faker.name().firstName();
    }

    @Step("Регистрация пользователя с email {email}, паролем {password} и именем {name}")
    private Response registerUser(String email, String password, String name) {
        String requestBody = String.format(
                "{ \"email\": \"%s\", \"password\": \"%s\", \"name\": \"%s\" }",
                email, password, name
        );
        return given()
                .header("Content-Type", "application/json")
                .body(requestBody)
                .when()
                .post(REGISTER_ENDPOINT);
    }

    @Step("Логин пользователя с email {email} и паролем {password}")
    private Response loginUser(String email, String password) {
        String requestBody = String.format(
                "{ \"email\": \"%s\", \"password\": \"%s\" }",
                email, password
        );
        return given()
                .header("Content-Type", "application/json")
                .body(requestBody)
                .when()
                .post(LOGIN_ENDPOINT);
    }

    @Step("Проверка полной структуры успешного ответа для регистрации")
    private void validateFullSuccessfulRegistration(Response response, String email, String name) {
        response.then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("user.email", equalTo(email))
                .body("user.name", equalTo(name))
                .body("accessToken", startsWith("Bearer "))
                .body("refreshToken", notNullValue());
    }

    @Step("Проверка полной структуры успешного ответа для логина")
    private void validateFullSuccessfulLogin(Response response, String email, String name) {
        response.then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("user.email", equalTo(email))
                .body("user.name", equalTo(name))
                .body("accessToken", startsWith("Bearer "))
                .body("refreshToken", notNullValue());
    }

    @Step("Проверка ошибки при логине")
    private void validateLoginError(Response response, int statusCode, String errorMessage) {
        response.then()
                .statusCode(statusCode)
                .body("success", equalTo(false))
                .body("message", equalTo(errorMessage));
    }

    @Step("Удаление пользователя с токеном {token}")
    private void deleteUser(String token) {
        given()
                .header("Authorization", token)
                .when()
                .delete(DELETE_ENDPOINT)
                .then()
                .statusCode(202);
    }
}
