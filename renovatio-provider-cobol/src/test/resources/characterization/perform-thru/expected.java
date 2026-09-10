package fixture;

public class CharacterizationFixture {
    public static String run() {
        PerformThruDto input = new PerformThruDto();
        PerformThruDto output = new PerformThruService().process(input);
        return output.getWsFirst() + "|" + output.getWsSecond();
    }
}

class PerformThruDto {
    private String wsFirst;
    private String wsSecond;

    public String getWsFirst() { return wsFirst; }

    public void setWsFirst(String wsFirst) { this.wsFirst = wsFirst; }

    public String getWsSecond() { return wsSecond; }

    public void setWsSecond(String wsSecond) { this.wsSecond = wsSecond; }
}

class PerformThruService {
    public PerformThruDto process(PerformThruDto input) {
        {
            PerformThruDto output = new PerformThruDto();
            perform1000Para(input, output);
            perform2000Para(input, output);
            return output;
        }
    }
    @GeneratedFrom(paragraph = "1000-PARA", lines = "11-12")
    private void perform1000Para(PerformThruDto input, PerformThruDto out) {
        out.setWsFirst("1-1000");
    }
    @GeneratedFrom(paragraph = "2000-PARA", lines = "13-14")
    private void perform2000Para(PerformThruDto input, PerformThruDto out) {
        out.setWsSecond("2-2000");
    }
}@interface GeneratedFrom {
    String paragraph();
    String lines();
}