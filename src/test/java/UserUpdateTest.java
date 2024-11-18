import com.github.javafaker.Faker;
import io.qameta.allure.Step;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class UserUpdateTest {

    private final String BASE_URI = "https://stellarburgers.nomoreparties.site";
    private final String REGISTER_ENDPOINT = "/api/auth/register";
    private final String USER_ENDPOINT = "/api/auth/user";
    private Faker faker;
    private String createdUserToken;
    private String email;
    private String password;

    // Логгер для ключевых этапов
    private static final Logger LOGGER = Logger.getLogger(UserUpdateTest.class.getName());

    @Before
    public void setup() {
        RestAssured.baseURI = BASE_URI;
        faker = new Faker();
        LOGGER.info("Установлен базовый URI для RestAssured");
    }

    @After
    @Step("Удаление созданного пользователя после теста")
    public void tearDown() {
        if (createdUserToken != null) {
            LOGGER.info("Удаление тестового пользователя...");
            deleteUser(createdUserToken);
            LOGGER.info("Пользователь успешно удалён.");
            createdUserToken = null;
        }
    }

    @Test
    public void updateUserEmailWithAuthorization() {
        email = generateUniqueEmail();
        password = generateUniquePassword();
        String name = generateUniqueName();

        LOGGER.info("Регистрация нового пользователя");
        createdUserToken = setupTestUser(email, password, name);

        // Обновляем только email
        String newEmail = generateUniqueEmail();
        LOGGER.info("Попытка обновления email");
        Response updateResponse = updateUser(createdUserToken, newEmail, null);
        validateSuccessfulUpdate(updateResponse, newEmail, name);
    }

    @Test
    public void updateUserNameWithAuthorization() {
        email = generateUniqueEmail();
        password = generateUniquePassword();
        String name = generateUniqueName();

        LOGGER.info("Регистрация нового пользователя");
        createdUserToken = setupTestUser(email, password, name);

        // Обновляем только имя
        String newName = generateUniqueName();
        LOGGER.info("Попытка обновления имени");
        Response updateResponse = updateUser(createdUserToken, null, newName);
        validateSuccessfulUpdate(updateResponse, email, newName);
    }

    @Test
    public void updateUserEmailWithoutAuthorization() {
        String newEmail = generateUniqueEmail();
        LOGGER.warning("Попытка обновления email без авторизации");
        Response updateResponse = updateUser(null, newEmail, null);
        validateUnauthorizedUpdate(updateResponse);
    }

    @Test
    public void updateUserNameWithoutAuthorization() {
        String newName = generateUniqueName();
        LOGGER.warning("Попытка обновления имени без авторизации");
        Response updateResponse = updateUser(null, null, newName);
        validateUnauthorizedUpdate(updateResponse);
    }

    @Test
    public void updateUserToExistingEmail() {
        String email1 = generateUniqueEmail();
        String password1 = generateUniquePassword();
        String name1 = generateUniqueName();
        LOGGER.info("Регистрация первого пользователя");
        String token1 = setupTestUser(email1, password1, name1);

        email = generateUniqueEmail();
        password = generateUniquePassword();
        String name = generateUniqueName();
        LOGGER.info("Регистрация второго пользователя");
        createdUserToken = setupTestUser(email, password, name);

        LOGGER.warning("Попытка обновления email второго пользователя на email первого");
        Response updateResponse = updateUser(createdUserToken, email1, null);
        validateEmailAlreadyExistsError(updateResponse);

        deleteUser(token1);
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

    @Step("Регистрация пользователя")
    private Response registerUser(String email, String password, String name) {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("email", email);
        requestBody.put("password", password);
        requestBody.put("name", name);

        LOGGER.info("Отправка запроса на регистрацию");
        Response response = given()
                .header("Content-Type", "application/json")
                .body(requestBody)
                .when()
                .post(REGISTER_ENDPOINT);
        logResponse(response);
        return response;
    }

    @Step("Обновление данных пользователя")
    private Response updateUser(String token, String newEmail, String newName) {
        Map<String, String> requestBody = new HashMap<>();
        if (newEmail != null) {
            requestBody.put("email", newEmail);
        }
        if (newName != null) {
            requestBody.put("name", newName);
        }

        LOGGER.info("Отправка запроса на обновление данных");
        Response response = given()
                .header("Content-Type", "application/json")
                .header("Authorization", token != null ? token : "")
                .body(requestBody)
                .when()
                .patch(USER_ENDPOINT);
        logResponse(response);
        return response;
    }

    @Step("Настройка тестового пользователя")
    private String setupTestUser(String email, String password, String name) {
        Response registrationResponse = registerUser(email, password, name);
        validateSuccessfulRegistration(registrationResponse, email, name);
        return registrationResponse.jsonPath().getString("accessToken");
    }

    @Step("Проверка успешной регистрации пользователя")
    private void validateSuccessfulRegistration(Response response, String email, String name) {
        response.then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("accessToken", startsWith("Bearer "))
                .body("refreshToken", notNullValue())
                .body("user.email", equalTo(email))
                .body("user.name", equalTo(name));
    }

    @Step("Проверка успешного обновления данных пользователя")
    private void validateSuccessfulUpdate(Response response, String newEmail, String newName) {
        response.then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("user.email", newEmail != null ? equalTo(newEmail) : notNullValue())
                .body("user.name", newName != null ? equalTo(newName) : notNullValue());
    }

    @Step("Проверка ошибки при обновлении без авторизации")
    private void validateUnauthorizedUpdate(Response response) {
        response.then()
                .statusCode(401)
                .body("success", equalTo(false))
                .body("message", equalTo("You should be authorised"));
    }

    @Step("Проверка ошибки при обновлении email на существующий")
    private void validateEmailAlreadyExistsError(Response response) {
        response.then()
                .statusCode(403)
                .body("success", equalTo(false))
                .body("message", equalTo("User with such email already exists"));
    }

    @Step("Удаление пользователя")
    private void deleteUser(String token) {
        LOGGER.info("Отправка запроса на удаление пользователя");
        Response response = given()
                .header("Authorization", token)
                .when()
                .delete(USER_ENDPOINT);
        logResponse(response);
        response.then().statusCode(202);
    }

    @Step("Логирование ответа сервера")
    private void logResponse(Response response) {
        LOGGER.info("Ответ сервера - Статус: " + response.getStatusCode());
        LOGGER.info("Ответ сервера - Тело: " + response.getBody().asString());
    }
}
