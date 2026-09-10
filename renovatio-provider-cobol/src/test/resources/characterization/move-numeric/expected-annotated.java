package fixture;

public class CharacterizationFixture {
    public static String run() {
        NumericDto input = new NumericDto();
        input.setSourceNum(-12.34);
        return Double.toString(new NumericService().process(input).getRoundedTotal());
    }
}

class NumericDto {
    private double sourceNum;
    private double roundedTotal;
    public double getSourceNum() { return sourceNum; }
    public void setSourceNum(double sourceNum) { this.sourceNum = sourceNum; }
    public double getRoundedTotal() { return roundedTotal; }
    public void setRoundedTotal(double targetNum) { this.roundedTotal = targetNum; }
}

class NumericService {
    public NumericDto process(NumericDto input) {
        {
            NumericDto output = new NumericDto();
            output.setRoundedTotal(input.getSourceNum());
            return output;
        }
    }
}
