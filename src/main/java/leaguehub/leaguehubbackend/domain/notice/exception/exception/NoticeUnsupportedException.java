package leaguehub.leaguehubbackend.domain.notice.exception.exception;

import leaguehub.leaguehubbackend.domain.notice.exception.NoticeExceptionCode;
import leaguehub.leaguehubbackend.global.exception.global.ExceptionCode;
import leaguehub.leaguehubbackend.global.exception.global.exception.ResourceNotFoundException;

public class NoticeUnsupportedException extends ResourceNotFoundException {

    private final ExceptionCode exceptionCode;

    public NoticeUnsupportedException() {
        super(NoticeExceptionCode.NOTICE_UNSUPPORTED);
        this.exceptionCode = NoticeExceptionCode.NOTICE_UNSUPPORTED;
    }

    @Override
    public ExceptionCode getExceptionCode() {
        return exceptionCode;
    }
}
