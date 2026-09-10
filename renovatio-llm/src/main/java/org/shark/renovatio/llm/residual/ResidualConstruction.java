package org.shark.renovatio.llm.residual;

/** Deterministically recognized construction before any residual enrichment is considered. */
public enum ResidualConstruction {
    MOVE,
    COMPUTE,
    IF,
    EVALUATE,
    SIMPLE_PERFORM,
    BASIC_PIC,
    LEVEL_88,
    PARAGRAPH,
    DATA_ITEM,
    CONTROL_FLOW_COMPONENT,
    REDEFINES,
    OCCURS_DEPENDING_ON,
    MOVE_CORRESPONDING,
    COMPUTE_OVERFLOW,
    UNSUPPORTED
}
