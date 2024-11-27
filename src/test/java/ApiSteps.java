import io.qameta.allure.Step;
import io.restassured.response.Response;

import java.util.Map;

import static io.restassured.RestAssured.given;

public class ApiSteps {

    @Step("Регистрация пользователя с данными {user}")
    public static Response registerUser(Map<String, String> user) {
        return given()
                .header("Content-Type", "application/json")
                .body(user)
                .post(ApiEndpoints.REGISTER);
    }

    @Step("Регистрация пользователя с email {email}, паролем {password} и именем {name}")
    public static Response registerUser(String email, String password, String name) {
        Map<String, String> user = Map.of(
                "email", email,
                "password", password,
                "name", name
        );
        return registerUser(user); // Используем метод с Map
    }


    @Step("Авторизация пользователя")
    public static Response loginUser(String email, String password) {
        Map<String, String> credentials = Map.of(
                "email", email,
                "password", password
        );
        return given()
                .header("Content-Type", "application/json")
                .body(credentials)
                .post(ApiEndpoints.LOGIN);
    }

    @Step("Удаление пользователя")
    public static Response deleteUser(String token) {
        return given()
                .header("Authorization", token)
                .header("Content-Type", "application/json")
                .delete(ApiEndpoints.USER);
    }

    @Step("Получение списка ингредиентов")
    public static Response fetchIngredientHashes() {
        return given()
                .header("Content-Type", "application/json")
                .get(ApiEndpoints.INGREDIENTS);
    }

    @Step("Создание заказа")
    public static Response createOrder(Map<String, Object> order, String token) {
        if (token != null && !token.isEmpty()) {
            return given()
                    .header("Authorization", token)
                    .header("Content-Type", "application/json")
                    .body(order)
                    .post(ApiEndpoints.ORDERS);
        } else {
            return given()
                    .header("Content-Type", "application/json")
                    .body(order)
                    .post(ApiEndpoints.ORDERS);
        }
    }

    @Step("Получение списка заказов пользователя")
    public static Response fetchOrders(String token) {
        if (token != null) {
            return given()
                    .header("Authorization", token)
                    .header("Content-Type", "application/json")
                    .get(ApiEndpoints.ORDERS);
        } else {
            return given()
                    .header("Content-Type", "application/json")
                    .get(ApiEndpoints.ORDERS);
        }
    }


    @Step("Обновление данных пользователя")
    public static Response updateUser(String token, Map<String, String> updatedData) {
        var requestSpec = given()
                .header("Content-Type", "application/json")
                .body(updatedData);

        if (token != null && !token.isEmpty()) {
            requestSpec.header("Authorization", token);
        }

        return requestSpec.when().patch(ApiEndpoints.USER);
    }

}
