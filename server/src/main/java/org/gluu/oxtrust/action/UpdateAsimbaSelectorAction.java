/*
 * oxTrust is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxtrust.action;

import java.io.Serializable;
import java.security.Identity;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.enterprise.context.SessionScoped;
import javax.enterprise.inject.Produces;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;
import javax.inject.Inject;
import javax.inject.Named;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.gluu.asimba.util.ldap.idp.IDPEntry;
import org.gluu.asimba.util.ldap.selector.ApplicationSelectorEntry;
import org.gluu.asimba.util.ldap.sp.RequestorEntry;
import org.gluu.jsf2.message.FacesMessages;
import org.gluu.oxtrust.ldap.service.AsimbaService;
import org.gluu.oxtrust.ldap.service.SvnSyncTimer;
import org.gluu.oxtrust.util.OxTrustConstants;
import org.slf4j.Logger;
import org.xdi.config.oxtrust.AppConfiguration;


/**
 * Action class for updating and adding the Requestor->IDP Selector to Asimba
 * 
 * @author Dmitry Ognyannikov
 */
@SessionScoped
@Named("updateAsimbaSelectorAction")
//TODO CDI @Restrict("#{identity.loggedIn}")
public class UpdateAsimbaSelectorAction implements Serializable {

    private static final long serialVersionUID = -1242167044333943680L;
    
    @Inject
    private Logger log;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private Identity identity;

    @Inject
    private SvnSyncTimer svnSyncTimer;
    
    @Inject
    private FacesMessages facesMessages;

    @Inject
    private FacesContext facesContext;
    
    @Inject
    private AsimbaService asimbaService;
    
    @Produces
    private ApplicationSelectorEntry selector;
    
    private boolean newEntry = true;
    
    private String editEntryInum = null;
    
    private List<ApplicationSelectorEntry> selectorList = new ArrayList<ApplicationSelectorEntry>();
    
    private List<SelectItem> idpList;
    
    private List<SelectItem> spRequestorList;
    
    @NotNull
    @Size(min = 0, max = 30, message = "Length of search string should be less than 30")
    private String searchPattern = "";
    
    public UpdateAsimbaSelectorAction() {
        
    }
    
    @PostConstruct
    public void init() {
        log.info("init() Selector call");
        
        clearEdit();
        
        refresh();
    }
    
    public void refresh() {
        log.info("refresh() Selector call");
        
        if (searchPattern == null || "".equals(searchPattern)) {
            //list loading
            selectorList = asimbaService.loadSelectors();
        } else {
            // search mode, clear pattern
            searchPattern = null;
        }
        
        // Load edit lists
        idpList = new ArrayList<SelectItem>();
        List<IDPEntry> idpListEntries = asimbaService.loadIDPs();
        for (IDPEntry entry : idpListEntries) {
            idpList.add(new SelectItem(entry.getId(), entry.getId(), entry.getFriendlyName()));
        }
        
        spRequestorList = new ArrayList<SelectItem>();
        List<RequestorEntry> spRequestorListEntries = asimbaService.loadRequestors();
        for (RequestorEntry entry : spRequestorListEntries) {
            spRequestorList.add(new SelectItem(entry.getId(), entry.getId(), entry.getFriendlyName()));
        }
    }
    
    public void clearEdit() {
        selector = new ApplicationSelectorEntry();
        editEntryInum = null;
        newEntry = true;
    }
    
    //TODO CDI @Restrict("#{s:hasPermission('trust', 'access')}")
    public void edit() {
        log.info("edit() Selector call, inum: "+editEntryInum);
        if (editEntryInum == null || "".equals(editEntryInum)) {
            // no inum, new entry mode
            clearEdit();
        } else {
            // edit entry
            newEntry = false;
            selector = asimbaService.readApplicationSelectorEntry(editEntryInum);
        }
    }
    
    //TODO CDI @Restrict("#{s:hasPermission('trust', 'access')}")
    public String add() {
        log.info("add() Selector call", selector);
        synchronized (svnSyncTimer) {
            asimbaService.addApplicationSelectorEntry(selector);
        }
        clearEdit();
        return OxTrustConstants.RESULT_SUCCESS;
    }
    
    //TODO CDI @Restrict("#{s:hasPermission('trust', 'access')}")
    public String update() {
        log.info("update() Selector", selector);
        synchronized (svnSyncTimer) {
            asimbaService.updateApplicationSelectorEntry(selector);
        }
        clearEdit();
        return OxTrustConstants.RESULT_SUCCESS;
    }
    
    //TODO CDI @Restrict("#{s:hasPermission('trust', 'access')}")
    public String cancel() {
        log.info("cancel() Selector", selector);
        clearEdit();
        return OxTrustConstants.RESULT_SUCCESS;
    }
    
    //TODO CDI @Restrict("#{s:hasPermission('trust', 'access')}")
    public String delete() {
        log.info("delete() Selector", selector);
        synchronized (svnSyncTimer) {
            asimbaService.removeApplicationSelectorEntry(selector);
        }
        clearEdit();
        return OxTrustConstants.RESULT_SUCCESS;
    }
    
    //TODO CDI @Restrict("#{s:hasPermission('person', 'access')}")
    public String search() {
        log.info("search() Selector searchPattern:", searchPattern);
        synchronized (svnSyncTimer) {
            if (searchPattern != null && !"".equals(searchPattern)){
                try {
                    selectorList = asimbaService.searchSelectors(searchPattern, 0);
                } catch (Exception ex) {
                    log.error("LDAP search exception", ex);
                }
            } else {
                //list loading
                selectorList = asimbaService.loadSelectors();
            }
        }
        return OxTrustConstants.RESULT_SUCCESS;
    }

    /**
     * @return the selector
     */
    public ApplicationSelectorEntry getSelector() {
        return selector;
    }

    /**
     * @param selector the selector to set
     */
    public void setSelector(ApplicationSelectorEntry selector) {
        this.selector = selector;
    }

    /**
     * @return the selectorList
     */
    public List<ApplicationSelectorEntry> getSelectorList() {
        return selectorList;
    }

    /**
     * @param selectorList the selectorList to set
     */
    public void setSelectorList(List<ApplicationSelectorEntry> selectorList) {
        this.selectorList = selectorList;
    }

    /**
     * @return the searchPattern
     */
    public String getSearchPattern() {
        return searchPattern;
    }

    /**
     * @param searchPattern the searchPattern to set
     */
    public void setSearchPattern(String searchPattern) {
        this.searchPattern = searchPattern;
    }

    /**
     * @return the newEntry
     */
    public boolean isNewEntry() {
        return newEntry;
    }

    /**
     * @param newEntry the newEntry to set
     */
    public void setNewEntry(boolean newEntry) {
        this.newEntry = newEntry;
    }

    /**
     * @return the editEntryInum
     */
    public String getEditEntryInum() {
        return editEntryInum;
    }

    /**
     * @param editEntryInum the editEntryInum to set
     */
    public void setEditEntryInum(String editEntryInum) {
        this.editEntryInum = editEntryInum;
    }

    /**
     * @return the idpList
     */
    public List<SelectItem> getIdpList() {
        return idpList;
    }

    /**
     * @param idpList the idpList to set
     */
    public void setIdpList(List<SelectItem> idpList) {
        this.idpList = idpList;
    }

    /**
     * @return the spRequestorList
     */
    public List<SelectItem> getSpRequestorList() {
        return spRequestorList;
    }

    /**
     * @param spRequestorList the spRequestorList to set
     */
    public void setSpRequestorList(List<SelectItem> spRequestorList) {
        this.spRequestorList = spRequestorList;
    }
}
