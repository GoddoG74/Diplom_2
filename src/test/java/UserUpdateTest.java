import io.qameta.allure.Description;
import io.restassured.response.Response;
import org.junit.Test;

import java.util.Map;

import static org.hamcrest.Matchers.equalTo;


public class UserUpdateTest extends BaseTest {

    @Test
    @Description("Обновление email пользователя с авторизацией")
    public void updateUserEmailWithAuthorization() {
        // Генерация данных пользователя
        String email = TestDataGenerator.generateEmail();
        String password = TestDataGenerator.generatePassword();
        String name = TestDataGenerator.generateName();

        // Регистрация пользователя
        Response registrationResponse = ApiSteps.registerUser(email, password, name);
        registrationResponse.then().statusCode(200);

        // Сохранение токена для удаления
        token = registrationResponse.jsonPath().getString("accessToken");

        // Обновление email
        String newEmail = TestDataGenerator.generateEmail();
        Map<String, String> updatedData = Map.of("email", newEmail);
        Response updateResponse = ApiSteps.updateUser(token, updatedData);

        // Проверка успешного обновления
        updateResponse.then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("user.email", equalTo(newEmail))
                .body("user.name", equalTo(name));
    }

    @Test
    @Description("Обновление имени пользователя с авторизацией")
    public void updateUserNameWithAuthorization() {
        // Генерация данных пользователя
        String email = TestDataGenerator.generateEmail();
        String password = TestDataGenerator.generatePassword();
        String name = TestDataGenerator.generateName();

        // Регистрация пользователя
        Response registrationResponse = ApiSteps.registerUser(email, password, name);
        registrationResponse.then().statusCode(200);

        // Сохранение токена для удаления
        token = registrationResponse.jsonPath().getString("accessToken");

        // Обновление имени
        String newName = TestDataGenerator.generateName();
        Map<String, String> updatedData = Map.of("name", newName);
        Response updateResponse = ApiSteps.updateUser(token, updatedData);

        // Проверка успешного обновления
        updateResponse.then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("user.email", equalTo(email))
                .body("user.name", equalTo(newName));
    }

    @Test
    @Description("Обновление email без авторизации")
    public void updateUserEmailWithoutAuthorization() {
        // Обновление email без токена
        String newEmail = TestDataGenerator.generateEmail();
        Map<String, String> updatedData = Map.of("email", newEmail);
        Response updateResponse = ApiSteps.updateUser(null, updatedData);

        // Проверка ошибки
        updateResponse.then()
                .statusCode(401)
                .body("success", equalTo(false))
                .body("message", equalTo("You should be authorised"));
    }

    @Test
    @Description("Обновление имени без авторизации")
    public void updateUserNameWithoutAuthorization() {
        // Обновление имени без токена
        String newName = TestDataGenerator.generateName();
        Map<String, String> updatedData = Map.of("name", newName);
        Response updateResponse = ApiSteps.updateUser(null, updatedData);

        // Проверка ошибки
        updateResponse.then()
                .statusCode(401)
                .body("success", equalTo(false))
                .body("message", equalTo("You should be authorised"));
    }

    @Test
    @Description("Попытка обновления email на существующий")
    public void updateUserToExistingEmail() {
        // Регистрация первого пользователя
        String email1 = TestDataGenerator.generateEmail();
        String password1 = TestDataGenerator.generatePassword();
        String name1 = TestDataGenerator.generateName();
        Response firstRegistrationResponse = ApiSteps.registerUser(email1, password1, name1);
        firstRegistrationResponse.then().statusCode(200);

        String token1 = firstRegistrationResponse.jsonPath().getString("accessToken");

        // Регистрация второго пользователя
        String email2 = TestDataGenerator.generateEmail();
        String password2 = TestDataGenerator.generatePassword();
        String name2 = TestDataGenerator.generateName();
        Response secondRegistrationResponse = ApiSteps.registerUser(email2, password2, name2);
        secondRegistrationResponse.then().statusCode(200);

        token = secondRegistrationResponse.jsonPath().getString("accessToken");

        // Попытка обновления email второго пользователя на email первого
        Map<String, String> updatedData = Map.of("email", email1);
        Response updateResponse = ApiSteps.updateUser(token, updatedData);

        // Проверка ошибки
        updateResponse.then()
                .statusCode(403)
                .body("success", equalTo(false))
                .body("message", equalTo("User with such email already exists"));

        // Удаление первого пользователя
        ApiSteps.deleteUser(token1);
    }
}
