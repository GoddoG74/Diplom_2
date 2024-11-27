import com.github.javafaker.Faker;

public class TestDataGenerator {

    private static final Faker faker = new Faker();

    // Генерация уникального email
    public static String generateEmail() {
        return faker.internet().emailAddress();
    }

    // Генерация уникального пароля
    public static String generatePassword() {
        return faker.internet().password();
    }

    // Генерация полного имени
    public static String generateName() {
        return faker.name().fullName();
    }

    // Генерация случайной строки
    public static String generateRandomString() {
        return faker.lorem().characters(10);
    }
}
