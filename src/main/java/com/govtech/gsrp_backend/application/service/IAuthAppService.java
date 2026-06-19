package com.govtech.gsrp_backend.application.service;

import com.govtech.gsrp_backend.application.dto.JwtResponse;
import com.govtech.gsrp_backend.application.dto.LoginRequest;
import com.govtech.gsrp_backend.application.dto.MessageResponse;
import com.govtech.gsrp_backend.application.dto.SignupRequest;

public interface IAuthAppService {
    JwtResponse authenticateUser(LoginRequest loginRequest);
    MessageResponse registerUser(SignupRequest signUpRequest);
    JwtResponse getCurrentUser();
}
