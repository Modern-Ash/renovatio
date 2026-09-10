package fixture;

public class CharacterizationFixture {
    public static String run() {
        PerformUntilAfterDto input = new PerformUntilAfterDto();
        return Integer.toString(new PerformUntilAfterService().process(input).getWsCount());
    }
}

class PerformUntilAfterDto {
    private int wsCount;

    public int getWsCount() { return wsCount; }

    public void setWsCount(int wsCount) { this.wsCount = wsCount; }
}

class PerformUntilAfterService {
    public PerformUntilAfterDto process(PerformUntilAfterDto input) {
        {
            PerformUntilAfterDto output = new PerformUntilAfterDto();
            do {
                output.setWsCount(1);
            } while (!(output.getWsCount() > 0));
            return output;
        }
    }
}
