package fixture;

public class CharacterizationFixture {
    public static String run() {
        PerformUntilDto input = new PerformUntilDto();
        return Integer.toString(new PerformUntilService().process(input).getWsCount());
    }
}

class PerformUntilDto {
    private int wsCount;

    public int getWsCount() { return wsCount; }

    public void setWsCount(int wsCount) { this.wsCount = wsCount; }
}

class PerformUntilService {
    public PerformUntilDto process(PerformUntilDto input) {
        {
            PerformUntilDto output = new PerformUntilDto();
            while (!(output.getWsCount() > 0)) {
                output.setWsCount(1);
            }
            return output;
        }
    }
}