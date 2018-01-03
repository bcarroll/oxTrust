/*
 * SCIM-Client is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2013, Gluu
 */
package org.gluu.oxtrust.ws.rs.scim2;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.Version;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.module.SimpleModule;
import org.gluu.oxtrust.model.exception.SCIMException;
import org.gluu.oxtrust.model.scim2.*;
import org.gluu.oxtrust.model.scim2.extensions.Extension;
import org.gluu.oxtrust.model.scim2.patch.PatchOperation;
import org.gluu.oxtrust.model.scim2.patch.PatchOperationType;
import org.gluu.oxtrust.model.scim2.patch.PatchRequest;
import org.gluu.oxtrust.model.scim2.util.IntrospectUtil;
import org.gluu.oxtrust.model.scim2.util.ResourceValidator;
import org.gluu.oxtrust.model.scim2.util.ScimResourceUtil;
import org.gluu.oxtrust.service.external.ExternalScimService;
import org.gluu.oxtrust.service.scim2.ExtensionService;
import org.gluu.oxtrust.service.scim2.serialization.ListResponseJsonSerializer;
import org.gluu.oxtrust.service.scim2.serialization.ScimResourceSerializer;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.xdi.config.oxtrust.AppConfiguration;
import org.xdi.ldap.model.SortOrder;

import javax.inject.Inject;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;

import static javax.ws.rs.core.Response.Status.*;
import static org.gluu.oxtrust.model.scim2.Constants.*;

/**
 * Base methods for SCIM web services
 *
 * @author Yuriy Movchan Date: 08/23/2013
 * Re-engineered by jgomer on 2017-09-14.
 */
public class BaseScimWebService {

    @Inject
    Logger log;

    @Inject
    AppConfiguration appConfiguration;

    @Inject
    ScimResourceSerializer resourceSerializer;

    @Inject
    ExtensionService extService;

    @Inject
    ExternalScimService externalScimService;

    public static final String SEARCH_SUFFIX = ".search";

    String endpointUrl;

    public String getEndpointUrl() {
        return endpointUrl;
    }

    public static Response getErrorResponse(Response.Status status, String detail) {
        return getErrorResponse(status.getStatusCode(), null, detail);
    }

    public static Response getErrorResponse(Response.Status status, ErrorScimType scimType, String detail) {
        return getErrorResponse(status.getStatusCode(), scimType, detail);
    }
/*
    public Response getErrorResponse(int statusCode, String detail) {
        return getErrorResponse(statusCode, null, detail);
    }
*/
    public static Response getErrorResponse(int statusCode, ErrorScimType scimType, String detail) {

        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setStatus(String.valueOf(statusCode));
        errorResponse.setScimType(scimType);
        errorResponse.setDetail(detail);

        return Response.status(statusCode).entity(errorResponse).build();
    }

    int getMaxCount(){
        return appConfiguration.getScimProperties().getMaxCount();
    }

    String getValueFromHeaders(HttpHeaders headers, String name){
        List<String> values=headers.getRequestHeaders().get(name);
        return (values==null || values.size()==0) ? null : values.get(0);
    }

    protected boolean isAttributeRecognized(Class<? extends BaseScimResource> cls, String attribute){

        boolean valid;

        Extension ext=extService.extensionOfAttribute(cls, attribute);
        valid=ext!=null;

        if (!valid) {
            attribute = ScimResourceUtil.stripDefaultSchema(cls, attribute);
            Field f= IntrospectUtil.findFieldFromPath(cls, attribute);
            valid= f!=null;
        }
        return valid;

    }

    protected void assignMetaInformation(BaseScimResource resource){

        //Generate some meta information (this replaces the info client passed in the request)
        long now=new Date().getTime();
        String val= ISODateTimeFormat.dateTime().withZoneUTC().print(now);

        Meta meta=new Meta();
        meta.setResourceType(ScimResourceUtil.getType(resource.getClass()));
        meta.setCreated(val);
        meta.setLastModified(val);
        //For version attritute: Service provider support for this attribute is optional and subject to the service provider's support for versioning
        //For location attribute: this will be set after current user creation in LDAP
        resource.setMeta(meta);

    }

    protected void executeDefaultValidation(BaseScimResource resource) throws SCIMException {
        executeValidation(resource, false);
    }

    protected void executeValidation(BaseScimResource resource, boolean skipRequired) throws SCIMException {

        ResourceValidator rv=new ResourceValidator(resource, extService.getResourceExtensions(resource.getClass()));
        if (!skipRequired){
            rv.validateRequiredAttributes();
            rv.validateSchemasAttribute();
        }
        rv.validateValidableAttributes();
        //By section 7 of RFC 7643, we are not forced to constrain attribute values when they have a list of canonical values associated
        //rv.validateCanonicalizedAttributes();
        rv.validateExtendedAttributes();

    }

    protected Response prepareSearchRequest(List<String> schemas, String filter, String sortBy, String sortOrder, Integer startIndex,
                                         Integer count, String attrsList, String excludedAttrsList, SearchRequest request, String sortByDefault){

        Response response=null;

        if (schemas!=null && schemas.size()==1 && schemas.get(0).equals(SEARCH_REQUEST_SCHEMA_ID)) {
            count = count == null ? getMaxCount() : count;
            //Per spec, a negative value SHALL be interpreted as "0" for count
            if (count<0)
                count=0;

            if (count <= getMaxCount()) {
                startIndex = (startIndex == null || startIndex < 1) ? 1 : startIndex;

                if (StringUtils.isEmpty(sortOrder) || !sortOrder.equals(SortOrder.DESCENDING.getValue()))
                    sortOrder = SortOrder.ASCENDING.getValue();

                request.setSchemas(schemas);
                request.setAttributes(attrsList);
                request.setExcludedAttributes(excludedAttrsList);
                request.setFilter(filter);
                request.setSortBy(StringUtils.isEmpty(sortBy) ? sortByDefault : sortBy);
                request.setSortOrder(sortOrder);
                request.setStartIndex(startIndex);
                request.setCount(count);
            }
            else
                response = getErrorResponse(BAD_REQUEST, ErrorScimType.TOO_MANY, "Maximum number of results per page is " + getMaxCount());
        }
        else
            response = getErrorResponse(BAD_REQUEST, ErrorScimType.INVALID_SYNTAX, "Wrong schema(s) supplied in Search Request");

        return response;

    }

    String getListResponseSerialized(int total, int startIndex, List<BaseScimResource> resources, String attrsList,
                                     String excludedAttrsList, boolean ignoreResults) throws IOException{

        ListResponse listResponse = new ListResponse(startIndex, resources.size(), total);
        listResponse.setResources(resources);

        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule("ListResponseModule", Version.unknownVersion());
        module.addSerializer(ListResponse.class, new ListResponseJsonSerializer(resourceSerializer, attrsList, excludedAttrsList, ignoreResults));
        mapper.registerModule(module);

        return mapper.writeValueAsString(listResponse);

    }

    protected Response inspectPatchRequest(PatchRequest patch, Class<? extends BaseScimResource> cls){

        Response response=null;
        List<String> schemas=patch.getSchemas();

        if (schemas!=null && schemas.size()==1 && schemas.get(0).equals(PATCH_REQUEST_SCHEMA_ID)) {
            List<PatchOperation> ops = patch.getOperations();

            if (ops != null) {
                //Adjust paths if they came prefixed

                String defSchema=ScimResourceUtil.getDefaultSchemaUrn(cls);
                List<String> urns=extService.getUrnsOfExtensions(cls);
                urns.add(defSchema);

                for (PatchOperation op : ops){
                    if (op.getPath()!=null)
                        op.setPath(ScimResourceUtil.adjustNotationInPath(op.getPath(), defSchema, urns));
                }

                for (PatchOperation op : ops) {

                    if (op.getType() == null)
                        response = getErrorResponse(BAD_REQUEST, ErrorScimType.INVALID_SYNTAX, "Operation '" + op.getOperation() + "' not recognized");
                    else {
                        String path = op.getPath();

                        if (StringUtils.isEmpty(path) && op.getType().equals(PatchOperationType.REMOVE))
                            response = getErrorResponse(BAD_REQUEST, ErrorScimType.NO_TARGET, "Path attribute is required for remove operation");
                        else
                        if (op.getValue() == null && !op.getType().equals(PatchOperationType.REMOVE))
                            response = getErrorResponse(BAD_REQUEST, ErrorScimType.INVALID_SYNTAX, "Value attribute is required for operations other than remove");
                    }
                    if (response != null)
                        break;
                }
            }
            else
                response = getErrorResponse(BAD_REQUEST, ErrorScimType.INVALID_SYNTAX, "Patch request MUST contain the attribute 'Operations'");
        }
        else
            response = getErrorResponse(BAD_REQUEST, ErrorScimType.INVALID_SYNTAX, "Wrong schema(s) supplied in Search Request");

        log.info("inspectPatchRequest. Preprocessing of patch request {}", response==null ? "passed" : "failed");
        return response;

    }

}
