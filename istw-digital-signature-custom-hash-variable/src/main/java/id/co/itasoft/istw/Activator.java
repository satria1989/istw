package id.co.itasoft.istw;

import java.util.ArrayList;
import java.util.Collection;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class Activator implements BundleActivator {

    protected Collection<ServiceRegistration> registrationList;

    public void start(BundleContext context) {
        registrationList = new ArrayList<ServiceRegistration>();

        //Register plugin here
        registrationList.add(context.registerService(PendingDocument.class.getName(), new PendingDocument(), null));
        registrationList.add(context.registerService(FilterRequest.class.getName(), new FilterRequest(), null));

        registrationList.add(context.registerService(HashCustomUserviewPermission.class.getName(), new HashCustomUserviewPermission(), null));
        registrationList.add(context.registerService(HashCustomUserviewPermissionCSS.class.getName(), new HashCustomUserviewPermissionCSS(), null));
        registrationList.add(context.registerService(EmailReminderRecipients.class.getName(), new EmailReminderRecipients(), null));
        registrationList.add(context.registerService(FormatEmailContent.class.getName(), new FormatEmailContent(), null));
        registrationList.add(context.registerService(CheckDefaultConstData.class.getName(), new CheckDefaultConstData(), null));
        registrationList.add(context.registerService(CheckTilakaEnvData.class.getName(), new CheckTilakaEnvData(), null));
        registrationList.add(context.registerService(CheckTilakaActionsData.class.getName(), new CheckTilakaActionsData(), null));
        registrationList.add(context.registerService(GetUserDetailInfo.class.getName(), new GetUserDetailInfo(), null));
        registrationList.add(context.registerService(DigiSignData.class.getName(), new DigiSignData(), null));
        
        registrationList.add(context.registerService(HashCheckIsValidApprovalURL.class.getName(), new HashCheckIsValidApprovalURL(), null));
        registrationList.add(context.registerService(GetTilakaUser.class.getName(), new GetTilakaUser(), null));
        registrationList.add(context.registerService(GetTilakaDocAuthUrl.class.getName(), new GetTilakaDocAuthUrl(), null));
        



    }

    public void stop(BundleContext context) {
        for (ServiceRegistration registration : registrationList) {
            registration.unregister();
        }
    }
}