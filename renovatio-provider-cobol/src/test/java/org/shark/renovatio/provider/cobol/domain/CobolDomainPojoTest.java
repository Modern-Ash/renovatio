package org.shark.renovatio.provider.cobol.domain;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CobolDomainPojoTest {

    @Test
    void cobolMcpTool_basic_fields() {
        CobolMcpTool t = new CobolMcpTool();
        t.setName("n");
        t.setDescription("d");
        t.setInputSchema(Map.of("type", "object"));
        assertEquals("n", t.getName());
        assertEquals("d", t.getDescription());
        assertEquals("object", ((Map<?, ?>) t.getInputSchema()).get("type"));

        CobolMcpTool t2 = new CobolMcpTool("n2", "d2");
        assertEquals("n2", t2.getName());
        assertEquals("d2", t2.getDescription());
    }

    @Test
    void cobolProgram_and_divisions() {
        CobolProgram p = new CobolProgram("ID1", "PROG1");
        assertEquals("ID1", p.getProgramId());
        assertEquals("PROG1", p.getProgramName());

        // EnvironmentDivision
        CobolEnvironmentDivision env = new CobolEnvironmentDivision();
        env.setConfigurationSection(Map.of("CFG", "V"));
        env.setInputOutputSection(Map.of("IN", "OUT"));
        p.setEnvironmentDivision(env);
        assertEquals("V", p.getEnvironmentDivision().getConfigurationSection().get("CFG"));
        assertEquals("OUT", p.getEnvironmentDivision().getInputOutputSection().get("IN"));

        // DataDivision
        CobolDataDivision data = new CobolDataDivision();
        CobolDataItem item = new CobolDataItem("A", 1, "X(10)");
        data.setWorkingStorageSection(List.of(item));
        data.setFileSection(List.of());
        data.setLinkageSection(List.of());
        p.setDataDivision(data);
        assertEquals(1, p.getDataDivision().getWorkingStorageSection().size());

        // ProcedureDivision
        CobolProcedureDivision proc = new CobolProcedureDivision();
        CobolParagraph para = new CobolParagraph();
        para.setName("P1");
        CobolSection sec = new CobolSection();
        sec.setName("S1");
        sec.setParagraphs(List.of(para));
        proc.setParagraphs(List.of(para));
        proc.setSections(List.of(sec));
        p.setProcedureDivision(proc);
        assertEquals("P1", p.getProcedureDivision().getParagraphs().get(0).getName());
        assertEquals("S1", p.getProcedureDivision().getSections().get(0).getName());

        p.setMetadata(Map.of("m", 1));
        assertEquals(1, p.getMetadata().get("m"));
    }

    @Test
    void cobolDataItem_getJavaType_branches() {
        CobolDataItem n = new CobolDataItem();
        // null picture -> Object
        assertEquals("Object", n.getJavaType());

        CobolDataItem d1 = new CobolDataItem("N1", 1, "9(5)");
        assertEquals("Integer", d1.getJavaType());

        CobolDataItem d2 = new CobolDataItem("N2", 1, "9(12)");
        assertEquals("Long", d2.getJavaType());

        CobolDataItem d3 = new CobolDataItem("N3", 1, "9(5)V99");
        assertEquals("BigDecimal", d3.getJavaType());

        CobolDataItem s1 = new CobolDataItem("S1", 1, "X(10)");
        assertEquals("String", s1.getJavaType());

        CobolDataItem s2 = new CobolDataItem("S2", 1, "A(5)");
        assertEquals("String", s2.getJavaType());

        CobolDataItem other = new CobolDataItem("O", 1, "Z(3)");
        assertEquals("Object", other.getJavaType());
    }

    @Test
    void cobolStatement_and_paragraph_section_setters() {
        CobolStatement st = new CobolStatement();
        st.setType(CobolStatement.StatementType.MOVE);
        st.setSourceCode("MOVE A TO B");
        st.setAttributes(Map.of("k", "v"));
        assertEquals(CobolStatement.StatementType.MOVE, st.getType());
        assertEquals("MOVE A TO B", st.getSourceCode());
        assertEquals("v", st.getAttributes().get("k"));

        CobolParagraph p = new CobolParagraph();
        p.setName("P");
        p.setStatements(List.of(st));
        assertEquals("P", p.getName());
        assertEquals(1, p.getStatements().size());

        CobolSection s = new CobolSection();
        s.setName("S");
        s.setParagraphs(List.of(p));
        assertEquals("S", s.getName());
        assertEquals(1, s.getParagraphs().size());
    }
}
