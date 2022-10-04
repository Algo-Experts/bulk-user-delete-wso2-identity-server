package org.wso2.custom.user.bulk.delete;

import au.com.bytecode.opencsv.CSVReader;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.jdbc.JDBCUserStoreManager;
import org.wso2.custom.user.bulk.delete.internal.CustomUserAdministratorDataHolder;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Callable;

public class BulkUserUploadThread implements Callable<Boolean> {

    private static final Log log = LogFactory.getLog(BulkUserUploadThread.class);

    private File[] files = null;
    private final ArrayList<String[]> firstLine = new ArrayList<>();
    private final Map<Integer, Map<String, Integer>> fileIndexToSpecialColumns = new LinkedHashMap<>();
    private final LinkedHashMap<Integer, LinkedHashSet<String[]>> fileIndexToUserHashMap = new LinkedHashMap<>();

    private String tenantDomain = null;
    private int tenantId;
    private String usernameField = null;
    private String secondaryUserDomain = null;
    private long timeToWaitForSecondary = Constants.DEFAULT_TIME_TO_WAIT_FOR;

    private JDBCUserStoreManager store;

    public BulkUserUploadThread() {
        super();
    }

    @Override
    public Boolean call() {

        if (!this.doCheckPrerequisites())
            return false;

        long t4 = System.currentTimeMillis();

        if (this.store != null) {

            InputStream targetStream = null;
            BufferedReader reader = null;
            CSVReader csvReader = null;

            try {
                log.info(Constants.BULK_DELETE_LOG_PREFIX + "Starting reading from files and checking columns");

                int usernameIndex = 0;
                boolean usernameFieldFound;
                String field;
                Map<String, Integer> specialColumnsToIndex;
                for (int q = 0; q < files.length; q++) {
                    log.info(Constants.BULK_DELETE_LOG_PREFIX + "Started column check for the CSV file: " +
                            files[q].getAbsolutePath() + ", file order: " + q);
                    log.info(Constants.BULK_DELETE_LOG_PREFIX + "Started reading from file " + files[q].getAbsolutePath() + ", file order: " + q);

                    targetStream = new FileInputStream(files[q]);
                    reader = new BufferedReader(new InputStreamReader(targetStream, StandardCharsets.UTF_8));
                    csvReader = new CSVReader(reader, ',', '"', 0);

                    String[] line = csvReader.readNext();
                    this.firstLine.add(line);

                    usernameFieldFound = false;

                    for (int j = 0; j < line.length; j++) {
                        field = line[j];
                        if (field.equals(this.usernameField)) {
                            if (usernameFieldFound) {
                                log.error(Constants.BULK_DELETE_LOG_PREFIX + "Field '" + Constants.USERNAME_FIELD + "': " + this.usernameField +
                                        "' is duplicated in the CSV. Task Aborted.");
                                return false;
                            }
                            usernameIndex = j;
                            usernameFieldFound = true;
                        }

                    }

                    if (!usernameFieldFound) {
                        String constant1 = Constants.USERNAME_FIELD;
                        String constant2 = this.usernameField;

                        log.error(Constants.BULK_DELETE_LOG_PREFIX + "Mandatory field '" + constant1 + "': " + constant2 +
                                "' not found on CSV. Task Aborted.");
                        return false;
                    }
                    specialColumnsToIndex = new HashMap<>();
                    specialColumnsToIndex.put(this.usernameField, usernameIndex);

                    this.fileIndexToSpecialColumns.put(q, specialColumnsToIndex);
                    LinkedHashSet<String[]> userSet = new LinkedHashSet<>();
                    while (line != null && line.length > 0) {
                        line = csvReader.readNext();
                        userSet.add(line);
                    }
                    this.fileIndexToUserHashMap.put(q, userSet);
                }
            } catch (IOException e) {
                log.error(Constants.BULK_DELETE_LOG_PREFIX + "Error occurred while reading from CSV files", e);
                return false;
            } finally {
                if (csvReader != null) {
                    try {
                        csvReader.close();
                    } catch (IOException e) {
                        log.error(Constants.BULK_DELETE_LOG_PREFIX + "Error occurred while closing the CSVReader", e);
                    }
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        log.error(Constants.BULK_DELETE_LOG_PREFIX + "Error occurred while closing the BufferedReader", e);
                    }
                }
                if (targetStream != null) {
                    try {
                        targetStream.close();
                    } catch (IOException e) {
                        log.error(Constants.BULK_DELETE_LOG_PREFIX + "Error occurred while closing the FileInputStream", e);
                    }
                }
            }

            long t5 = System.currentTimeMillis();
            log.info(Constants.BULK_DELETE_LOG_PREFIX + "[TIME INDICATOR] Total time taken to read from CSV files and check columns" +
                    "(in milliseconds) : " + (t5 - t4));
            log.info(Constants.BULK_DELETE_LOG_PREFIX + "Starting user provisioning to the given user store...");

            PrivilegedCarbonContext.startTenantFlow();
            PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(this.tenantDomain);
            PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantId(this.tenantId);
            LinkedHashSet<String[]> userSet1;
            Map<String, Integer> specialColumnsToIndex;
            int fileIndex;
            int usernameColumnIndex;
            String[] fileFirstLine;
            for (Map.Entry<Integer, LinkedHashSet<String[]>> entry : this.fileIndexToUserHashMap.entrySet()) {
                fileIndex = entry.getKey();
                userSet1 = entry.getValue();
                specialColumnsToIndex = this.fileIndexToSpecialColumns.get(fileIndex);
                usernameColumnIndex = specialColumnsToIndex.get(this.usernameField);

                fileFirstLine = this.firstLine.get(fileIndex);
                for (String[] user : userSet1) {
                    if (user != null && user[usernameColumnIndex] != null && !user[usernameColumnIndex].isEmpty()) {
                        Map<String, String> claims = new HashMap<>();
                        for (int i = 0; i < fileFirstLine.length; i++) {
                            if (i != usernameColumnIndex) {
                                claims.put(fileFirstLine[i], user[i]);
                            }
                        }

                        try {
                            store.doDeleteUser(user[usernameColumnIndex]);
                        } catch (UserStoreException e) {
                            log.error(Constants.BULK_DELETE_LOG_PREFIX + "Error occurred while deleting user with the username : " + user[0] + " | claims : " + claims, e);
                        }
                    }
                }
            }
            PrivilegedCarbonContext.endTenantFlow();

            long t6 = System.currentTimeMillis();
            log.info(Constants.BULK_DELETE_LOG_PREFIX + "[TIME INDICATOR] Total time taken to delete users from the user store " +
                    "(in milliseconds) : " + (t6 - t5));
        }
        return true;
    }

    private boolean doCheckPrerequisites() {

        long t1 = System.currentTimeMillis();
        log.info(Constants.BULK_DELETE_LOG_PREFIX + "Started prerequisites check.");
        boolean check = false;

        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(Constants.CONFIG_FILE_PATH);
        Properties properties;
        if (inputStream != null) {
            properties = new Properties();
        } else {
            log.error(Constants.BULK_DELETE_LOG_PREFIX + "Prerequisites were not satisfied. Property file '" + Constants.CONFIG_FILE_PATH + "' not found. Task Aborted.");
            return false;
        }

        try {
            properties.load(inputStream);
        } catch (IOException ioException) {
            log.error(Constants.BULK_DELETE_LOG_PREFIX + "Error while loading input stream. Task Aborted.");
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                log.error(Constants.BULK_DELETE_LOG_PREFIX + "Error occurred while closing the inputStream", e);
            }
        }

        String tenantDomain = properties.getProperty(Constants.TENANT_DOMAIN);
        String isSecondary = properties.getProperty(Constants.IS_SECONDARY);
        String usernameField = properties.getProperty(Constants.USERNAME_FIELD);
        if (StringUtils.isBlank(tenantDomain) || StringUtils.isBlank(isSecondary)
                || StringUtils.isBlank(usernameField)) {
            String constant = null;
            if (StringUtils.isBlank(tenantDomain))
                constant = Constants.TENANT_DOMAIN;
            else if (StringUtils.isBlank(isSecondary))
                constant = Constants.IS_SECONDARY;
            else if (StringUtils.isBlank(usernameField))
                constant = Constants.USERNAME_FIELD;

            log.error(Constants.BULK_DELETE_LOG_PREFIX + "Prerequisites were not satisfied. Mandatory key '" + constant +
                    "' not found on '" + Constants.CONFIG_FILE_NAME + "'. Task Aborted.");
            return false;
        }

        if (!isSecondary.equals("true") && !isSecondary.equals("false")) {
            log.error(Constants.BULK_DELETE_LOG_PREFIX + "Prerequisites were not satisfied. Key '" + Constants.IS_SECONDARY +
                    "' can take only 'true' or 'false' values. Task Aborted.");
            return false;
        }

        this.tenantDomain = tenantDomain;
        boolean isSecondary1 = isSecondary.equals("true");
        this.usernameField = usernameField;

        if (isSecondary1) {
            String secondaryUserDomain = properties.getProperty(Constants.SECONDARY_USER_DOMAIN);
            String timeToWaitFor = properties.getProperty(Constants.TIME_TO_WAIT_FOR);
            if (StringUtils.isBlank(secondaryUserDomain)) {
                log.error(Constants.BULK_DELETE_LOG_PREFIX + "Prerequisites were not satisfied. Key '" + Constants.SECONDARY_USER_DOMAIN
                        + "' must be defined if " + Constants.IS_SECONDARY + " is set to true. Task Aborted.");
                return false;
            }
            if (StringUtils.isBlank(timeToWaitFor)) {
                log.warn(Constants.BULK_DELETE_LOG_PREFIX + Constants.TIME_TO_WAIT_FOR + " is not defined in the " + Constants.CONFIG_FILE_PATH +
                        ". Hence, using the default time for the secondary user store: " + Constants.SECONDARY_USER_DOMAIN +
                        "(in milliseconds): " + Constants.DEFAULT_TIME_TO_WAIT_FOR);
            } else {
                this.timeToWaitForSecondary = Integer.parseInt(timeToWaitFor);
            }
            this.secondaryUserDomain = secondaryUserDomain;
        }

        int tenantId = this.getTenantIdFromDomain(this.tenantDomain);
        if (tenantId == -2) return false;
        this.tenantId = tenantId;

        File dir = new File(Constants.FOLDER_PATH);
        File[] files = dir.listFiles((dir1, name) -> name.toLowerCase().endsWith(".csv"));
        if (files == null || files.length == 0) {
            log.error(Constants.BULK_DELETE_LOG_PREFIX + "Prerequisites were not satisfied. No CSV file is found at " + dir.getAbsolutePath());
            return false;
        }
        log.info(Constants.BULK_DELETE_LOG_PREFIX + "At least one CSV file is found in " + dir.getAbsolutePath());
        this.files = files;

        JDBCUserStoreManager store;
        long t2 = System.currentTimeMillis();
        try {
            if (!isSecondary1) {
                store = (JDBCUserStoreManager) CustomUserAdministratorDataHolder.
                        getInstance().getRealmService().getBootstrapRealm().getUserStoreManager();
                if (store == null) {
                    log.error(Constants.BULK_DELETE_LOG_PREFIX + "Prerequisites were not satisfied. Primary user store was not found. Task aborted.");
                    return false;
                }
                this.store = store;
                check = true;
            } else {
                log.info(Constants.BULK_DELETE_LOG_PREFIX + "Waiting until secondary user store is found...");
                boolean timeReached = false;
                do {
                    store = (JDBCUserStoreManager) CustomUserAdministratorDataHolder.
                            getInstance().getRealmService().getBootstrapRealm().getUserStoreManager().getSecondaryUserStoreManager(this.secondaryUserDomain);

                    if (System.currentTimeMillis() > t2 + this.timeToWaitForSecondary) {
                        log.error(Constants.BULK_DELETE_LOG_PREFIX + "Prerequisites were not satisfied. Secondary user store was not found [Reason could be that the given user store domain is wrong. Check whether it is matching]. Allocated time exceeded. Task aborted.");
                        timeReached = true;
                        break;
                    }
                } while (store == null);

                if (!timeReached) {
                    this.store = store;
                    check = true;
                }
            }
            if (check) {
                String val;
                if (isSecondary1)
                    val = this.secondaryUserDomain;
                else
                    val = "Primary";

                log.info(Constants.BULK_DELETE_LOG_PREFIX + "Prerequisites were satisfied. '" + val + "' user store found Continuing on...");
            }
        } catch (UserStoreException e) {
            log.error(Constants.BULK_DELETE_LOG_PREFIX + "Error while obtaining user store manager", e);
        }
        long t3 = System.currentTimeMillis();
        log.info(Constants.BULK_DELETE_LOG_PREFIX + "[TIME INDICATOR] Total time taken for the prerequisites check " +
                "(in milliseconds) : " + (t3 - t1));
        return check;
    }

    private int getTenantIdFromDomain(String tenantDomain) {

        try {
            return -1234;
        } catch (Throwable e) {
            log.error(Constants.BULK_DELETE_LOG_PREFIX + "Prerequisites were not satisfied. Error occurred while resolving tenant Id from tenant domain :" + tenantDomain, e);
            return -2;
        }
    }

}
