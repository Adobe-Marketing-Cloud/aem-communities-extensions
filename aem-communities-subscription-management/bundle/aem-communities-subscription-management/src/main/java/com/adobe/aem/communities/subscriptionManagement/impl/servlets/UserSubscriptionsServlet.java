package com.adobe.aem.communities.subscriptionManagement.impl.servlets;

import com.adobe.cq.social.graph.SocialGraph;
import com.adobe.cq.social.graph.Vertex;

import com.adobe.granite.socialgraph.Direction;
import com.adobe.granite.socialgraph.Relationship;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;

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

@Component(label = "User Subscriptions Servlet", immediate = true, enabled = true,
        description = "This servlet provides facility to fetch subcriptions and unsubscribe multiple subscriptions" +
                "of a given type for a user")
@Service
@Properties({@Property(name = "sling.servlet.paths", value = "/services/communities/user/subscriptions")
})
public class UserSubscriptionsServlet extends SlingAllMethodsServlet {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static final String CONTENT_TYPE = "application/json";
    private static final String RESPONSE_ENCODING = "UTF-8";
    private static final String OPERATION = "operation";
    private static final String ANONYMOUS = "anonymous";
    private static final String SUBSCRIPTION_TYPE = "subscriptiontype";
    private static final String SUBSCRIBED_ID = "subscribedId";
    private static final String USER_ID = "userId";
    private static final String EDGE_START = "startid_s";
    private static final String EDGE_END = "endid_s";
    private static final String CATEGORY = "rel_type_s";
    private static final String TYPE = "type_s";
    private static final String COMMUNITY_ADMINISTRATOR = "community-administrators";

    //Operations
    private static final String GET_SUBSCRIPTIONS = "getUserSubscriptions";
    private static final String REMOVE_SUBSCRIPTIONS = "removeUserSubscriptions";

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

    /**
     * Generate a list of user subscriptions based on the subscription type, e.g. subscription, notification, following
     * @param request the client request
     * @param response response to be sent to client
     * curl command example :  curl -u {username}:{password} '{publish instance}/services/communities/user/subscriptions'
     *                 --data 'operation=getUserSubscriptions&subscriptiontype=subscription[optional]&userId={userId}'
     */
    private void getUserSubscriptions(final SlingHttpServletRequest request, final SlingHttpServletResponse response) {
        try {
            ResourceResolver resourceResolver = request.getResourceResolver();

            if (isUserAuthorized(request)) {
                String substype = request.getParameter(SUBSCRIPTION_TYPE);
                if (substype != null) {
                    SocialGraph graph = resourceResolver.adaptTo(SocialGraph.class);
                    String graphRootPath = null;
                    if (graph != null) {
                        graphRootPath = resourceResolver.adaptTo(SocialGraph.class).getRootPath();
                    }
                    Iterator<Resource> relationships = null;
                    if (graphRootPath != null) {
                        relationships = resourceResolver.getResource(graphRootPath).listChildren();
                    }
                    CustomSubscription customSubscriptionObject = null;
                    //Fetch list of relationships in social graph and then extract relevant relationships
                    //& create a custom subscription object from them and add to result
                    ArrayList<CustomSubscription> subscriptions = new ArrayList<CustomSubscription>();
                    Resource resource = null;
                    if (relationships != null) {
                        while (relationships.hasNext()) {
                            resource = relationships.next();
                            ValueMap map = resource.adaptTo(ValueMap.class);
                            if (map != null) {
                                String ownerId = map.get(EDGE_START).toString();
                                String type = map.get(TYPE).toString();
                                //Add to result if the
                                if(ownerId.equals(getUserId(request)) && type.equals(substype)) {
                                    customSubscriptionObject = new CustomSubscription(map.get(EDGE_END).toString(), resource.getPath(), map.get(CATEGORY).toString());
                                    subscriptions.add(customSubscriptionObject);
                                }
                            }
                        }
                    }

                    //Serialize each subscription object to json and add to response
                    JsonArray subscriptionsArray = new JsonArray();
                    for(CustomSubscription user_subscription : subscriptions) {
                        String subscriptionJson = new Gson().toJson(user_subscription);
                        subscriptionsArray.add(new JsonPrimitive(subscriptionJson));
                    }
                    response.setContentType(CONTENT_TYPE);
                    response.setStatus(HttpServletResponse.SC_OK);
                    response.setCharacterEncoding(RESPONSE_ENCODING);

                    PrintWriter responseWriter = response.getWriter();
                    responseWriter.write(subscriptionsArray.toString());
                } else {
                    //No subscription type, bad request
                    logger.error("No subscription type specified");
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                }
            } else {
                //user is not authorized
                logger.error("User is not authorized");
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            }
        } catch (Exception ex) {
            //Unsubscription operation failed due to some exception
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
        }
    }

    /**
     * Unsubscribe a user from all the specified subscriptions of a given type, e.g. subscription, notification, following
     * @param request the client request
     * @param response response to be sent to client
     * curl command example : curl -u {username}:{password} '{publish instance}/services/communities/user/subscriptions'
     *                       --data 'operation=removeUserSubscriptions&subscriptiontype=subscription&
     *                       subscribedId={subscribeId1}
     *                       &subscribedId={subscribeId2}&..[optional]&userId={userId}'
     */
    private void removeUserSubscriptions(final SlingHttpServletRequest request, final SlingHttpServletResponse response) {
        try {
            ResourceResolver resourceResolver = request.getResourceResolver();
            int count = 0;
            if (isUserAuthorized(request)) {
                String subscribedIds[] = request.getParameterValues(SUBSCRIBED_ID);
                String type = request.getParameter(SUBSCRIPTION_TYPE);
                if(subscribedIds != null && subscribedIds.length != 0 && type != null) {
                    //Create social graph
                    SocialGraph graph = resourceResolver.adaptTo(SocialGraph.class);
                    Vertex subscribedNode = null;
                    for(String subscribedId : subscribedIds) {
                        //Initialize and subscribed resource vertex & fetch all incoming relationships on this resource
                        subscribedNode = graph.getVertex(subscribedId);
                        if (subscribedNode != null) {
                            Iterable<Relationship> relationships = subscribedNode.getRelationships(Direction.INCOMING, (String) null, type);
                            for (Relationship relationship : relationships) {
                                //If the start node of this relationship is the user id, delete it
                                if (relationship.get(EDGE_START).equals(getUserId(request))) {
                                    relationship.delete();
                                    count++;
                                }
                            }
                        } else {
                            logger.error("No such subscription");
                        }
                        //Save the changes
                        graph.save();
                    }

                    //Write results to response
                    PrintWriter responseWriter = response.getWriter();
                    JsonObject jsonObject = new JsonObject();
                    jsonObject.addProperty("Unsubscriptions", count);
                    responseWriter.write(jsonObject.toString());
                    response.setContentType(CONTENT_TYPE);
                    response.setStatus(HttpServletResponse.SC_OK);
                    response.setCharacterEncoding(RESPONSE_ENCODING);
                } else {
                    //No subscribed resource or subscription type, bad request
                    if(type == null) {
                        logger.error("No subscription type specified");
                    } else {
                        logger.error("No subscribed resource specified");
                    }
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                }
            } else {
                //user is not authorized
                logger.error("User is not authorized");
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            }
        } catch (Exception ex) {
            //Unsubscription operation failed due to some exception
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
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

    private String getUserId(final SlingHttpServletRequest request) {
        String userId = null;
        userId = request.getParameter(USER_ID);
        if(userId == null) {
            ResourceResolver resourceResolver = request.getResourceResolver();
            userId = resourceResolver.getUserID();
        }
        return userId;
    }

    private class CustomSubscription {
        private String subscribedId;
        private String path;
        private String category;

        public CustomSubscription(String subscribedId, String path, String category) {
            this.subscribedId = subscribedId;
            this.path = path;
            this.category = category;
        }

        public String getSubscribedId() {
            return this.subscribedId;
        }

        public String path() {
            return this.path;
        }

        public String getCategory () {
            return this.category;
        }
    }

}