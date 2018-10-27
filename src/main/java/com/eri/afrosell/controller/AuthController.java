/*
 * Copyright (c) NIT-Software. All Rights Reserved.
 * This software is the confidential and proprietary information of NIT-Software,
 * ("Confidential Information").
 * You shall not disclose such Confidential Information and shall use it only in accordance
 * with the terms of the license agreement you entered into with NIT-Software.
 */
package com.eri.afrosell.controller;
import com.eri.afrosell.exceptions.ApplicationException;
import com.eri.afrosell.model.AuthRequestModel;
import com.eri.afrosell.model.User;
import com.eri.afrosell.model.UserAddress;
import com.eri.afrosell.model.UserDetailResponseModel;
import com.eri.afrosell.model.UserToken;
import com.eri.afrosell.response.APIResponse;
import com.eri.afrosell.response.util.APIStatus;
import com.eri.afrosell.service.adressService.UserAddressService;
import com.eri.afrosell.service.auth.AuthService;
import com.eri.afrosell.service.auth.AuthUserFactory;
import com.eri.afrosell.util.Constant;
import com.eri.afrosell.util.MD5Hash;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.security.NoSuchAlgorithmException;

/**
 *
 * @author Quy Duong
 */
@RestController
@RequestMapping(APINames.AUTH_API)
class AuthController extends AbstractBaseController {

    @Autowired
    AuthService authService;

    @Autowired
    AuthUserFactory authUserFactory;

    @Autowired
    UserAddressService userAddressService;

    @RequestMapping(path = APINames.ADMIN_LOGIN_API, method = RequestMethod.POST)
    public ResponseEntity<APIResponse> adminLogin(
            @PathVariable("company_id") Long companyId,
            @RequestBody AuthRequestModel authRequestModel
    ) {
        User adminUser = authService.getUserByEmailAndCompanyIdAndStatus(authRequestModel.getUsername(), companyId, Constant.USER_STATUS.ACTIVE.getStatus());
        if (adminUser == null) {
            throw new ApplicationException(APIStatus.ERR_USER_NOT_EXIST);
        } else {
            String passwordHash = null;
            try {
                passwordHash = MD5Hash.MD5Encrypt(authRequestModel.password + adminUser.getSalt());
            } catch (NoSuchAlgorithmException ex) {
                throw new RuntimeException("User login encrypt password error", ex);
            }

            if (passwordHash.equals(adminUser.getPasswordHash())) {
                // Check role
                if (adminUser.getRoleId() == Constant.USER_ROLE.SYS_ADMIN.getRoleId()
                        || adminUser.getRoleId() == Constant.USER_ROLE.STORE_MANAGER.getRoleId()
                        || adminUser.getRoleId() == Constant.USER_ROLE.STORE_ADMIN.getRoleId()) {

                    // TODO login
                    UserToken userToken = authService.createUserToken(adminUser, authRequestModel.isKeepMeLogin());
                    // Create Auth User -> Set to filter config
                    // Perform the security
                    Authentication authentication = new UsernamePasswordAuthenticationToken(
                            adminUser.getEmail(),
                            adminUser.getPasswordHash()
                    );
                    //final Authentication authentication = authenticationManager.authenticate();
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    return responseUtil.successResponse(userToken.getToken());
                } else {
                    throw new ApplicationException(APIStatus.ERR_PERMISSION_DENIED);
                }
            } else {
                throw new ApplicationException(APIStatus.ERR_USER_NOT_VALID);
            }

        }
    }

    @RequestMapping(path = APINames.SESSION_DATA, method = RequestMethod.GET)
    public ResponseEntity<APIResponse> getSessionData(
            HttpServletRequest request,
            @PathVariable("company_id") Long companyId
    ) {
        String userId = getAuthUserFromSession(request).getId();
        if (userId != null && !"".equals(userId)) {
            User user = authService.getUserByUserIdAndCompanyIdAndStatus(userId, companyId, Constant.USER_STATUS.ACTIVE.getStatus());
            UserAddress userAddress = userAddressService.getAddressByUserIdAndStatus(userId, Constant.STATUS.ACTIVE_STATUS.getValue());
            if (user != null) {
                UserDetailResponseModel userResponse = new UserDetailResponseModel();
                userResponse.setUserId(user.getUserId());
                userResponse.setCompanyId(user.getCompanyId());
                userResponse.setEmail(user.getEmail());
                userResponse.setFirstName(user.getFirstName());
                userResponse.setLastName(user.getLastName());
                userResponse.setRoleId(user.getRoleId());
                if (userAddress != null) {
                    userResponse.setAddress(userAddress.getAdress());
                    userResponse.setCity(userAddress.getCity());
                    userResponse.setCountry(userAddress.getCountry());
                    userResponse.setPhone(userAddress.getPhone());
                }
                return responseUtil.successResponse(userResponse);
            } else {
                throw new ApplicationException(APIStatus.ERR_USER_NOT_EXIST);
            }
        } else {
            throw new ApplicationException(APIStatus.INVALID_PARAMETER);
        }
    }

    @RequestMapping(path = APINames.USER_LOGOUT, method = RequestMethod.POST)
    public ResponseEntity<APIResponse> logout(
            @RequestHeader(value = Constant.HEADER_TOKEN) String tokenId
    ) {
        if (tokenId != null && !"".equals(tokenId)) {
            UserToken userToken = authService.getUserTokenById(tokenId);
            if (userToken != null) {
                authService.deleteUserToken(userToken);
                return responseUtil.successResponse("OK");
            } else {
                throw new ApplicationException(APIStatus.ERR_SESSION_NOT_FOUND);
            }
        } else {
            throw new ApplicationException(APIStatus.INVALID_PARAMETER);
        }
    }
}
