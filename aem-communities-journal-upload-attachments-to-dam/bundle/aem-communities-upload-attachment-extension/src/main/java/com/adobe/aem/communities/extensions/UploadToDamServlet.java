package com.adobe.aem.communities.extensions;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;

import javax.jcr.Session;

@Component(label = "Upload to Dam Servlet", immediate = true, enabled = true,
        description = "This servlet provides facility to upload attachments to dam instead of SRP")
@Service
@Properties({@Property(name = "sling.servlet.paths", value = "/services/attachments/upload/dam")
})
public class UploadToDamServlet extends SlingAllMethodsServlet {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private static final String COMMUNITY_ADMINISTRATOR = "community-administrators";
    private static final String ANONYMOUS = "anonymous";
    private static final String USER_ID = "userId";
    private static final String UPLOAD_ATTACHMENT = "getUserSubscriptions";
    private static final String DELETE_ATTACHMENT = "removeUserSubscriptions";


    @Override
    protected void doPost(final SlingHttpServletRequest request, final SlingHttpServletResponse response) {
        String operation = request.getParameter(OPERATION);

        //Check for operation
        if (operation != null) {
            if (operation.equals(GET_SUBSCRIPTIONS)) {
                getUserSubscriptions(request, response);
            } else if(operation.equals(REMOVE_SUBSCRIPTIONS)){
                removeUserSubscriptions(request, response);
            }
        } else {
            logger.error("No operation specified");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }
    }



    private boolean isCommunityAdmin(final ResourceResolver resourceResolver) {
        Session session = resourceResolver.adaptTo(Session.class);
        Authorizable authorizable = null;
        try {
            UserManager userManager = ((JackrabbitSession) session).getUserManager();
            if(userManager != null) {
                authorizable = userManager.getAuthorizable(resourceResolver.getUserID());
                Iterator<Group> groups = authorizable.memberOf();
                while (groups.hasNext()) {
                    Group group = groups.next();
                    if (group.getID().equals(COMMUNITY_ADMINISTRATOR)) {
                        return true;
                    }
                }
            }
        } catch (RepositoryException ex) {
            logger.error("Unable to find current user's group memberships");
        }
        return false;
    }

    private boolean isUserAuthorized(final SlingHttpServletRequest request) {
        ResourceResolver resourceResolver = request.getResourceResolver();
        String userId = resourceResolver.getUserID();

        //return true if
        //1. if it's an admin session
        //2. no user id in request and session is not anonymous
        //3. user id in request is equal to resource resolver user id, i.e. user trying to access it's own subscriptions
        //4. user in resource resolver is a community admin
        if(resourceResolver.adaptTo(User.class).isAdmin() || (request.getParameter(USER_ID) == null && !userId.equals(ANONYMOUS))
                || request.getParameter(USER_ID).equals(userId) || isCommunityAdmin(resourceResolver)) {
            return true;
        } else {
            return false;
        }
    }
}
