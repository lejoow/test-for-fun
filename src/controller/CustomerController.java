package controller;

import model.Customer;
import model.Order;
import model.SaxosReceipt;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

public class CustomerController {
    OrderController orderController;
    SaxosController saxosController;
    HashMap<String, Customer> customerMap;

    public CustomerController(OrderController orderController, SaxosController saxosController){
        this.orderController = orderController;
        this.saxosController = saxosController;

        String csvFile = "src/resources/customer.csv";
        BufferedReader br = null;
        String line = "";
        String cvsSplitBy = ",";

        customerMap = new HashMap<>();

        try {

            br = new BufferedReader(new FileReader(csvFile));
            while ((line = br.readLine()) != null) {

                // use comma as separator
                String[] customerOrders = line.split(cvsSplitBy);
                if(customerMap.get(customerOrders[0]) == null){
                    Customer customer = new Customer(customerOrders[0]);
                    String identifierStr = customerOrders[1];
                    BigDecimal assetUnits = new BigDecimal(customerOrders[2]);
                    BigDecimal txUnits = new BigDecimal(customerOrders[3]);
                    BigDecimal value = orderController.getValue(identifierStr);
                    Order tx = new Order(txUnits, value, identifierStr);
                    Order asset = new Order(assetUnits, value, identifierStr);
                    customer.addTransactions(tx);
                    customer.addDerivatives(asset);
                    customerMap.put(customerOrders[0], customer);

                } else {
                    Customer c = customerMap.get(customerOrders[0]);
                    String identifierStr = customerOrders[1];
                    BigDecimal assetUnits = new BigDecimal(customerOrders[2]);
                    BigDecimal txUnits = new BigDecimal(customerOrders[3]);
                    BigDecimal value = orderController.getValue(identifierStr);
                    Order tx = new Order(txUnits, value, identifierStr);
                    Order asset = new Order(assetUnits, value, identifierStr);
                    c.addTransactions(tx);
                    c.addDerivatives(asset);
                    customerMap.put(customerOrders[0], c);

                }
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public List<Customer> getCustomers(){
        return new ArrayList(customerMap.values());
    }

    public BigDecimal getCustomersTotalAum(){
        BigDecimal result = BigDecimal.ZERO;
        for (Customer x: customerMap.values()){
            result = result.add(x.calculateTotalAum());
        }
        return result;
    }

    public BigDecimal getCustomersTotalCash(){
        BigDecimal result = BigDecimal.ZERO;
        for (Customer x: customerMap.values()){
            result = result.add(x.getCash());
        }
        return result;
    }

    public BigDecimal getCustomersTotalDerivatives(){
        BigDecimal result = BigDecimal.ZERO;
        for (Customer x: customerMap.values()){
            result = result.add(x.calculateTotalDerivatives());
        }
        return result;
    }

    public boolean allCustomersSettled(){
        boolean result = true;
        for (Customer x: customerMap.values()){
            if (x.getTransactions().size() > 0){
                result = false;
            }
        }
        return result;
    }

    public boolean allCustomersPositiveDerivative(){
        boolean result = true;
        for (Customer x: customerMap.values()){
            List<Order> derivatives = x.getDerivatives();
            for (Order o: derivatives){
                if (o.getUnits().compareTo(BigDecimal.ZERO) == -1){
                    result = false;
                }
            }
        }
        return result;
    }

    /**
     * TODO: Complete this method to pass the JUnit test
     *
     * Scenario: All customers have a list of pending transactions (getTransactions()), a list of current holdings (getDerivatives()), amongst other things. All these are represented by simplistically as Order objects.
     * Objective: All pending transactions need to be settled by sending relevant orders to our brokerage, Saxos
     * Methodology: This is done by sending a List of Order objects to SaxosController by calling processOrder()
     *
     * Saxos only accepts integer units. No 20.33 units.
     * Negative units are to sell the orders
     * Positive units are to buy the orders
     * Saxos will charge a transaction fee of $5 for each Order in the List of Order objects
     * Saxos will return a List of SaxosReceipt objects
     *
     * You can use the completeTransaction() method in Customer which takes in an Order object.
     * THIS WILL GENERATED BY YOU.
     * The complete transaction method checks the identifier, as well as the number of units in the passed Order object
     * If the passed Order object's units is LESS THAN the pending order's units, the pending transaction will not be removed.
     * The pending transaction will deduct what was completed and keep the remainder of the units. e.g. if completed order has 10 units, and pending is 11. Pending will now have 1 unit.
     *
     * If the completed order is LARGER or EQUALS to the pending transaction, the pending transaction is considered completed, and it is removed.
     *
     * Following which, the completed order will be deducted from the current holding, and the gains from the sale will be placed in the customer's cash (getCash()).
     * So if the completed order is 10 units, the holding is 100 units; the remainder will be 90 units.
     *
     * Do not over or undersell by too large a margin. No customer should be left holding derivatives with negative units. e.g. holding has -10 units.
     *
     */
    public void settleAllCustomersTransactions() throws Exception{
        List<Order> orders = new ArrayList<>();
        Map<String, List<Order>> ordersByCustomer = new HashMap<>();

        // combine
        for (Customer x: customerMap.values()) {
            for (Order order: x.getTransactions()) {
                if (orderExists(order, orders)) {
                    combineOrder(order, orders);
                }
                else {
                    orders.add(new Order(order));
                }
            }
        }
        Collections.sort(orders, (o1, o2) -> o1.getIdentifier().compareTo(o2.getIdentifier()));
        System.out.println("Combined:\t" + orders);

        // remove fraction
        List<Order> refinedOrders = new ArrayList<>();
        for (Order order: orders) {
            BigDecimal units = order.getUnits();
            Order o = order;
            BigDecimal fraction = BigDecimal.ZERO;
            if (hasFraction(units)) {
                fraction = units.remainder(BigDecimal.ONE);
                BigDecimal completeUnits = order.getUnits().subtract(fraction);
                o = new Order(completeUnits, order.getValue(), order.getIdentifier());
            }
            refinedOrders.add(o);

            // calculate the actual amount of units purchased by each customer
            // by distributing the fraction equally
            List<Customer> customers = whoOrdered(order, customerMap.values());
            BigDecimal toDeduct = fraction.divide(BigDecimal.valueOf(customers.size()), 5, BigDecimal.ROUND_HALF_UP);
            for (Customer customer: customers) {
                List<Order> customerOrders = new ArrayList<>();
                if (ordersByCustomer.containsKey(customer.getName())) {
                    customerOrders = ordersByCustomer.get(customer.getName());
                }
                Optional<Order> pendingTransaction = customer.findPendingTransaction(order.getIdentifier());
                if (pendingTransaction.isPresent()) {
                    Order original = pendingTransaction.get();
                    BigDecimal purchasedUnits = original.getUnits().subtract(toDeduct);
                    customerOrders.add(new Order(purchasedUnits, original.getValue(), original.getIdentifier()));
                    ordersByCustomer.put(customer.getName(), customerOrders);
                }
            }
        }
        System.out.println("To Saxos:\t" + refinedOrders);

        // send
        List<SaxosReceipt> receipts = saxosController.processOrder(refinedOrders);

        System.out.println("From Saxos:\t" + receipts);

        // ignore this tiny monster
        Map<String, List<Order>> originalOrders = customerMap.values()
                                                             .stream()
                                                             .collect(toMap(Customer::getName,
                                                                 c -> c.getTransactions()
                                                                       .stream()
                                                                       .sorted((o1, o2) -> o1.getIdentifier().compareTo(o2.getIdentifier()))
                                                                       .collect(toList())));
        System.out.println("Original/Cust:\t" + originalOrders);
        System.out.println("Actual/Cust:\t" + ordersByCustomer);

        // calculate return percentage per customer
        for (SaxosReceipt receipt : receipts) {
            String orderId = receipt.getIdentifier();
            BigDecimal pricePerUnit = receipt.getTotalValue().divide(receipt.getTotalUnits(), 5, BigDecimal.ROUND_HALF_UP);
            for (Customer customer: customerMap.values()) {
                String customerId = customer.getName();

                Optional<Order> purchased = findPurchased(orderId, ordersByCustomer.get(customerId));
                if (purchased.isPresent()) {
                    BigDecimal share = pricePerUnit.multiply(purchased.get().getUnits());
                    Order completedOrder = new Order(purchased.get().getUnits(), share, orderId);
                    customer.completeTransaction(completedOrder);
                }
            }
        }
    }

    private Optional<Order> findPurchased(String orderId, List<Order> customerOrders) {
        return customerOrders.stream()
                             .filter(o -> o.getIdentifier().equals(orderId))
                             .findFirst();
    }

    private List<Customer> whoOrdered(Order order, Collection<Customer> customers) {
        return customers.stream()
                        .filter(customer -> customer.getTransactions()
                                                    .stream()
                                                    .filter(o -> o.getIdentifier().equals(order.getIdentifier()))
                                                    .findFirst().isPresent()
                        )
                        .collect(toList());
    }

    private boolean orderExists(Order order, List<Order> orders) {
        for (Order existing: orders) {
            if (existing.getIdentifier().equals(order.getIdentifier())) {
                return true;
            }
        }
        return false;
    }

    private void combineOrder(Order order, List<Order> orders) {
      if (order.getUnits().compareTo(BigDecimal.ZERO) != 0) {
          orders.stream()
                .filter(o -> o.getIdentifier().equals(order.getIdentifier()))
                .forEach(o -> o.setUnits(o.getUnits().add(order.getUnits())));
      }
    }

    private boolean hasFraction(BigDecimal bd) {
        return !(bd.signum() == 0 || bd.scale() <= 0 || bd.stripTrailingZeros().scale() <= 0);
    }
}