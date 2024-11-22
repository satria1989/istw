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

        this.registrationList.add(context.registerService(LoadDataDSDetail.class.getName(), new LoadDataDSDetail(), null));
        this.registrationList.add(context.registerService(ImportUserDirToMasterSignature.class.getName(), new ImportUserDirToMasterSignature(), null));
        this.registrationList.add(context.registerService(GetTilakaID.class.getName(), new GetTilakaID(), null));

        this.registrationList.add(context.registerService(SignatureDone.class.getName(), new SignatureDone(), null));
        this.registrationList.add(context.registerService(TilakaDocReject.class.getName(), new TilakaDocReject(), null));
        this.registrationList.add(context.registerService(TilakaCheckSignStatus.class.getName(), new TilakaCheckSignStatus(), null));
        this.registrationList.add(context.registerService(TilakaCheckCertificateStatus.class.getName(), new TilakaCheckCertificateStatus(), null));
        this.registrationList.add(context.registerService(TilakaRegisterForKycCheck.class.getName(), new TilakaRegisterForKycCheck(), null));
        this.registrationList.add(context.registerService(TilakaDownloadFinalDoc.class.getName(), new TilakaDownloadFinalDoc(), null));
        this.registrationList.add(context.registerService(TilakaRegisterForKycCheckWna.class.getName(), new TilakaRegisterForKycCheckWna(), null));
        this.registrationList.add(context.registerService(TilakaRegisterForKycCheckKitas.class.getName(), new TilakaRegisterForKycCheckKitas(), null));        this.registrationList.add(context.registerService(TilakaRegisterForKycCheckKitas.class.getName(), new TilakaRegisterForKycCheckKitas(), null));
        this.registrationList.add(context.registerService(TilakaRegisterForReenroll.class.getName(), new TilakaRegisterForReenroll(), null));
        this.registrationList.add(context.registerService(TilakaRegisterForReenrollWNA.class.getName(), new TilakaRegisterForReenrollWNA(), null));
        this.registrationList.add(context.registerService(TilakaRegisterForReenrollKitas.class.getName(), new TilakaRegisterForReenrollKitas(), null));
        this.registrationList.add(context.registerService(TilakaUserRegStatus.class.getName(), new TilakaUserRegStatus(), null));
        this.registrationList.add(context.registerService(IsTilakaUserReady.class.getName(), new IsTilakaUserReady(), null));
        this.registrationList.add(context.registerService(TilakaRevoke.class.getName(), new TilakaRevoke(), null));
        this.registrationList.add(context.registerService(GetTilakaReenrolID.class.getName(), new GetTilakaReenrolID(), null));


    }

    public void stop(BundleContext context) {
        for (ServiceRegistration registration : registrationList) {
            registration.unregister();
        }
    }
}