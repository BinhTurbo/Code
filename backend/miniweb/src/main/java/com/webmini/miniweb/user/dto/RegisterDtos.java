package com.webmini.miniweb.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RegisterDtos {
    public record RegisterRequest(

            @NotBlank(message = "Ten dang nhap khong de trong")
            @Size(min = 3, max = 100, message = "Ten dang nhap tu 3 - 100 ky tu")
            @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Tên đăng nhập chỉ được chứa chữ cái, số, dấu gạch dưới (_) và gạch ngang (-)")
            String username,

            @NotBlank(message = "Mật khẩu không được để trống")
            @Size(min = 8, max = 72, message = "Mật khẩu phải từ 8-72 ký tự")
            @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$", message = "Mật khẩu phải có ít nhất 1 chữ hoa, 1 chữ thường và 1 chữ số")
            String password,

            @NotBlank(message = "Họ tên không được để trống")
            @Size(min = 2, max = 150, message = "Họ tên phải từ 2-150 ký tự")
            @Pattern(regexp = "^[a-zA-ZÀ-ỹ\\s]+$", message = "Họ tên chỉ được chứa chữ cái và dấu cách")
            String fullName
    ) {}
}
