package org.shark.renovatio.cobol.ir.context;

import org.junit.jupiter.api.Test;
import org.shark.renovatio.cobol.runtime.PicType;

import static org.junit.jupiter.api.Assertions.*;

class CobolTypeMapperTest {

    @Test
    void picType_exposesRichDescriptor() {
        PicType t = CobolTypeMapper.picType("PIC S9(4)V99 COMP-3");
        assertEquals(PicType.Category.NUMERIC, t.category());
        assertEquals(6, t.digits());
        assertEquals(2, t.scale());
        assertTrue(t.signed());
        assertEquals(PicType.Usage.COMP_3, t.usage());
    }

    @Test
    void picType_returnsNullForUnparseablePicture() {
        assertNull(CobolTypeMapper.picType("   "));
        assertNull(CobolTypeMapper.picType("???"));
    }

    @Test
    void picType_returnsNullWhenPictureParsingThrows() {
        assertNull(CobolTypeMapper.picType("9(99999999999999)"));
    }

    @Test
    void picToJavaType_shouldMapVariousPICs() {
        assertEquals("String", CobolTypeMapper.picToJavaType(null));
        assertEquals("String", CobolTypeMapper.picToJavaType("   "));
        assertEquals("Integer", CobolTypeMapper.picToJavaType("PIC 9(5)"));
        assertEquals("Long", CobolTypeMapper.picToJavaType("9(12)"));
        assertEquals("BigDecimal", CobolTypeMapper.picToJavaType("9(20)"));
        assertEquals("BigDecimal", CobolTypeMapper.picToJavaType("9(5)V9"));
        assertEquals("String", CobolTypeMapper.picToJavaType("X(10)"));
        assertEquals("String", CobolTypeMapper.picToJavaType("A(10)"));
        assertEquals("Integer", CobolTypeMapper.picToJavaType("9(3) COMP"));
        assertEquals("String", CobolTypeMapper.picToJavaType("???"));
    }
}

