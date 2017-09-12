package model;

import java.math.BigDecimal;

public class Order {
    private BigDecimal units;
    private BigDecimal value;
    private String identifier;

    public Order(BigDecimal units, BigDecimal value, String identifier) {

        this.units = units;
        this.value = value;
        this.identifier = identifier;
    }

    public Order(Order order) {
        this(order.getUnits(), order.getValue(), order.getIdentifier());
    }

    public BigDecimal getUnits() {
        return units;
    }

    public void setUnits(BigDecimal units) {
        this.units = units;
    }

    public BigDecimal getValue() {
        return value;
    }

    public void setValue(BigDecimal value) {
        this.value = value;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String toString (){
        return identifier + "|" + units.toString() + "|" + value.toString();
    }
}
