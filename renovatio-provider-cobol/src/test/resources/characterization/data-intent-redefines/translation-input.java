package fixture;

public class CharacterizationFixture {
    public static String run() {
        return new RedefinesService().process(new RedefinesDto()).getRawRecord();
    }
}

class RedefinesDto {
    private String rawRecord;
    private Integer numericView;
    public String getRawRecord() { return rawRecord; }
    public void setRawRecord(String rawRecord) { this.rawRecord = rawRecord; }
    public Integer getNumericView() { return numericView; }
    public void setNumericView(Integer numericView) { this.numericView = numericView; }
}

class RedefinesService {
    public RedefinesDto process(RedefinesDto input) {
        RedefinesDto output = new RedefinesDto();
        return output;
    }
}
