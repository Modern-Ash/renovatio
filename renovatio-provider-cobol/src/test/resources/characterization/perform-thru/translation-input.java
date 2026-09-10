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
        PerformThruDto output = new PerformThruDto();
        return output;
    }
}