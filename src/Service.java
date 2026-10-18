

import java.util.Scanner;
import java.util.Locale;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.net.URI;


public class Service {
    private final String accessKey;
    private double lat;
    private double lon;
    private int lim;

    public Service(final String accessKey) {
        this.accessKey = accessKey;
    }

    public Service(final String accessKey, double lat, double lon, int lim) {
        this.accessKey = accessKey;
        this.lat = lat;
        this.lon = lon;
        this.lim = lim;
    }


    public void start() {
        menu();
    }

    private HttpResponse<String> request(String url) {

        System.out.println("Выполнение запроса...");
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder().
                uri(URI.create(url)).
                header("X-Yandex-Weather-Key", "%s".formatted(accessKey)).build();
        CompletableFuture<HttpResponse<String>> future = client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).orTimeout(10, TimeUnit.SECONDS);

        future.exceptionally(e -> {
            System.out.println("Ошибка: " + e.getMessage());
            return null;
        });

        HttpResponse<String> response = future.join();

        System.out.println("Запрос выполнен...");
        System.out.println("Статус запроса: " + response.statusCode());
        System.out.println("URL: " + response.uri() + "\n");
        return response;
    }

    private void response(HttpResponse<String> response) {
        if (response != null && response.statusCode() >= 200 && response.statusCode() < 300) {
            System.out.println("\nВывод данных: \n" + response.body().replace(",", ",\n") + "\n");
        } else
            System.out.println("\n\nДанные отсутсвуют... Выполните запрос...\n\n");
    }

    private int parseTemp(String response) {
        int factIndex = response.indexOf("fact");
        int tempIndex = response.indexOf("temp", factIndex);
        return catTemp(response, tempIndex);
    }

    private int catTemp(String response, int tempIndex) {
        int startIndex = response.indexOf(":", tempIndex) + 1;
        int endIndex = response.indexOf(",", startIndex);
        return Integer.parseInt(response.substring(startIndex, endIndex));
    }


    private int parseAverageTemperatureDay(String response) {

        int forecastsIndex = response.indexOf("forecasts");
        int partsIndex = response.indexOf("parts", forecastsIndex);
        int[] daysTemp = new int[lim];
        int count = 0;
        while (partsIndex != -1) {
            int dayIndex = response.indexOf("day", partsIndex);
            int tempIndex = response.indexOf("temp_avg", dayIndex);
            daysTemp[count++] = catTemp(response, tempIndex);
            partsIndex = response.indexOf("parts", tempIndex);
        }
        int sumDaysTemp = 0;
        for (int item : daysTemp)
            sumDaysTemp += item;

        return sumDaysTemp / lim;
    }


    private void menu() {


        String strMenu = """
                1.Ввести координаты;
                2.Выполнить запрос;
                3.Закрыть программу;
                """;
        System.out.println(strMenu);
        System.out.println("Введите число: ");
        Scanner scanner = new Scanner(System.in).useLocale(Locale.ROOT);

        while (!scanner.hasNextInt()) {
            scanner.nextInt();
        }

        int value = scanner.nextInt();


        switch (value) {
            case 1:
                int valueLim;
                coordinateInput();

                do {
                    while (!scanner.hasNextInt()) {
                        scanner.nextLine();
                        System.out.println("Не коректные данные! Введите целое число...");
                    }
                    valueLim = scanner.nextInt();
                    if (valueLim <= 0)
                        System.out.println("Количество дней должно быть больше нуля!");

                } while (valueLim <= 0);

                lim = valueLim;

                break;

            case 2:

                    HttpResponse<String> response = request(String.format(Locale.US, "https://api.weather.yandex.ru/v2/forecast?lat=%f&lon=%f", lat, lon));

                    response(response);
                    System.out.println("Температура: " + parseTemp(response.body()) + "\n");

                    if (lim <= 0)
                        break;

                    response = request(String.format(Locale.US, "https://api.weather.yandex.ru/v2/forecast?lat=%f&lon=%f&limit=%d", lat, lon, lim));
                    System.out.println("Температура: " + parseTemp(response.body()) + "\n");
                    int limit = lim % 10;
                    System.out.println(("Средняя температура днем за %d %s: ".formatted(lim, limit > 1 && limit < 5 ? "дня" : limit == 1 ? "день" : "дней")) +
                            parseAverageTemperatureDay(response.body()) + "\n");

                break;
            case 3:
                scanner.close();
                return;
            default:
                System.out.println("Нет такого пункта! Повторите попытку..");
        }

        menu();

    }


    private void coordinateInput() {
        Scanner scanner = new Scanner(System.in).useLocale(Locale.ROOT);

        System.out.println("Введите координаты: ");

        System.out.println("Широта: ");
        checkingInput(scanner);
        lat = scanner.nextDouble();

        System.out.println("Долгота: ");
        checkingInput(scanner);
        lon = scanner.nextDouble();

        System.out.println("Укажите количество дней для рассчета средней температуры: ");

    }

    private void checkingInput(Scanner scanner) {
        while (!scanner.hasNextDouble()) {
            scanner.nextLine();
            System.out.println("Не коректные данные! Введите вещественное число...");
        }
    }

}
