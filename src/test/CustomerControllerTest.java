package test;

import controller.CustomerController;
import controller.OrderController;
import controller.SaxosController;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;

public class CustomerControllerTest {
    OrderController orderController;
    SaxosController saxosController;
    CustomerController customerController;

    @Before
    public void initialize() {
        orderController = new OrderController();
        saxosController = new SaxosController();
        customerController = new CustomerController(orderController, saxosController);
    }

    @Test
    public void beforeSettlingTest() throws Exception {
        // Given
        BigDecimal totalAum = customerController.getCustomersTotalAum();
        BigDecimal totalDerivatives = customerController.getCustomersTotalDerivatives();
        BigDecimal cash = customerController.getCustomersTotalCash();

        System.out.println(totalAum);
        System.out.println(totalDerivatives);
        System.out.println(cash);

        // Then
        assertEquals(0, totalAum.compareTo(totalDerivatives.add(cash)));
        assertEquals(0, totalAum.compareTo(BigDecimal.valueOf(12716.0344)));
        assert(customerController.allCustomersPositiveDerivative());
    }

    @Test
    public void afterSettlingTest() throws Exception {

        customerController.settleAllCustomersTransactions();
        BigDecimal totalAum = customerController.getCustomersTotalAum();
        BigDecimal totalDerivatives = customerController.getCustomersTotalDerivatives();
        BigDecimal cash = customerController.getCustomersTotalCash();

        //Make sure that no customers have pending transactions
        assert(customerController.allCustomersSettled());

        //Make sure that no customers have any derivatives that have negative units
        assert(customerController.allCustomersPositiveDerivative());

        System.out.println(totalAum);
        System.out.println(totalDerivatives);
        System.out.println(cash);

        //Make sure that the math makes sense
        assertEquals(0, totalAum.compareTo(totalDerivatives.add(cash)));
        assertEquals(0, totalAum.setScale(0, BigDecimal.ROUND_HALF_UP).compareTo(BigDecimal.valueOf(13646)));

        System.out.println(totalAum);
    }
}