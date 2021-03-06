/*
 * oxTrust is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2018, Gluu
 */
package org.gluu.oxtrust.api.test.client;


import java.util.logging.Logger;
import org.glassfish.jersey.filter.LoggingFilter;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * REST webservice CRUD template.
 * 
 * @author Dmitry Ognyannikov
 */
public class AbstractClient<T> {
    protected WebTarget webTarget;
    protected Client client;
    protected Class<T> entityClass;
    
    public AbstractClient(Class<T> entityClass, String baseURI, String path) {
        this.entityClass = entityClass;
        client = javax.ws.rs.client.ClientBuilder.newClient();
        client.register(new LoggingFilter(Logger.global, true));
        webTarget = client.target(baseURI).path(path);
    }

    public T read(String id) throws ClientErrorException {
        WebTarget resource = webTarget.path(java.text.MessageFormat.format("read/{0}", new Object[]{id}));
        return resource.request(javax.ws.rs.core.MediaType.APPLICATION_JSON)
                .accept(javax.ws.rs.core.MediaType.APPLICATION_JSON)
                .get(entityClass);
    }

    public void create(T requestEntity) throws ClientErrorException {
//        Entity<T> entity = Entity.entity(requestEntity, MediaType.APPLICATION_JSON);
//        System.out.println("entity: " + entity);
//        Response response = client.target("http://localhost:8080/ws/user/create")
//             .request()
//             .post(entity);
        
        Response response = webTarget.path("create").request().post(Entity.entity(requestEntity, MediaType.APPLICATION_JSON));
        
        System.out.println("response: " + response.getStatusInfo().getReasonPhrase());
        response.close();
    }

    public boolean update(T requestEntity, String id) throws ClientErrorException {
        WebTarget resource = webTarget.path(java.text.MessageFormat.format("update/{0}", new Object[]{id}));
        
        Response response = resource.request().put(Entity.entity(requestEntity, MediaType.APPLICATION_JSON));
        
        //Response response = webTarget.path("update").request().put(Entity.entity(requestEntity, MediaType.APPLICATION_JSON));
        
        int code = response.getStatusInfo().getStatusCode();
        response.close();
        return code == 200;
    }

    public boolean delete(String id) throws ClientErrorException {
        WebTarget resource = webTarget.path(java.text.MessageFormat.format("delete/{0}", new Object[]{id}));
        Response response = resource.request(javax.ws.rs.core.MediaType.TEXT_PLAIN).delete();
        
        //System.out.println("response phrase: " + response.getStatusInfo().getReasonPhrase());
        //System.out.println("response statusCode: " + response.getStatusInfo().getStatusCode());
        
        int code = response.getStatusInfo().getStatusCode();
        response.close();
        return code == 200;
    }

    public void close() {
        client.close();
    }
    
}

