package id.co.itasoft.esign;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import java.util.ArrayList;
import java.util.Collection;

public class Activator implements BundleActivator {

    protected Collection<ServiceRegistration> registrationList;

    public void start(BundleContext context) {
        registrationList = new ArrayList<ServiceRegistration>();

        this.registrationList.add(context.registerService(PDFFileDownloadCustom.class.getName(), new PDFFileDownloadCustom(), null));
        this.registrationList.add(context.registerService(AbortDocument.class.getName(), new AbortDocument(), null));
        
        //Register plugin here
        //registrationList.add(context.registerService(MyPlugin.class.getName(), new MyPlugin(), null));
    }

    public void stop(BundleContext context) {
        for (ServiceRegistration registration : registrationList) {
            registration.unregister();
        }
    }
}