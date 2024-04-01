package leaguehub.leaguehubbackend.domain.notice.exception;

import leaguehub.leaguehubbackend.domain.notice.exception.exception.NoticeUnsupportedException;
import leaguehub.leaguehubbackend.domain.notice.exception.exception.WebScrapingException;
import leaguehub.leaguehubbackend.global.exception.global.ExceptionCode;
import leaguehub.leaguehubbackend.global.exception.global.ExceptionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class NoticeExceptionHandler {

    @ExceptionHandler(NoticeUnsupportedException.class)
    public ResponseEntity<ExceptionResponse> noticeUnsupportedException(
            NoticeUnsupportedException e
    ) {
        ExceptionCode exceptionCode = e.getExceptionCode();
        log.error("{}", exceptionCode.getMessage());

        return new ResponseEntity<>(
                new ExceptionResponse(exceptionCode),
                exceptionCode.getHttpStatus()
        );
    }

    @ExceptionHandler(WebScrapingException.class)
    public ResponseEntity<ExceptionResponse> webScrapingException(
            WebScrapingException e
    ) {
        ExceptionCode exceptionCode = e.getExceptionCode();
        log.error("{}", exceptionCode.getMessage());

        return new ResponseEntity<>(
                new ExceptionResponse(exceptionCode),
                exceptionCode.getHttpStatus()
        );
    }


}
