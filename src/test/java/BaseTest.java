import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.After;
import org.junit.Before;

import static io.restassured.RestAssured.given;

public class BaseTest {

    protected String token; // Токен авторизованного пользователя

    @Before
    public void setUpBaseUri() {
        RestAssured.baseURI = "https://stellarburgers.nomoreparties.site"; // Базовый URL для всех тестов
    }

    @After
    public void tearDown() {
        if (token != null && !token.isEmpty()) {
            Response deleteResponse = given()
                    .header("Authorization", token)
                    .delete(ApiEndpoints.USER);
            deleteResponse.then().statusCode(202); // Проверяем, что пользователь удалён
        }
    }
}
