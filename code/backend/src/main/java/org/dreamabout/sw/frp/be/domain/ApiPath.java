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

    public static final String ADMIN = "/admin";
    public static final String ACTIVE = "/active";
    
    public static final String ADMIN_USERS = ADMIN + "/users";
    public static final String ADMIN_USERS_FULL = API_ROOT + ADMIN_USERS;

    public static final String MAINTENANCE = ADMIN + "/maintenance";
    public static final String MAINTENANCE_FULL = API_ROOT + MAINTENANCE;
    public static final String ORPHAN_SCHEMAS = "/orphan-schemas";

    public static final String ID_PARAM = "/{id}";
    public static final String ID_ACTIVE = ID_PARAM + ACTIVE;
    public static final String ID_ADMIN = ID_PARAM + ADMIN;
    public static final String ID_GROUPS = ID_PARAM + "/groups";

    public static final String CONTEXT = "/context";
    public static final String CONTEXT_FULL = API_ROOT + CONTEXT;

    public static final String SCHEMA = "/schema";
    public static final String SCHEMA_FULL = API_ROOT + SCHEMA;

    public static final String MODULES = "/modules";
    public static final String MODULES_FULL = API_ROOT + MODULES;

    public static final String COPY = "/copy";
    public static final String NAME_PARAM = "/{name}";
    public static final String CODE_PARAM = "/{code}";

    public static final String ACCOUNTING = "/accounting";
    public static final String ACCOUNTS = "/accounts";
    public static final String CURRENCIES = "/currencies";
    public static final String TRANSACTIONS = "/transactions";
    public static final String JOURNALS = "/journals";
    public static final String ACCOUNTS_TREE = "/tree";
    public static final String MOVE = "/move";
}