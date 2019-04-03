package sample.moderation.filter.core.filters;

import com.adobe.cq.social.moderation.dashboard.api.ModerationFilter;
import com.adobe.cq.social.ugc.api.ConstraintGroup;
import com.adobe.cq.social.ugc.api.ValueConstraint;
import org.osgi.framework.Constants;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@Component(immediate = true)
@Properties({
		@Property(name = Constants.SERVICE_RANKING, intValue = 10)
})
@Service(value=ModerationFilter.class)
public class ModerationTagFilter implements ModerationFilter {
	/** default log. */
	private final Logger LOG = LoggerFactory.getLogger(getClass());

	private static final String FILTER_TAG = "social:tags";
	private static final int TAG_LIMIT = 10;
	private static String PROP_TAG_NAME = "cq:tags";
	/**
	 * create OR constraints for each tag
	 * @param predicates Map of all the predicates sent from Moderation dashboard UI
	 * All constraintgroups will finally run with an AND operator for Moderation Search
	 * @return  The ConstraintGroup containing all the tag constraints.
	 */
	public ConstraintGroup getFilterConstraint(final Map<String,String[]> predicates){
		boolean hasConstraints = false;
		final ConstraintGroup cg = new ConstraintGroup();
		final String[] predicateValues = predicates.get(this.getSupportedPredicate());
		if (predicateValues != null && predicateValues.length > 0) {
			for (final String tag : predicateValues) {
					cg.or(new ValueConstraint<String>(PROP_TAG_NAME, tag ));
					hasConstraints = true;
					LOG.debug("Searching for tag - {}", tag);

			}
		}

		return hasConstraints ? cg : null;
	}
	/**
	 * Predicate Name 'namespace:string'. It defines which predicate this filter supports e.g. social:sentiment
	 * @return String fully qualified predicateName
	 */
	public String getSupportedPredicate(){
		return FILTER_TAG;
	}
}
