package org.apereo.cas.util;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.Base64Utils;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;

public class MyPasswordEncoder implements PasswordEncoder {
    @Override
    public String encode(CharSequence rawPassword) {
        if(rawPassword==null) return null;
        //md5 -> byte[] ->Base64 ->passStr -> {MD5}passStr
        byte[] bs =  DigestUtils.md5Digest(rawPassword.toString().getBytes(StandardCharsets.UTF_8));
        String str =  "{MD5}"+Base64Utils.encodeToString(bs);
        System.out.println(str);
        return str;
    }
    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        if(rawPassword==null || encodedPassword==null) return false;
        return encodedPassword.equals(encode(rawPassword));
    }
}
