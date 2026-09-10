package fixture;

public class CharacterizationFixture {
    public static String run() {
        NumericDto input = new NumericDto();
        input.setSourceNum(-12.34);
        return Double.toString(new NumericService().process(input).getTargetNum());
    }
}

class NumericDto {
    private double sourceNum;
    private double targetNum;
    public double getSourceNum() { return sourceNum; }
    public void setSourceNum(double sourceNum) { this.sourceNum = sourceNum; }
    public double getTargetNum() { return targetNum; }
    public void setTargetNum(double targetNum) { this.targetNum = targetNum; }
}

class NumericService {
    public NumericDto process(NumericDto input) {
        {
            NumericDto output = new NumericDto();
            output.setTargetNum(input.getSourceNum());
            return output;
        }
    }
}
