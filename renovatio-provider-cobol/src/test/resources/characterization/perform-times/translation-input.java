package fixture;

public class CharacterizationFixture {
    public static String run() {
        PerformTimesDto input = new PerformTimesDto();
        return new PerformTimesService().process(input).getWsTrace();
    }
}

class PerformTimesDto {
    private String wsTrace;

    public String getWsTrace() { return wsTrace; }

    public void setWsTrace(String wsTrace) { this.wsTrace = wsTrace; }
}

class PerformTimesService {
    public PerformTimesDto process(PerformTimesDto input) {
        PerformTimesDto output = new PerformTimesDto();
        return output;
    }
}