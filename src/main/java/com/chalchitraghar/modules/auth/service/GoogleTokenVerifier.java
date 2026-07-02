package com.chalchitraghar.modules.auth.service;

import com.chalchitraghar.modules.auth.dto.GoogleUserInfo;

public interface GoogleTokenVerifier {

    GoogleUserInfo verify(String idToken);
}
