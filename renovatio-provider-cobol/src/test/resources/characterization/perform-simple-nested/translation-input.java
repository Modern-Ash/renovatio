package fixture;

public class CharacterizationFixture {
    public static String run() {
        PerformSimpleNestedDto input = new PerformSimpleNestedDto();
        return new PerformSimpleNestedService().process(input).getWsTrace();
    }
}

class PerformSimpleNestedDto {
    private String wsTrace;

    public String getWsTrace() { return wsTrace; }

    public void setWsTrace(String wsTrace) { this.wsTrace = wsTrace; }
}

class PerformSimpleNestedService {
    public PerformSimpleNestedDto process(PerformSimpleNestedDto input) {
        PerformSimpleNestedDto output = new PerformSimpleNestedDto();
        return output;
    }
}