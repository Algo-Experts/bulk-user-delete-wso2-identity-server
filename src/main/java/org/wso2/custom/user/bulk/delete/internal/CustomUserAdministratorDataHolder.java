package org.wso2.custom.user.bulk.delete.internal;

import org.wso2.carbon.user.core.service.RealmService;

public class CustomUserAdministratorDataHolder {

    private static CustomUserAdministratorDataHolder dataHolder = new CustomUserAdministratorDataHolder();
    private RealmService realmService;

    public static CustomUserAdministratorDataHolder getInstance() {

        return dataHolder;
    }

    public void setDataHolder(CustomUserAdministratorDataHolder dataHolder) {

        this.dataHolder = dataHolder;
    }

    public RealmService getRealmService() {

        return realmService;
    }

    public void setRealmService(RealmService realmService) {

        this.realmService = realmService;
    }

}
