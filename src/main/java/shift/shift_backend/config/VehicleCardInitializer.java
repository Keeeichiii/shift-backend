package shift.shift_backend.config;

import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import shift.shift_backend.domain.entity.VehicleCard;
import shift.shift_backend.repository.VehicleCardRepository;

@Component
@RequiredArgsConstructor
public class VehicleCardInitializer implements CommandLineRunner {

    private final VehicleCardRepository vehicleCardRepository;

    @Override
    @Transactional
    public void run(String... args) {
        ensureCard(
                "volkswagen-polo-sedan-wrapped",
                "Volkswagen Polo Sedan",
                "standard",
                "/images/cars/стандарт/volkswagen-polo-sedan.png",
                new BigDecimal("0.39"),
                true,
                "Стандарт, с оклейкой",
                "Практичный городской седан в фирменной оклейке FredAvto для ежедневных поездок по городу.",
                "%",
                "Автомат",
                "Бензин",
                "1.6L",
                "Бензин включён\nЗавершение аренды в разрешённых зонах\nДоступно подтверждённым пользователям",
                "Камера\nКондиционер\nПодогрев сидений",
                "1 мин. - 0.39 BYN\nОжидание - 0.15 BYN / мин.",
                "3 ч. - 19.00 BYN\n6 ч. - 29.00 BYN",
                "1 сутки - 49.00 BYN\n3 суток - 139.00 BYN"
        );
        ensureCard(
                "ford-mustang-wrapped",
                "Ford Mustang",
                "standard",
                "/images/cars/премиум/ford-mustang.png",
                new BigDecimal("1.29"),
                true,
                "Стандарт, с оклейкой",
                "Яркая модель в фирменной оклейке FredAvto для заметных городских поездок и особых случаев.",
                "%",
                "Автомат",
                "Бензин",
                "2.3L",
                "Топливо включено\nЗавершение аренды в разрешённых зонах\nДоступно подтверждённым пользователям",
                "Камера\nКлимат-контроль\nМультимедиа",
                "1 мин. - 1.29 BYN\nОжидание - 0.25 BYN / мин.",
                "3 ч. - 39.00 BYN\n6 ч. - 69.00 BYN",
                "1 сутки - 119.00 BYN\n3 суток - 339.00 BYN"
        );
        ensureCard(
                "toyota-rav4-city",
                "Toyota RAV4",
                "crossover",
                "/images/cars/кроссовер/Toyota RAV4.png",
                new BigDecimal("0.89"),
                false,
                "Кроссовер, без оклейки",
                "Универсальный кроссовер для города и трассы, комфортная посадка и просторный багажник.",
                "NEW",
                "Автомат",
                "Бензин",
                "2.0L",
                "Топливо включено\nЗавершение аренды в разрешённых зонах\nДоступно подтверждённым пользователям",
                "Камера 360\nКлимат-контроль\nПодогрев руля",
                "1 мин. - 0.89 BYN\nОжидание - 0.22 BYN / мин.",
                "3 ч. - 29.00 BYN\n6 ч. - 49.00 BYN",
                "1 сутки - 89.00 BYN\n3 суток - 249.00 BYN"
        );
        ensureCard(
                "bmw-530i-business",
                "BMW 530i",
                "premium",
                "/images/cars/премиум/BMW 530i.png",
                new BigDecimal("1.39"),
                false,
                "Премиум, без оклейки",
                "Бизнес-седан с расширенным пакетом комфорта для деловых поездок и дальних маршрутов.",
                "VIP",
                "Автомат",
                "Бензин",
                "2.0 Turbo",
                "Топливо включено\nЗавершение аренды в разрешённых зонах\nДоступно подтверждённым пользователям",
                "Адаптивный круиз\nПамять сидений\nПремиум-аудио",
                "1 мин. - 1.39 BYN\nОжидание - 0.30 BYN / мин.",
                "3 ч. - 49.00 BYN\n6 ч. - 89.00 BYN",
                "1 сутки - 159.00 BYN\n3 суток - 449.00 BYN"
        );
        ensureCard(
                "kia-carnival-family",
                "Kia Carnival",
                "minivan",
                "/images/cars/минивэн 7 мест/Kia Carnival.png",
                new BigDecimal("0.99"),
                false,
                "Минивэн 7 мест, без оклейки",
                "Семиместный минивэн для семейных и групповых поездок с увеличенным багажным отсеком.",
                "7 мест",
                "Автомат",
                "Бензин",
                "2.2D",
                "Топливо включено\nЗавершение аренды в разрешённых зонах\nДоступно подтверждённым пользователям",
                "7 сидений\nТри ряда климат-контроля\nЭлектродвери",
                "1 мин. - 0.99 BYN\nОжидание - 0.24 BYN / мин.",
                "3 ч. - 35.00 BYN\n6 ч. - 59.00 BYN",
                "1 сутки - 109.00 BYN\n3 суток - 309.00 BYN"
        );
        ensureCard(
                "porsche-911-icon",
                "Porsche 911",
                "exclusive",
                "/images/cars/эксклюзив/Porsche 911.png",
                new BigDecimal("2.49"),
                false,
                "Эксклюзив, без оклейки",
                "Спортивная модель для особых случаев с высокой динамикой и узнаваемым дизайном.",
                "TOP",
                "Автомат",
                "Бензин",
                "3.0 Turbo",
                "Топливо включено\nПовышенный депозит\nДоступно подтверждённым пользователям",
                "Launch Control\nСпорт-выхлоп\nАдаптивная подвеска",
                "1 мин. - 2.49 BYN\nОжидание - 0.50 BYN / мин.",
                "3 ч. - 89.00 BYN\n6 ч. - 159.00 BYN",
                "1 сутки - 299.00 BYN\n3 суток - 849.00 BYN"
        );
        ensureCard(
                "tesla-model-3-electric",
                "Tesla Model 3",
                "electric",
                "/images/cars/электро/Tesla Model 3.png",
                new BigDecimal("1.09"),
                false,
                "Электро, без оклейки",
                "Электромобиль с тихим ходом и моментальным ускорением для городских и пригородных маршрутов.",
                "EV",
                "Автомат",
                "Электро",
                "Dual Motor",
                "Заряд включён\nЗавершение аренды в разрешённых зонах\nДоступно подтверждённым пользователям",
                "Автопилот\nБольшой дисплей\nБыстрая зарядка",
                "1 мин. - 1.09 BYN\nОжидание - 0.20 BYN / мин.",
                "3 ч. - 39.00 BYN\n6 ч. - 69.00 BYN",
                "1 сутки - 129.00 BYN\n3 суток - 369.00 BYN"
        );
        ensureCard(
                "mini-cooper-cabrio-sun",
                "Mini Cooper Cabrio",
                "cabriolet",
                "/images/cars/кабриолет/Mini Cooper Cabrio.png",
                new BigDecimal("1.19"),
                false,
                "Кабриолет, без оклейки",
                "Компактный кабриолет для тёплой погоды, городских прогулок и поездок выходного дня.",
                "CABRIO",
                "Автомат",
                "Бензин",
                "1.5L",
                "Топливо включено\nЗавершение аренды в разрешённых зонах\nДоступно подтверждённым пользователям",
                "Складной верх\nКамера заднего вида\nСпорт-режим",
                "1 мин. - 1.19 BYN\nОжидание - 0.24 BYN / мин.",
                "3 ч. - 42.00 BYN\n6 ч. - 74.00 BYN",
                "1 сутки - 139.00 BYN\n3 суток - 399.00 BYN"
        );
        ensureCard(
                "toyota-land-cruiser-trail",
                "Toyota Land Cruiser",
                "offroad",
                "/images/cars/внедорожник/Toyota Land Cruiser.png",
                new BigDecimal("1.59"),
                false,
                "Внедорожник, без оклейки",
                "Полноразмерный внедорожник с высоким клиренсом и полным приводом для сложных дорожных условий.",
                "4x4",
                "Автомат",
                "Дизель",
                "3.0D",
                "Топливо включено\nЗавершение аренды в разрешённых зонах\nДоступно подтверждённым пользователям",
                "Полный привод\nКамеры кругового обзора\nБлокировки",
                "1 мин. - 1.59 BYN\nОжидание - 0.32 BYN / мин.",
                "3 ч. - 59.00 BYN\n6 ч. - 99.00 BYN",
                "1 сутки - 179.00 BYN\n3 суток - 519.00 BYN"
        );
        ensureCard(
                "ford-transit-cargo",
                "Ford Transit",
                "cargo",
                "/images/cars/грузовой/Ford Transit.png",
                new BigDecimal("1.29"),
                false,
                "Грузовой, без оклейки",
                "Фургон для перевозки грузов и коммерческих задач с увеличенным объёмом кузова.",
                "L2H2",
                "Механика",
                "Дизель",
                "2.0D",
                "Топливо включено\nЗавершение аренды в разрешённых зонах\nДоступно подтверждённым пользователям",
                "Грузовой отсек\nКрепления груза\nПарктроники",
                "1 мин. - 1.29 BYN\nОжидание - 0.28 BYN / мин.",
                "3 ч. - 45.00 BYN\n6 ч. - 79.00 BYN",
                "1 сутки - 149.00 BYN\n3 суток - 429.00 BYN"
        );
    }

    private void ensureCard(
            String slug,
            String title,
            String category,
            String imagePath,
            BigDecimal pricePerMinute,
            boolean wrapped,
            String shortDescription,
            String detailDescription,
            String badge,
            String transmission,
            String fuelType,
            String engine,
            String conditions,
            String features,
            String minutePackages,
            String hourPackages,
            String dayPackages
    ) {
        if (vehicleCardRepository.findBySlug(slug).isPresent()) {
            return;
        }

        VehicleCard card = new VehicleCard();
        card.setSlug(slug);
        card.setTitle(title);
        card.setCategory(category);
        card.setWrapped(wrapped);
        card.setImagePath(imagePath);
        card.setPricePerMinute(pricePerMinute);
        card.setShortDescription(shortDescription);
        card.setDetailDescription(detailDescription);
        card.setBadge(badge);
        card.setTransmission(transmission);
        card.setFuelType(fuelType);
        card.setEngine(engine);
        card.setConditionsText(conditions);
        card.setFeaturesText(features);
        card.setMinutePackagesText(minutePackages);
        card.setHourPackagesText(hourPackages);
        card.setDayPackagesText(dayPackages);
        card.setPublished(true);
        vehicleCardRepository.save(card);
    }
}
