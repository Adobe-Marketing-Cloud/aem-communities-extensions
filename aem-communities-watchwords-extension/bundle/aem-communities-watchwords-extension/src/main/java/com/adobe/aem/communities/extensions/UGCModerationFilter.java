package com.adobe.aem.communities.extensions;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;

import com.adobe.cq.social.ugcbase.moderation.AutoModerationProcess;
import com.adobe.cq.social.ugcbase.moderation.AutoModerationProcessException;

@Service(value = AutoModerationProcess.class)
@Component(label = "UGCModerationFilter", description = "Automoderate UGC", immediate = true, metatype = true)
public class UGCModerationFilter implements AutoModerationProcess {

    private static final String PN_SENTIMENT = "sentiment";
    private static final String PROP_APPROVED = "approved";
    private static final String PROP_SPAM = "isSpam";

    @Override
    public void execute(final Resource resource) throws AutoModerationProcessException {
        // check that the resource was set
        if (resource == null) {
            throw new AutoModerationProcessException("Can't execute on a null resource.");
        }
        if (hasProperty(resource, PN_SENTIMENT)) {
            int sentimentScore = getProperty(resource, PN_SENTIMENT, Integer.class).intValue();
            if (sentimentScore == 1) {
                setProperty(resource, PROP_APPROVED, false);
                setProperty(resource, PROP_SPAM, true);
                try {
                    resource.getResourceResolver().commit();
                } catch (final PersistenceException ex) {
                    throw new AutoModerationProcessException("Failed to mark the bad UGC as spam", ex);
                }
            }

        }

    }

    private boolean hasProperty(final Resource resource, final String propName) {
        final ValueMap valMap = resource.adaptTo(ValueMap.class);
        return valMap.containsKey(propName);
    }

    private <T> T getProperty(final Resource resource, final String propName, final Class<T> type) {
        final ValueMap valMap = resource.adaptTo(ValueMap.class);
        return valMap.get(propName, type);
    }

    private void setProperty(final Resource resource, final String propName, final Object value) {
        final ModifiableValueMap modValueMap = resource.adaptTo(ModifiableValueMap.class);
        modValueMap.put(propName, value);
    }
}