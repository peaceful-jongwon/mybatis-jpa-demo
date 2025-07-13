package kr.co.peacefuljw.mybatisjpademo.common.exception;

public class InvalidEmailException extends RuntimeException {
    public InvalidEmailException(String msg) {
        super(msg);
    }
}
