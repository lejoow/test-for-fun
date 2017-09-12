package model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Customer {
    private String name;
    //pending transactions
    private List<Order> transactions;
    //completed transactions
    private List<Order> completedTransactions;
    //current holding
    private List<Order> derivatives;
    //cash after selling holding
    private BigDecimal cash;

    public Customer(String name) {
        this.name = name;
        this.transactions = new ArrayList<>();
        this.completedTransactions = new ArrayList<>();
        this.derivatives = new ArrayList<>();
        this.cash = BigDecimal.ZERO;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getCash() {
        return cash;
    }

    public void addCash(BigDecimal newCash){
        this.cash = this.cash.add(newCash);
    }

    public void setCash(BigDecimal cash) {
        this.cash = cash;
    }

    public List<Order> getTransactions() {
        return new ArrayList(transactions);
    }

    public void setTransactions(List<Order> transactions) {
        this.transactions = transactions;
    }

    public List<Order> getDerivatives() {
        return new ArrayList(derivatives);
    }

    public void setDerivatives(List<Order> assets) {
        this.derivatives = assets;
    }

    public List<Order> getCompletedTransactions() {
        return new ArrayList(completedTransactions);
    }

    public void setCompletedTransactions(List<Order> completedTransactions) {
        this.completedTransactions = completedTransactions;
    }

    public void addDerivatives(Order x){
        this.derivatives.add(x);
    }

    public void addTransactions(Order x){
        this.transactions.add(x);
    }

    public Optional<Order> findPendingTransaction(String id) {
        return this.transactions
            .stream()
            .filter(order -> order.getIdentifier().equals(id))
            .findFirst();
    }

    public void updateOrderValue(String name, BigDecimal value){
        for (Order x: transactions){
            if (x.getIdentifier().equals(name)){
                x.setValue(value);
            }
        }

        for (Order x: derivatives){
            if (x.getIdentifier().equals(name)){
                x.setValue(value);
            }
        }

        for (Order x: completedTransactions){
            if (x.getIdentifier().equals(name)){
                x.setValue(value);
            }
        }
    }

    public BigDecimal calculateTotalDerivatives(){
        BigDecimal result = BigDecimal.ZERO;
        for (Order x: derivatives){
            BigDecimal value = x.getValue();
            BigDecimal units = x.getUnits();
            BigDecimal orderValue= value.multiply(units);
            result = result.add(orderValue);
        }
        return result;
    }


    public BigDecimal calculateTotalAum(){
        BigDecimal result = BigDecimal.ZERO;
        result = result.add(cash);
        result = result.add(calculateTotalDerivatives());
        return result;
    }


    /**
     * Checks if
     * 1) Order passed in is less than the existing transaction
     * If it is, we update the transaction, but we do not remove it
     * Else, we will remove it from the list of pending transactions; we will deduct the total units from assets and payout in cash
     */
    public void completeTransaction(Order order) throws Exception{
        boolean exists = false;
        int index = -1;
        for (int i = 0; i < transactions.size(); i++){
            Order x = transactions.get(i);
            if (x.getIdentifier().equals(order.getIdentifier())){
                exists = true;
                index = i;
            }
        }
        if (exists){
            Order x = transactions.get(index);
            completedTransactions.add(x);
            int result = x.getUnits().compareTo(order.getUnits());
            if (result == 1){
                transactions.get(index).setUnits(x.getUnits().subtract(order.getUnits()));
            } else {
                transactions.remove(index);
            }

            for(int i = 0; i < derivatives.size(); i++){
                Order y = derivatives.get(i);
                if (y.getIdentifier().equals(order.getIdentifier())){
                    y.setUnits(y.getUnits().add(order.getUnits()));
                    i = derivatives.size();
                }
            }
            BigDecimal newCash = order.getUnits().abs().multiply(order.getValue());
            addCash(newCash);
        } else {
            throw new Exception("tx not found");
        }
    }
}
