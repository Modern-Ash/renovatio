package fixture;

public class CharacterizationFixture {
    public static String run() {
        PerformInlineDto input = new PerformInlineDto();
        return Integer.toString(new PerformInlineService().process(input).getWsTraceNum());
    }
}

class PerformInlineDto {
    private int wsIdx;
    private int wsTraceNum;

    public int getWsIdx() { return wsIdx; }

    public void setWsIdx(int wsIdx) { this.wsIdx = wsIdx; }

    public int getWsTraceNum() { return wsTraceNum; }

    public void setWsTraceNum(int wsTraceNum) { this.wsTraceNum = wsTraceNum; }
}

class PerformInlineService {
    public PerformInlineDto process(PerformInlineDto input) {
        {
            PerformInlineDto output = new PerformInlineDto();
            for (int wsIdx = 1; ; wsIdx += 1) {
                output.setWsTraceNum(wsIdx);
                if (wsIdx > 1) break;
            }
            return output;
        }
    }
}