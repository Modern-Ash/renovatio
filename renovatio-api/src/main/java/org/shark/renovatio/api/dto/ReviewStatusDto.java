package org.shark.renovatio.api.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewStatusDto {
    @NotBlank(message = "Status is required")
    @Pattern(regexp = "ACCEPTED|REJECTED", message = "Status must be ACCEPTED or REJECTED")
    private String status;
}
