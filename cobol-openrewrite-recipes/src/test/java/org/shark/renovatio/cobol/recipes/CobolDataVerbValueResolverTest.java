package org.shark.renovatio.cobol.recipes;

import org.junit.jupiter.api.Test;
import org.shark.renovatio.cobol.ir.model.CobolDataItem;
import org.shark.renovatio.cobol.ir.model.Level88Condition;
import org.shark.renovatio.cobol.ir.model.Level88Value;
import org.shark.renovatio.cobol.runtime.PicType;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.shark.renovatio.cobol.runtime.PicType.Category.ALPHANUMERIC;
import static org.shark.renovatio.cobol.runtime.PicType.Category.NUMERIC;
import static org.shark.renovatio.cobol.runtime.PicType.Usage.DISPLAY;

class CobolDataVerbValueResolverTest {

    @Test
    void initializeValuesRespectPicCategoryAndJavaType() {
        assertThat(CobolDataVerbValueResolver.initialValue(item(
                "NAME", "String", new PicType(ALPHANUMERIC, 4, 0, false, DISPLAY))))
                .contains("\" \".repeat(4)");
        assertThat(CobolDataVerbValueResolver.initialValue(item(
                "COUNT", "Integer", new PicType(NUMERIC, 3, 0, false, DISPLAY))))
                .contains("0");
        assertThat(CobolDataVerbValueResolver.initialValue(item(
                "AMOUNT", "BigDecimal", new PicType(NUMERIC, 5, 2, true, DISPLAY))))
                .contains("java.math.BigDecimal.ZERO");
    }

    @Test
    void falseConditionNeverInventsAValueOutsideAnUnsignedPicDomain() {
        CobolDataItem unsignedDigit = item(
                "DIGIT", "Integer", new PicType(NUMERIC, 1, 0, false, DISPLAY));
        Level88Condition allValues = new Level88Condition(
                "ANY-DIGIT", "DIGIT", List.of(Level88Value.range("0", "9")));

        assertThat(CobolDataVerbValueResolver.conditionValue(unsignedDigit, allValues, false)).isEmpty();

        CobolDataItem signedDigit = item(
                "SIGNED-DIGIT", "Integer", new PicType(NUMERIC, 1, 0, true, DISPLAY));
        assertThat(CobolDataVerbValueResolver.conditionValue(signedDigit, allValues, false))
                .contains("-1");
    }

    private static CobolDataItem item(String name, String javaType, PicType picType) {
        return new CobolDataItem(name, "", 1, null, null, javaType, picType, List.of());
    }
}
