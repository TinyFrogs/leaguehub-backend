package leaguehub.leaguehubbackend.domain.notice.exception.exception;

import leaguehub.leaguehubbackend.domain.notice.exception.NoticeExceptionCode;
import leaguehub.leaguehubbackend.global.exception.global.ExceptionCode;
import leaguehub.leaguehubbackend.global.exception.global.exception.ResourceNotFoundException;

public class WebScrapingException extends ResourceNotFoundException {

    private final ExceptionCode exceptionCode;

    public WebScrapingException() {
        super(NoticeExceptionCode.WEB_SCRAPING_ERROR);
        this.exceptionCode = NoticeExceptionCode.WEB_SCRAPING_ERROR;
    }

    @Override
    public ExceptionCode getExceptionCode() {
        return exceptionCode;
    }
}
