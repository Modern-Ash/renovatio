package fixture;

import org.shark.renovatio.cobol.annotations.CobolDataIntent;

public class CharacterizationFixture {
    public static String run() {
        return new RedefinesService().process(new RedefinesDto()).getRawRecord();
    }
}

class RedefinesDto {
    private String rawRecord;
    @CobolDataIntent(nodeId = "e6b2813579d8d3acb3a0e152753d44bbeeea3ae1252ca7cfafee5088b02530a0", annotationId = "d851be1c6a6aa066afa07dc99cccdc088b32e8ac30fc43d7fcd3bea81653c8bb", construction = CobolDataIntent.Construction.REDEFINES, interpretation = "Numeric view overlays the raw record", assumptions = {"raw and numeric views share storage"})
    private Integer numericView;
    public String getRawRecord() { return rawRecord; }
    public void setRawRecord(String rawRecord) { this.rawRecord = rawRecord; }
    public Integer getNumericView() { return numericView; }
    public void setNumericView(Integer numericView) { this.numericView = numericView; }
}

class RedefinesService {
    public RedefinesDto process(RedefinesDto input) {
        {
            RedefinesDto output = new RedefinesDto();
            output.setRawRecord("00001234");
            return output;
        }
    }
}
