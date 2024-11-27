import io.qameta.allure.Description;
import io.restassured.response.Response;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.equalTo;

public class OrderCreationTest extends BaseTest {

    private List<String> ingredientHashes;

    @Before
    public void setUp() {
        super.setUpBaseUri(); // Устанавливаем базовый URL
        ingredientHashes = ApiSteps.fetchIngredientHashes().jsonPath().getList("data._id", String.class); // Получаем список ингредиентов
    }

    @Test
    @Description("Создание заказа с авторизацией")
    public void createOrderWithAuthorization() {
        // Генерация данных пользователя
        String email = TestDataGenerator.generateEmail();
        String password = TestDataGenerator.generatePassword();
        String name = TestDataGenerator.generateName();

        // Регистрация и авторизация
        ApiSteps.registerUser(email, password, name);
        token = ApiSteps.loginUser(email, password).jsonPath().getString("accessToken");

        // Создание заказа
        Map<String, Object> order = Map.of("ingredients", ingredientHashes.subList(0, 2));
        Response response = ApiSteps.createOrder(order, token);

        // Проверка ответа
        response.then()
                .statusCode(200)
                .body("success", equalTo(true));
    }

    @Test
    @Description("Попытка создания заказа без авторизации")
    public void createOrderWithoutAuthorization() {
        // Создание заказа без авторизации
        Map<String, Object> order = Map.of("ingredients", ingredientHashes.subList(0, 2));
        Response response = ApiSteps.createOrder(order, null);

        // Проверка ответа
        response.then()
                .statusCode(401)
                .body("message", equalTo("You should be authorised"));
    }

    @Test
    @Description("Создание заказа без ингредиентов")
    public void createOrderWithoutIngredients() {
        // Генерация данных пользователя
        String email = TestDataGenerator.generateEmail();
        String password = TestDataGenerator.generatePassword();
        String name = TestDataGenerator.generateName();

        // Регистрация и авторизация
        ApiSteps.registerUser(email, password, name);
        token = ApiSteps.loginUser(email, password).jsonPath().getString("accessToken");

        // Создание заказа без ингредиентов
        Map<String, Object> order = Map.of("ingredients", List.of());
        Response response = ApiSteps.createOrder(order, token);

        // Проверка ответа
        response.then()
                .statusCode(400)
                .body("success", equalTo(false))
                .body("message", equalTo("Ingredient ids must be provided"));
    }

    @Test
    @Description("Создание заказа с неверным хешем ингредиента")
    public void createOrderWithInvalidIngredientHash() {
        // Генерация данных пользователя
        String email = TestDataGenerator.generateEmail();
        String password = TestDataGenerator.generatePassword();
        String name = TestDataGenerator.generateName();

        // Регистрация и авторизация
        ApiSteps.registerUser(email, password, name);
        token = ApiSteps.loginUser(email, password).jsonPath().getString("accessToken");

        // Создание заказа с неверным хешем
        Map<String, Object> order = Map.of("ingredients", List.of("invalidIngredientHash"));
        Response response = ApiSteps.createOrder(order, token);

        // Проверка ответа
        response.then()
                .statusCode(500); // Предполагается, что сервер вернёт 500
    }
}
