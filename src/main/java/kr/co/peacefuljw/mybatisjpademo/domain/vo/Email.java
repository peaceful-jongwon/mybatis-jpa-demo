package kr.co.peacefuljw.mybatisjpademo.domain.vo;

import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.regex.Pattern;

@Value
@EqualsAndHashCode
public class Email {

    private static final String EMAIL_PATTERN =
            "^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$";

    private static final Pattern pattern = Pattern.compile(EMAIL_PATTERN);

    String value;

    public Email(String value) {
        if (value == null || !isValidFormat(value)) {
            throw new IllegalArgumentException("올바르지 않은 이메일 형식입니다: " + value);
        }
        this.value = value;
    }

    public boolean isValid() {
        return isValidFormat(this.value);
    }

    private boolean isValidFormat(String email) {
        return pattern.matcher(email).matches();
    }

    @Override
    public String toString() {
        return value;
    }
}
