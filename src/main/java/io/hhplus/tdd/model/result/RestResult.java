package io.hhplus.tdd.model.result;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RestResult {

    private String status;
    private String message;
    private Map<String,Object> data;

    public RestResult(String status, String message) {
        this.status = status;
        this.message = message;
    }


}
