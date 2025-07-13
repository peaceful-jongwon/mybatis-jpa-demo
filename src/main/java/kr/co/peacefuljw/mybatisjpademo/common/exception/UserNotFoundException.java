package kr.co.peacefuljw.mybatisjpademo.common.exception;

public class UserNotFoundException extends RuntimeException{

    public UserNotFoundException(String s) {
        super(s);
    }
}
