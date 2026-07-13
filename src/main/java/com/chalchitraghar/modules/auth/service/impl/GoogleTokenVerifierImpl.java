package com.chalchitraghar.modules.auth.service.impl;

import com.chalchitraghar.modules.auth.dto.GoogleUserInfo;
import com.chalchitraghar.modules.auth.service.GoogleTokenVerifier;
import com.chalchitraghar.shared.exception.AuthenticationException;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GoogleTokenVerifierImpl implements GoogleTokenVerifier {

    @Value("${google.client-id}")
    private String googleClientId;

    private GoogleIdTokenVerifier verifier;

    @PostConstruct
    void initializeVerifier() throws Exception {
        verifier =
                new GoogleIdTokenVerifier.Builder(
                                GoogleNetHttpTransport.newTrustedTransport(),
                                GsonFactory.getDefaultInstance())
                        .setAudience(Collections.singletonList(googleClientId))
                        .build();
    }

    @Override
    public GoogleUserInfo verify(String idToken) {
        try {
            GoogleIdToken googleIdToken = verifier.verify(idToken);
            if (googleIdToken == null) {
                throw new AuthenticationException("Invalid Google ID token");
            }

            GoogleIdToken.Payload payload = googleIdToken.getPayload();
            String email = payload.getEmail();
            String name = (String) payload.get("name");
            String picture = (String) payload.get("picture");
            Boolean emailVerified = payload.getEmailVerified();

            return new GoogleUserInfo(
                    payload.getSubject(),
                    email,
                    name == null || name.isBlank() ? email : name,
                    picture,
                    Boolean.TRUE.equals(emailVerified));
        } catch (GeneralSecurityException | IOException e) {
            throw new AuthenticationException("Invalid Google ID token");
        }
    }
}
