import com.github.javafaker.Faker;
import io.qameta.allure.Allure;
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
import static org.hamcrest.Matchers.equalTo;

public class OrderCreationTest {

    private static final Logger LOGGER = Logger.getLogger(OrderCreationTest.class.getName());

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
    public void createOrderWithAuthorization() {
        logInfo("Тест: создание заказа с авторизацией...");
        String email = faker.internet().emailAddress();
        String password = faker.internet().password();
        String name = faker.name().fullName();
        registerUser(email, password, name);
        authorizeUser(email, password);

        String[] ingredients = ingredientHashes.subList(0, 2).toArray(new String[0]);
        Response response = createOrder(ingredients, true);
        response.then()
                .statusCode(200)
                .body("success", equalTo(true));
    }

    @Test
    public void createOrderWithoutAuthorization() {
        logInfo("Тест: создание заказа без авторизации...");
        String[] ingredients = ingredientHashes.subList(0, 2).toArray(new String[0]);

        Response response = createOrder(ingredients, false);

        logBugIfNeeded(response, 401, "You should be authorised");
        response.then()
                .statusCode(401)
                .body("message", equalTo("You should be authorised"));
    }

    @Test
    public void createOrderWithoutIngredients() {
        logInfo("Тест: создание заказа без ингредиентов...");
        String email = faker.internet().emailAddress();
        String password = faker.internet().password();
        String name = faker.name().fullName();
        registerUser(email, password, name);
        authorizeUser(email, password);

        String[] emptyIngredients = {};
        Response response = createOrder(emptyIngredients, true);

        response.then()
                .statusCode(400)
                .body("success", equalTo(false))
                .body("message", equalTo("Ingredient ids must be provided"));
    }

    @Test
    public void createOrderWithInvalidIngredientHash() {
        logInfo("Тест: создание заказа с неверным хешем ингредиентов...");
        String email = faker.internet().emailAddress();
        String password = faker.internet().password();
        String name = faker.name().fullName();
        registerUser(email, password, name);
        authorizeUser(email, password);

        String[] ingredients = {"invalidIngredientHash"};
        Response response = createOrder(ingredients, true);
        response.then()
                .statusCode(500);
    }

    // Вспомогательные методы

    @Step("Логируем баг, если статус код ответа не соответствует ожиданию")
    private void logBugIfNeeded(Response response, int expectedStatusCode, String expectedMessage) {
        if (response.statusCode() != expectedStatusCode) {
            Allure.addAttachment("Баг-детали",
                    String.format("Ожидался код %d, но сервер вернул %d. Сообщение: %s",
                            expectedStatusCode, response.statusCode(), response.asString()));
            LOGGER.warning(String.format("Баг зафиксирован: Ожидался код %d, но сервер вернул %d",
                    expectedStatusCode, response.statusCode()));
        }
    }

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
