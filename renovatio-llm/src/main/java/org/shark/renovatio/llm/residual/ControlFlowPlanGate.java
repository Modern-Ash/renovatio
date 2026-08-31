package org.shark.renovatio.llm.residual;

import com.fasterxml.jackson.databind.JsonNode;
import org.shark.renovatio.cobol.ir.annotated.AnnotatedCobolModel;

import java.util.Objects;

/** Discards ineligible GO TO proposals before they can enter the annotated sidecar. */
public final class ControlFlowPlanGate {
    public static final String DIAGNOSTIC = "LLM_CHARACTERIZATION_NOT_GREEN";
    public static final String MANUAL_ACTION = "Restructure the identified component manually or restore "
            + "a green characterization baseline before requesting another plan.";

    private final ResidualAnnotationAssembler assembler;

    public ControlFlowPlanGate(ResidualAnnotationAssembler assembler) {
        this.assembler = Objects.requireNonNull(assembler);
    }

    public Decision retainIfEligible(AnnotatedCobolModel sidecar, JsonNode validatedPlan,
                                     ResidualAnnotationContext context,
                                     CharacterizationEvidence evidence) {
        Objects.requireNonNull(sidecar, "sidecar");
        Objects.requireNonNull(validatedPlan, "validatedPlan");
        Objects.requireNonNull(context, "context");
        if (evidence == null || !evidence.isGreen()
                || context.characterizationBaselineRef() == null
                || !context.characterizationBaselineRef().equals(evidence.baselineRef())) {
            return new Decision(sidecar, false, DIAGNOSTIC, MANUAL_ACTION,
                    evidence == null ? null : evidence.baselineRef());
        }
        AnnotatedCobolModel enriched = assembler.append(sidecar, ResidualRoute.CONTROL_FLOW_PLAN,
                validatedPlan, context);
        return new Decision(enriched, true, null, null, evidence.baselineRef());
    }

    public record Decision(AnnotatedCobolModel sidecar, boolean proposalRetained,
                           String diagnosticCode, String manualAction, String baselineRef) {
    }
}
