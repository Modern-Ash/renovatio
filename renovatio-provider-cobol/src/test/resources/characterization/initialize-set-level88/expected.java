package fixture;

public class CharacterizationFixture {
    public static String run() {
        CustomerDto result = new CustomerService().process(new CustomerDto());
        return "[" + result.getCustomerName() + "]:" + result.getCustomerCount()
                + ":[" + result.getCustomerStatus() + "]";
    }
}

class CustomerDto {
    private String customerName;
    private Integer customerCount;
    private String customerStatus;
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public Integer getCustomerCount() { return customerCount; }
    public void setCustomerCount(Integer customerCount) { this.customerCount = customerCount; }
    public String getCustomerStatus() { return customerStatus; }
    public void setCustomerStatus(String customerStatus) { this.customerStatus = customerStatus; }
}

class CustomerService {
    public CustomerDto process(CustomerDto input) {
        {
            CustomerDto output = new CustomerDto();
            output.setCustomerName(" ".repeat(4));
            output.setCustomerCount(0);
            output.setCustomerStatus("Y");
            output.setCustomerStatus(" ");
            return output;
        }
    }
}
