package exception;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

public class ErrorResponse {

    public ErrorResponse(String message, List<String> details) {
        super();
        this.message = message;
        this.details = details;
    }

    //General error message about nature of error
    @Getter
    @Setter
    private String message;

    //Specific errors in API request processing
    @Getter
    @Setter
    private List<String> details;
}
