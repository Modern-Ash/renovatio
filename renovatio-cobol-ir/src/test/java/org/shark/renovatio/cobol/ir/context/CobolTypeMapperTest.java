package org.shark.renovatio.cobol.ir.context;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CobolTypeMapperTest {

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

