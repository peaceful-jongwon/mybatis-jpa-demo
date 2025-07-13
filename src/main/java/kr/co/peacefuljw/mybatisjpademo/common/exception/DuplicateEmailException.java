package kr.co.peacefuljw.mybatisjpademo.common.exception;

public class DuplicateEmailException extends RuntimeException {
    public DuplicateEmailException(String msg) {
        super(msg);
    }
}
