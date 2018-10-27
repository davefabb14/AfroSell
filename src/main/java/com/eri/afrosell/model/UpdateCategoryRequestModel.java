package com.eri.afrosell.model;

import lombok.Data;
import org.hibernate.validator.constraints.NotBlank;

import javax.validation.constraints.NotNull;

@Data
public class UpdateCategoryRequestModel {

    private long id;

    private Long parentId;

    @NotNull
    @NotBlank
    private String name;

    private Integer position;

    private String description;
}
