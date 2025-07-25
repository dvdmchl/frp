package org.dreamabout.sw.frp.be.domain;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ApiPath {

    public static final String API_ROOT = "/api";

    public static final String USER = "/user";

    public static final String USER_REGISTER = "/register";

    public static final String USER_REGISTER_FULL = API_ROOT + USER + USER_REGISTER;

    public static final String USER_LOGIN = "/login";

    public static final String USER_LOGIN_FULL = API_ROOT + USER + USER_LOGIN;

    public static final String USER_ME = "/me";

    public static final String USER_ME_FULL = API_ROOT + USER + USER_ME;

    public static final String USER_UPDATE_INFO = "/me/info";

    public static final String USER_UPDATE_INFO_FULL = API_ROOT + USER + USER_UPDATE_INFO;

    public static final String USER_UPDATE_PASSWORD = "/me/password";

    public static final String USER_UPDATE_PASSWORD_FULL = API_ROOT + USER + USER_UPDATE_PASSWORD;

    public static final String USER_LOGOUT = "/logout";
}
