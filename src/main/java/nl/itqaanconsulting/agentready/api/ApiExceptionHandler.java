package nl.itqaanconsulting.agentready.api;

import nl.itqaanconsulting.agentready.zaak.OngeldigZaaknummerException;
import nl.itqaanconsulting.agentready.zaak.ZaakNietGevondenException;
import nl.itqaanconsulting.agentready.zaak.ZaaksysteemNietBeschikbaarException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** RFC 9457 problem details; never leaks legacy error details to the caller. */
@RestControllerAdvice
class ApiExceptionHandler {

    @ExceptionHandler(OngeldigZaaknummerException.class)
    ProblemDetail invalid(OngeldigZaaknummerException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(ZaakNietGevondenException.class)
    ProblemDetail notFound(ZaakNietGevondenException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(ZaaksysteemNietBeschikbaarException.class)
    ProblemDetail unavailable(ZaaksysteemNietBeschikbaarException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
                "Het zaaksysteem is tijdelijk niet beschikbaar");
    }
}
