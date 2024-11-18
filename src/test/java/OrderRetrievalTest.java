import com.github.javafaker.Faker;
import io.qameta.allure.Step;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class OrderRetrievalTest {

    private static final Logger LOGGER = Logger.getLogger(OrderRetrievalTest.class.getName());

    private final String BASE_URI = "https://stellarburgers.nomoreparties.site";
    private final String REGISTER_ENDPOINT = "/api/auth/register";
    private final String LOGIN_ENDPOINT = "/api/auth/login";
    private final String USER_ENDPOINT = "/api/auth/user";
    private final String ORDER_ENDPOINT = "/api/orders";
    private final String INGREDIENTS_ENDPOINT = "/api/ingredients";

    private Faker faker;
    private String createdUserToken;
    private List<String> ingredientHashes;

    @Before
    public void setup() {
        RestAssured.baseURI = BASE_URI;
        faker = new Faker();
        ingredientHashes = fetchIngredientHashes();
        logInfo("Тестовая среда настроена, ингредиенты загружены.");
    }

    @After
    public void cleanup() {
        if (createdUserToken != null) {
            deleteUser();
            logInfo("Тестовая среда очищена: пользователь удалён.");
        } else {
            logInfo("Очистка не требуется: пользователь не был создан.");
        }
    }

    @Step("Получаем хэши ингредиентов")
    private List<String> fetchIngredientHashes() {
        Response response = sendGetRequest(INGREDIENTS_ENDPOINT);
        logResponse(response);
        response.then().statusCode(200);
        return response.jsonPath().getList("data._id", String.class);
    }

    @Step("Регистрируем нового пользователя")
    private void registerUser(String email, String password, String name) {
        Map<String, String> user = Map.of(
                "email", email,
                "password", password,
                "name", name
        );
        Response response = sendPostRequest(REGISTER_ENDPOINT, user);
        logResponse(response);
        response.then().statusCode(200);
        logInfo("Пользователь зарегистрирован: " + email);
    }

    @Step("Авторизуем пользователя")
    private void authorizeUser(String email, String password) {
        Map<String, String> credentials = Map.of(
                "email", email,
                "password", password
        );
        Response response = sendPostRequest(LOGIN_ENDPOINT, credentials);
        logResponse(response);
        createdUserToken = response.jsonPath().getString("accessToken");
        response.then().statusCode(200);
        logInfo("Пользователь авторизован: " + email);
    }

    @Step("Удаляем пользователя")
    private void deleteUser() {
        Response response = sendAuthorizedDeleteRequest(USER_ENDPOINT);
        logResponse(response);
        response.then().statusCode(202);
        logInfo("Пользователь удалён.");
    }

    @Step("Создаём заказ")
    private Response createOrder(String[] ingredients, boolean withAuthorization) {
        Map<String, Object> order = Map.of("ingredients", ingredients);
        logInfo("Создаём заказ с ингредиентами: " + String.join(", ", ingredients));
        Response response = withAuthorization
                ? sendAuthorizedPostRequest(ORDER_ENDPOINT, order)
                : sendPostRequest(ORDER_ENDPOINT, order);
        logResponse(response);
        return response;
    }

    @Test
    public void getOrdersForAuthorizedUser() {
        logInfo("Тест: получение заказов для авторизованного пользователя...");

        // Шаг 1. Генерация нового пользователя и авторизация
        String email = faker.internet().emailAddress();
        String password = faker.internet().password();
        String name = faker.name().fullName();
        registerUser(email, password, name);
        authorizeUser(email, password);

        // Шаг 2. Получение списка ингредиентов
        String[] ingredients = ingredientHashes.subList(0, 2).toArray(new String[0]);

        // Шаг 3. Создание заказа
        Response createOrderResponse = createOrder(ingredients, true);
        int createdOrderNumber = createOrderResponse.jsonPath().getInt("order.number");
        logInfo("Создан заказ с номером: " + createdOrderNumber);

        // Шаг 4. Запрос на получение заказов
        Response response = sendAuthorizedGetRequest(ORDER_ENDPOINT);

        // Логируем ответ
        logResponse(response);

        // Шаг 5. Проверка ответа
        response.then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("orders.size()", equalTo(1));

        // Детальная проверка полей
        response.then()
                .body("orders[0].number", equalTo(createdOrderNumber))
                .body("orders[0].status", notNullValue()) // Проверка, что статус существует
                .body("orders[0].status", not(emptyOrNullString())) // Проверка, что статус не пустой
                .body("orders[0].ingredients", equalTo(List.of(ingredients)));

        logInfo("Успешно получен список заказов для авторизованного пользователя.");
    }

    @Test
    public void getOrdersForUnauthorizedUser() {
        logInfo("Тест: получение заказов для неавторизованного пользователя...");

        // Шаг 1. Генерация нового пользователя и авторизация
        String email = faker.internet().emailAddress();
        String password = faker.internet().password();
        String name = faker.name().fullName();
        registerUser(email, password, name);
        authorizeUser(email, password);

        // Шаг 2. Получение списка ингредиентов
        String[] ingredients = ingredientHashes.subList(0, 2).toArray(new String[0]);

        // Шаг 3. Создание заказа
        createOrder(ingredients, true);

        // Шаг 4. Запрос на получение заказов без авторизации
        Response response = sendGetRequest(ORDER_ENDPOINT);

        // Логируем ответ
        logResponse(response);

        // Шаг 5. Проверка ответа
        response.then()
                .statusCode(401)
                .body("success", equalTo(false))
                .body("message", equalTo("You should be authorised"));

        logInfo("Запрос без авторизации завершился с ошибкой, как ожидалось.");
    }

    // Вспомогательные методы

    private Response sendGetRequest(String endpoint) {
        return given()
                .header("Content-Type", "application/json")
                .get(endpoint);
    }

    private Response sendPostRequest(String endpoint, Map<String, ?> body) {
        return given()
                .header("Content-Type", "application/json")
                .body(body)
                .post(endpoint);
    }

    private Response sendAuthorizedPostRequest(String endpoint, Map<String, ?> body) {
        return given()
                .header("Authorization", createdUserToken)
                .header("Content-Type", "application/json")
                .body(body)
                .post(endpoint);
    }

    private Response sendAuthorizedGetRequest(String endpoint) {
        return given()
                .header("Authorization", createdUserToken)
                .header("Content-Type", "application/json")
                .get(endpoint);
    }

    private Response sendAuthorizedDeleteRequest(String endpoint) {
        return given()
                .header("Authorization", createdUserToken)
                .delete(endpoint);
    }

    private void logInfo(String message) {
        LOGGER.info(message);
    }

    private void logResponse(Response response) {
        LOGGER.info("Код ответа: " + response.statusCode());
        LOGGER.info("Тело ответа: " + response.asString());
    }
}
