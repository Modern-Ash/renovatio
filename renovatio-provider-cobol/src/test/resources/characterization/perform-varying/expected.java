package fixture;

public class CharacterizationFixture {
    public static String run() {
        PerformVaryingDto input = new PerformVaryingDto();
        return Integer.toString(new PerformVaryingService().process(input).getWsTraceNum());
    }
}

class PerformVaryingDto {
    private int wsIdx;
    private int wsTraceNum;

    public int getWsIdx() { return wsIdx; }

    public void setWsIdx(int wsIdx) { this.wsIdx = wsIdx; }

    public int getWsTraceNum() { return wsTraceNum; }

    public void setWsTraceNum(int wsTraceNum) { this.wsTraceNum = wsTraceNum; }
}

class PerformVaryingService {
    public PerformVaryingDto process(PerformVaryingDto input) {
        {
            PerformVaryingDto output = new PerformVaryingDto();
            for (output.setWsIdx((int) (1)); !(output.getWsIdx() > 3); output.setWsIdx((int) (output.getWsIdx() + 1))) {
                output.setWsTraceNum(output.getWsIdx());
            }
            return output;
        }
    }
}