package org.wso2.custom.user.bulk.delete.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.wso2.custom.user.bulk.delete.BulkUserUploadThread;
import org.wso2.custom.user.bulk.delete.Constants;
import org.wso2.carbon.user.core.service.RealmService;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.wso2.custom.user.bulk.delete.Constants.BULK_DELETE;
import static org.wso2.custom.user.bulk.delete.Constants.BULK_DELETE_LOG_PREFIX;

@Component(name = "org.wso2.custom.user.bulk.delete.component",
        immediate = true)
public class CustomUserAdministratorServiceComponent {

    private static final Log log = LogFactory.getLog(CustomUserAdministratorServiceComponent.class);

    @Activate
    protected void activate(ComponentContext context) {

        try {
            boolean bulkUpload = Boolean.parseBoolean(System.getProperty(BULK_DELETE));
            if (bulkUpload) {
                log.info(BULK_DELETE_LOG_PREFIX + "Bulk user delete is enabled from the file system.");
                Callable<Boolean> bulkUploadThread = new BulkUserUploadThread();
                ExecutorService executorService = Executors.newFixedThreadPool(Constants.DEFAULT_BULK_USER_DELETE_POOL_SIZE);
                executorService.submit(bulkUploadThread);
            }

            if (log.isDebugEnabled()) {
                log.debug("Custom bulk user delete component is activated.");
            }
        } catch (Throwable e) {
            log.error("Error occurred while activating the custom component.", e);
        }
    }

    @Deactivate
    protected void deactivate(ComponentContext cxt) {

        if (log.isDebugEnabled()) {
            log.debug("Custom component is deactivated.");
        }
    }

    @Reference(name = "realm.service",
            service = org.wso2.carbon.user.core.service.RealmService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetRealmService")
    protected void setRealmService(RealmService realmService) {

        // Custom user administrator bundle depends on the Realm Service
        // Therefore, bind the realm service
        if (log.isDebugEnabled()) {
            log.debug("Setting the Realm Service.");
        }
        CustomUserAdministratorDataHolder.getInstance().setRealmService(realmService);
    }

    protected void unsetRealmService(RealmService realmService) {

        if (log.isDebugEnabled()) {
            log.debug("Unsetting the Realm Service.");
        }
        CustomUserAdministratorDataHolder.getInstance().setRealmService(null);
    }
}
