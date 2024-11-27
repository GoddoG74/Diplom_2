import io.qameta.allure.Description;
import io.restassured.response.Response;
import org.junit.Test;

import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

public class UserCreationTest extends BaseTest {

    @Test
    @Description("Создание уникального пользователя")
    public void createUniqueUser() {
        // Генерация тестовых данных
        String email = TestDataGenerator.generateEmail();
        String password = TestDataGenerator.generatePassword();
        String name = TestDataGenerator.generateName();

        // Регистрация пользователя
        Response response = ApiSteps.registerUser(email, password, name);

        // Проверка успешного ответа
        response.then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("user.email", equalTo(email))
                .body("user.name", equalTo(name))
                .body("accessToken", notNullValue())
                .body("refreshToken", notNullValue());

        // Сохраняем токен для удаления пользователя
        token = response.jsonPath().getString("accessToken");
    }

    @Test
    @Description("Попытка повторной регистрации уже существующего пользователя")
    public void createUserAlreadyRegistered() {
        // Генерация тестовых данных
        String email = TestDataGenerator.generateEmail();
        String password = TestDataGenerator.generatePassword();
        String name = TestDataGenerator.generateName();

        // Первая регистрация
        Response firstResponse = ApiSteps.registerUser(email, password, name);
        firstResponse.then().statusCode(200);

        // Сохраняем токен для удаления пользователя
        token = firstResponse.jsonPath().getString("accessToken");

        // Повторная регистрация с теми же данными
        Response secondResponse = ApiSteps.registerUser(email, password, name);

        // Проверка ошибки
        secondResponse.then()
                .statusCode(403)
                .body("success", equalTo(false))
                .body("message", equalTo("User already exists"));
    }

    @Test
    @Description("Попытка регистрации пользователя без имени")
    public void createUserMissingName() {
        // Генерация тестовых данных
        String email = TestDataGenerator.generateEmail();
        String password = TestDataGenerator.generatePassword();

        // Регистрация без имени
        Map<String, String> userWithoutName = Map.of(
                "email", email,
                "password", password
        );
        Response response = ApiSteps.registerUser(userWithoutName);

        // Проверка ошибки
        response.then()
                .statusCode(403)
                .body("success", equalTo(false))
                .body("message", equalTo("Email, password and name are required fields"));
    }

    @Test
    @Description("Попытка регистрации пользователя без email")
    public void createUserMissingEmail() {
        // Генерация тестовых данных
        String password = TestDataGenerator.generatePassword();
        String name = TestDataGenerator.generateName();

        // Регистрация без email
        Map<String, String> userWithoutEmail = Map.of(
                "password", password,
                "name", name
        );
        Response response = ApiSteps.registerUser(userWithoutEmail);

        // Проверка ошибки
        response.then()
                .statusCode(403)
                .body("success", equalTo(false))
                .body("message", equalTo("Email, password and name are required fields"));
    }

    @Test
    @Description("Попытка регистрации пользователя без пароля")
    public void createUserMissingPassword() {
        // Генерация тестовых данных
        String email = TestDataGenerator.generateEmail();
        String name = TestDataGenerator.generateName();

        // Регистрация без пароля
        Map<String, String> userWithoutPassword = Map.of(
                "email", email,
                "name", name
        );
        Response response = ApiSteps.registerUser(userWithoutPassword);

        // Проверка ошибки
        response.then()
                .statusCode(403)
                .body("success", equalTo(false))
                .body("message", equalTo("Email, password and name are required fields"));
    }
}
