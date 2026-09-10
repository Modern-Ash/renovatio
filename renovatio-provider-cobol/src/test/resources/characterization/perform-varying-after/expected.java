package fixture;

public class CharacterizationFixture {
    public static String run() {
        PerformVaryingAfterDto input = new PerformVaryingAfterDto();
        return Integer.toString(new PerformVaryingAfterService().process(input).getWsTraceNum());
    }
}

class PerformVaryingAfterDto {
    private int wsI;
    private int wsJ;
    private int wsTraceNum;

    public int getWsI() { return wsI; }

    public void setWsI(int wsI) { this.wsI = wsI; }

    public int getWsJ() { return wsJ; }

    public void setWsJ(int wsJ) { this.wsJ = wsJ; }

    public int getWsTraceNum() { return wsTraceNum; }

    public void setWsTraceNum(int wsTraceNum) { this.wsTraceNum = wsTraceNum; }
}

class PerformVaryingAfterService {
    public PerformVaryingAfterDto process(PerformVaryingAfterDto input) {
        {
            PerformVaryingAfterDto output = new PerformVaryingAfterDto();
            for (int wsI = 1; !(wsI > 2); wsI += 1) {
                for (int wsJ = 1; !(wsJ > 3); wsJ += 1) {
                    output.setWsTraceNum(wsJ);
                }
            }
            return output;
        }
    }
}
