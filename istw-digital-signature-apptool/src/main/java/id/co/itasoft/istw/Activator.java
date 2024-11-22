package id.co.itasoft.istw;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import java.util.ArrayList;
import java.util.Collection;

public class Activator implements BundleActivator {

    protected Collection<ServiceRegistration> registrationList;

    public void start(BundleContext context) {
        registrationList = new ArrayList<ServiceRegistration>();

        this.registrationList.add(context.registerService(UploadFinalPDFFileToMinio.class.getName(), new UploadFinalPDFFileToMinio(), null));
        this.registrationList.add(context.registerService(UploadMasterSignatureImageToMinio.class.getName(), new UploadMasterSignatureImageToMinio(), null));
        this.registrationList.add(context.registerService(RequestSignDocumentOnTilaka.class.getName(), new RequestSignDocumentOnTilaka(), null));  
        this.registrationList.add(context.registerService(UploadPDFSourceFileToTilaka.class.getName(), new UploadPDFSourceFileToTilaka(), null));
        
    }

    public void stop(BundleContext context) {
        for (ServiceRegistration registration : registrationList) {
            registration.unregister();
        }
    }
}