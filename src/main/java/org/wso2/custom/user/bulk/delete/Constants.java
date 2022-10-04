package org.wso2.custom.user.bulk.delete;

public class Constants {
    public static final String BULK_DELETE = "bulkDelete";

    public static final int DEFAULT_BULK_USER_DELETE_POOL_SIZE = 4;
    public static final String BULK_DELETE_LOG_PREFIX = "[CUSTOM BULK USER DELETER] =========================> ";
    public static final String FOLDER_PATH = "./repository/resources/identity/users/";
    public static final String CONFIG_FILE_NAME = "config.properties";
    public static final String CONFIG_FILE_PATH = FOLDER_PATH + CONFIG_FILE_NAME;
    public static final int DEFAULT_TIME_TO_WAIT_FOR = 30000;

    // MANDATORY
    public static final String TENANT_DOMAIN = "tenantDomain";
    public static final String IS_SECONDARY = "isSecondary";
    public static final String USERNAME_FIELD = "usernameField";

    // OPTIONAL OR MANDATORY BASED ON isSecondary
    public static final String SECONDARY_USER_DOMAIN = "secondaryUserDomain";
    public static final String TIME_TO_WAIT_FOR = "timeToWaitFor";
}
