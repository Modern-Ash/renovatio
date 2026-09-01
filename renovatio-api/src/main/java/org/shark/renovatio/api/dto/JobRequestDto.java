package org.shark.renovatio.api.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobRequestDto {
    @NotBlank(message = "Operation is required")
    private String operation;

    private Map<String, Object> params;
}
