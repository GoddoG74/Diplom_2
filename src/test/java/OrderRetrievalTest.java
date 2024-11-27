import io.qameta.allure.Description;
import io.restassured.response.Response;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;

public class OrderRetrievalTest extends BaseTest {

    private List<String> ingredientHashes;

    @Before
    public void setUp() {
        super.setUpBaseUri(); // Устанавливаем базовый URL
        ingredientHashes = ApiSteps.fetchIngredientHashes().jsonPath().getList("data._id", String.class); // Получаем список ингредиентов
    }

    @Test
    @Description("Получение заказов для авторизованного пользователя")
    public void getOrdersForAuthorizedUser() {
        // Генерация данных пользователя
        String email = TestDataGenerator.generateEmail();
        String password = TestDataGenerator.generatePassword();
        String name = TestDataGenerator.generateName();

        // Регистрация и авторизация
        ApiSteps.registerUser(email, password, name);
        token = ApiSteps.loginUser(email, password).jsonPath().getString("accessToken");

        // Создание заказа
        Map<String, Object> order = Map.of("ingredients", ingredientHashes.subList(0, 2));
        Response createOrderResponse = ApiSteps.createOrder(order, token);

        // Получение номера созданного заказа
        int createdOrderNumber = createOrderResponse.jsonPath().getInt("order.number");

        // Запрос списка заказов
        Response ordersResponse = ApiSteps.fetchOrders(token);

        // Проверка ответа
        ordersResponse.then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("orders.size()", greaterThan(0));

        // Проверка данных заказа
        ordersResponse.then()
                .body("orders[0].number", equalTo(createdOrderNumber))
                .body("orders[0].status", notNullValue())
                .body("orders[0].ingredients", equalTo(ingredientHashes.subList(0, 2)));
    }

    @Test
    @Description("Попытка получения заказов без авторизации")
    public void getOrdersForUnauthorizedUser() {
        // Запрос заказов без авторизации
        Response response = ApiSteps.fetchOrders(null);

        // Проверка ответа
        response.then()
                .statusCode(401)
                .body("success", equalTo(false))
                .body("message", equalTo("You should be authorised"));
    }

}
