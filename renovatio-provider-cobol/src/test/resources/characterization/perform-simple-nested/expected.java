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
        {
            PerformSimpleNestedDto output = new PerformSimpleNestedDto();
            performOuterPara(input, output);
            return output;
        }
    }
    @GeneratedFrom(paragraph = "OUTER-PARA", lines = "10-12")
    private void performOuterPara(PerformSimpleNestedDto input, PerformSimpleNestedDto out) {
        out.setWsTrace("1-OUTER");
        performInnerPara(input, out);
    }
    @GeneratedFrom(paragraph = "INNER-PARA", lines = "13-14")
    private void performInnerPara(PerformSimpleNestedDto input, PerformSimpleNestedDto out) {
        out.setWsTrace("2-INNER");
    }
}@interface GeneratedFrom {
    String paragraph();
    String lines();
}