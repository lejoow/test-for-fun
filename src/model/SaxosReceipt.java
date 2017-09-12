package model;

import java.math.BigDecimal;

public class SaxosReceipt {
    private String identifier;
    private BigDecimal totalValue;
    private BigDecimal totalUnits;

    public SaxosReceipt(String identifier, BigDecimal totalValue, BigDecimal totalUnits) {
        this.identifier = identifier;
        this.totalValue = totalValue;
        this.totalUnits = totalUnits;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public BigDecimal getTotalValue() {
        return totalValue;
    }

    public void setTotalValue(BigDecimal totalValue) {
        this.totalValue = totalValue;
    }

    public BigDecimal getTotalUnits() {
        return totalUnits;
    }

    public void setTotalUnits(BigDecimal totalUnits) {
        this.totalUnits = totalUnits;
    }

    public String toString (){
        return identifier + "|" + totalUnits.toString() + "|" + totalValue.toString();
    }
}
