import com.github.javafaker.Faker;
import io.qameta.allure.Step;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class UserCreationTest {

    private final String BASE_URI = "https://stellarburgers.nomoreparties.site";
    private final String REGISTER_ENDPOINT = "/api/auth/register";
    private final String DELETE_ENDPOINT = "/api/auth/user";
    private Faker faker;
    private String createdUserToken;

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
    public void createUniqueUser() {
        String email = generateUniqueEmail();
        String password = generateUniquePassword();
        String name = generateUniqueName();

        Response response = registerUser(email, password, name);

        // Проверяем структуру и данные успешного ответа
        validateFullSuccessfulResponse(response, email, name);

        // Сохраняем токен для удаления пользователя
        createdUserToken = response.jsonPath().getString("accessToken");
    }

    @Test
    public void createUserAlreadyRegistered() {
        String email = generateUniqueEmail();
        String password = generateUniquePassword();
        String name = generateUniqueName();

        // Регистрируем первого пользователя
        Response firstResponse = registerUser(email, password, name);
        validateFullSuccessfulResponse(firstResponse, email, name);

        // Сохраняем токен для удаления
        createdUserToken = firstResponse.jsonPath().getString("accessToken");

        // Пытаемся зарегистрировать того же пользователя второй раз
        Response secondResponse = registerUser(email, password, name);

        // Проверяем структуру и данные ошибки
        validateErrorResponse(secondResponse, 403, "User already exists");
    }

    @Test
    public void createUserMissingName() {
        String email = generateUniqueEmail();
        String password = generateUniquePassword();

        Response response = registerUser(email, password, null);

        // Проверяем структуру и данные ошибки
        validateErrorResponse(response, 403, "Email, password and name are required fields");
    }

    @Test
    public void createUserMissingEmail() {
        String password = generateUniquePassword();
        String name = generateUniqueName();

        Response response = registerUser(null, password, name);

        // Проверяем структуру и данные ошибки
        validateErrorResponse(response, 403, "Email, password and name are required fields");
    }

    @Test
    public void createUserMissingPassword() {
        String email = generateUniqueEmail();
        String name = generateUniqueName();

        Response response = registerUser(email, null, name);

        // Проверяем структуру и данные ошибки
        validateErrorResponse(response, 403, "Email, password and name are required fields");
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
                "{ \"email\": %s, \"password\": %s, \"name\": %s }",
                email != null ? "\"" + email + "\"" : null,
                password != null ? "\"" + password + "\"" : null,
                name != null ? "\"" + name + "\"" : null
        );
        return given()
                .header("Content-Type", "application/json")
                .body(requestBody)
                .when()
                .post(REGISTER_ENDPOINT);
    }

    @Step("Проверка полной структуры успешного ответа для пользователя {email}")
    private void validateFullSuccessfulResponse(Response response, String email, String name) {
        response.then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("user.email", equalTo(email))
                .body("user.name", equalTo(name))
                .body("accessToken", notNullValue())
                .body("refreshToken", notNullValue());
    }

    @Step("Проверка структуры ответа с ошибкой")
    private void validateErrorResponse(Response response, int statusCode, String errorMessage) {
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
                .statusCode(202); // Успешное удаление
    }
}
