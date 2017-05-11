/*
 * oxTrust is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxtrust.action;

import java.io.Serializable;

import javax.annotation.PostConstruct;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.gluu.asimba.util.ldap.idp.IDPEntry;
import org.gluu.oxtrust.ldap.service.AsimbaService;
import org.gluu.oxtrust.ldap.service.SvnSyncTimer;
import org.gluu.oxtrust.util.OxTrustConstants;
import org.slf4j.Logger;

/**
 * Action class for adding the SAML IDP to Asimba.
 * 
 * @author Dmitry Ognyannikov
 */
@SessionScoped
@Named
//TODO CDI @Restrict("#{identity.loggedIn}")
public class AsimbaAddIDPAction implements Serializable {

    private static final long serialVersionUID = -1024167091985943689L;
    
    @Inject
    private Logger log;

    @Inject
    private SvnSyncTimer svnSyncTimer;
    
    @Inject
    private AsimbaService asimbaService;
    
    private IDPEntry idp;
    
    private String idpURL;
    
    @PostConstruct
    public void init() {        
        log.info("init() IDP call");
    }
    
    public void refresh() {
        log.info("refresh() IDP call");
        
        idp = new IDPEntry();
    }
    
    //TODO CDI @Restrict("#{s:hasPermission('trust', 'access')}")
    public String add() {
        log.info("add new IDP", idp);
        // save
        synchronized (svnSyncTimer) {
            //TODO
        }
        refresh();
        return OxTrustConstants.RESULT_SUCCESS;
    }
    
    //TODO CDI @Restrict("#{s:hasPermission('trust', 'access')}")
    public String cancel() {
        log.info("cancel IDP", idp);
        
        return OxTrustConstants.RESULT_SUCCESS;
    }

    /**
     * @return the idp
     */
    public IDPEntry getIdp() {
        return idp;
    }

    /**
     * @param idp the idp to set
     */
    public void setIdp(IDPEntry idp) {
        this.idp = idp;
    }

    /**
     * @return the idpURL
     */
    public String getIdpURL() {
        return idpURL;
    }

    /**
     * @param idpURL the idpURL to set
     */
    public void setIdpURL(String idpURL) {
        this.idpURL = idpURL;
    }
    
}
