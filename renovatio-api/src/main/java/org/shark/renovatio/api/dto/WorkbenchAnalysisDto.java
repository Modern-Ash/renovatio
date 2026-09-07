package org.shark.renovatio.api.dto;

import java.util.List;
import java.util.Map;

public record WorkbenchAnalysisDto(Map<String, Long> inventory, List<RunDto> runs) { }
