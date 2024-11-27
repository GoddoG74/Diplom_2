import io.qameta.allure.Description;
import io.restassured.response.Response;
import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;

public class UserLoginTest extends BaseTest {

    @Test
    @Description("Логин с корректными данными зарегистрированного пользователя")
    public void loginWithExistingUser() {
        // Генерация данных пользователя
        String email = TestDataGenerator.generateEmail();
        String password = TestDataGenerator.generatePassword();
        String name = TestDataGenerator.generateName();

        // Регистрация пользователя
        Response registrationResponse = ApiSteps.registerUser(email, password, name);
        registrationResponse.then().statusCode(200);

        // Сохранение токена для удаления
        token = registrationResponse.jsonPath().getString("accessToken");

        // Логин с корректными данными
        Response loginResponse = ApiSteps.loginUser(email, password);

        // Проверка успешного логина
        loginResponse.then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("user.email", equalTo(email))
                .body("user.name", equalTo(name))
                .body("accessToken", startsWith("Bearer "))
                .body("refreshToken", notNullValue());
    }

    @Test
    @Description("Логин с правильным email и неправильным паролем")
    public void loginWithCorrectEmailAndIncorrectPassword() {
        // Генерация данных пользователя
        String email = TestDataGenerator.generateEmail();
        String password = TestDataGenerator.generatePassword();
        String name = TestDataGenerator.generateName();

        // Регистрация пользователя
        Response registrationResponse = ApiSteps.registerUser(email, password, name);
        registrationResponse.then().statusCode(200);

        // Сохранение токена для удаления
        token = registrationResponse.jsonPath().getString("accessToken");

        // Логин с правильным email и неправильным паролем
        String invalidPassword = TestDataGenerator.generatePassword();
        Response loginResponse = ApiSteps.loginUser(email, invalidPassword);

        // Проверка ошибки
        loginResponse.then()
                .statusCode(401)
                .body("success", equalTo(false))
                .body("message", equalTo("email or password are incorrect"));
    }

    @Test
    @Description("Логин с неправильным email и правильным паролем")
    public void loginWithIncorrectEmailAndCorrectPassword() {
        // Генерация данных пользователя
        String email = TestDataGenerator.generateEmail();
        String password = TestDataGenerator.generatePassword();
        String name = TestDataGenerator.generateName();

        // Регистрация пользователя
        Response registrationResponse = ApiSteps.registerUser(email, password, name);
        registrationResponse.then().statusCode(200);

        // Сохранение токена для удаления
        token = registrationResponse.jsonPath().getString("accessToken");

        // Логин с неправильным email и правильным паролем
        String invalidEmail = TestDataGenerator.generateEmail();
        Response loginResponse = ApiSteps.loginUser(invalidEmail, password);

        // Проверка ошибки
        loginResponse.then()
                .statusCode(401)
                .body("success", equalTo(false))
                .body("message", equalTo("email or password are incorrect"));
    }
}
