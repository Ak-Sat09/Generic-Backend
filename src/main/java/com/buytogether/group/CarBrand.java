package com.buytogether.group;

/**
 * Brands sold in India. A fixed list keeps "Mahindra", "mahindra" and "M&M"
 * from becoming three different groups. Add new brands at the end.
 */
public enum CarBrand {

    MARUTI_SUZUKI("Maruti Suzuki"),
    HYUNDAI("Hyundai"),
    TATA("Tata Motors"),
    MAHINDRA("Mahindra"),
    KIA("Kia"),
    TOYOTA("Toyota"),
    HONDA("Honda"),
    MG("MG Motor"),
    SKODA("Skoda"),
    VOLKSWAGEN("Volkswagen"),
    RENAULT("Renault"),
    NISSAN("Nissan"),
    CITROEN("Citroen"),
    JEEP("Jeep"),
    BYD("BYD"),
    BMW("BMW"),
    MERCEDES_BENZ("Mercedes-Benz"),
    AUDI("Audi"),
    VOLVO("Volvo"),
    LAND_ROVER("Land Rover");

    private final String displayName;

    CarBrand(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
