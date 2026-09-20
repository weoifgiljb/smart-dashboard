package com.selfdiscipline.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public class BulkStatusRequest {
    @NotEmpty(message = "ids 不能为空")
    private List<String> ids;

    @NotBlank(message = "status 不能为空")
    private String status;

    public List<String> getIds() {
        return ids;
    }

    public void setIds(List<String> ids) {
        this.ids = ids;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
