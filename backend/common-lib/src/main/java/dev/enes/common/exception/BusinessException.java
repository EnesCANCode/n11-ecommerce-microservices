package dev.enes.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

import java.net.URI;

public class BusinessException extends ErrorResponseException {

    public BusinessException(HttpStatus status, String detail) {
        super(status, asProblemDetail(status, detail), null);
    }

    private static ProblemDetail asProblemDetail(HttpStatus status, String detail) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setType(URI.create("https://api.n11.dev/errors/" + status.value()));
        pd.setTitle(status.getReasonPhrase());
        return pd;
    }
}
