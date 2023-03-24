package com.skyway;

import com.dassault_systemes.platform.restServices.ModelerBase;

import javax.ws.rs.ApplicationPath;
/**
 * Контроллер веб приложения
 * */
@ApplicationPath("/sw")
public class Controller extends ModelerBase {
    @Override
    public Class<?>[] getServices() {
        return new Class[]{
                Utils.class,
                Console.class,
                SpecPdf.class,
                Requirements.class,
                SpecDocs.class,
                SpecComp.class,
                SpecCompRSW.class,
                Activities.class,
                Groups.class,
                Orders.class,
                Norm.class,
                Approver.class,
                ApproverPdo.class,
                ApproverCat.class,
                ApproverBoss.class,
                ApproverNorm.class,
        };
    }
}