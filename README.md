# bulk-user-delete-wso2-identity-server

A custom **sample** jar application developed to delete users in bulk (Makes use of .csv files).

## Prepare

### WSO2 Identity Server

- Add the .csv file (A sample content overview can be seen below)
  which contains the list of usernames to the `<IS_HOME>/repository/resources/identity/users`
  directory (create the users' folder if it does not exist).

```
UserName
user1
user2
user3
```

- Add a file named `config.properties` to the same directory mentioned in the previous point with the custom
  configurations. A sample set of configurations is provided below.

```
# MANDATORY FIELDS
# The tenant domain of the users. Currently the sample jar only supports super tenant.
tenantDomain=carbon.super
# Whether the users are stored in a secondary user store.
isSecondary=false
# The name of the username field in the .csv file.
usernameField=UserName
```

### Clone and Build

Clone and build the project by executing the following commands sequentially:

```
git clone https://github.com/deshankoswatte/wso2-is-bulk-user-delete.git
mvn clean install
```

### Deploy

After successfully building the project, copy the artifact `org.wso2.carbon.custom.user.bulk.delete-1.0.jar` from the
target folder and paste it inside the `<IS HOME>/repository/components/dropins` folder.

## Run

Start your WSO2 Identity Server by executing the command `sh wso2server.sh -DbulkDelete=true` from your `<IS HOME>/bin`
folder.

## Important points for consideration

- Please test this in a lower environment with a set of sample users.
- With the client, there will be an improvement, but it will not improve the performance drastically.
- Make sure that the process is started in a separate node by pointing to the original database so that it would not
  affect normal usage.
- The database resource usage will be high at that time, so it should be managed at your end.

## Test

### Scenario Reproduction Steps

1. Start the WSO2 Identity Server.
2. Bulk import a reasonable amount of users.
3. Stop the server.
4. Restart the server with `-DbulkDelete=true`.
5. The users will be deleted based on the .csv file provided.
6. Go to the management console, and you will see that the users mentioned in the .csv file is deleted.

### Tested Environment Details

```
Operating System - Ubuntu 20.04
Java Version - 1.8
Identity Server Versions - IS-5.7.0
Database Type - JDBC
```
