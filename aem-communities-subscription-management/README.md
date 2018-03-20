aem-communities-subscription-management
=======================================
This sample provides a servlet with operations to get all subscriptions as well as to remove subscriptions of a user.

Building the sample
===================

* Change directory to the root of the repository aem-communities-subscription-management.
* Run *mvn clean install*.
* A successful build should create a bundle artifact in *bundles/aem-communities-subscription-management* and a package artifact(with bundle embedded) in *content/target*.

Installing the sample
=====================

* Use the package manager at http://[server]/crx/packmgr/index.jsp and upload the zip file found at *content/target*.
* Install the package.


Using the servlet
=================

* getUserSubscriptions : Generate a list of user subscriptions based on the subscription type, e.g. subscription, notification, following. If userId is not provided, subscriptions of current user are returned.
    curl command example :  curl -u {username}:{password} '{publish instance}/services/communities/user/subscriptions'
							--data 'operation=getUserSubscriptions&subscriptiontype=subscription[optional]&userId={userId}'
    
	 
* removeUserSubscriptions : Unsubscribe a user from all the specified subscriptions of a given type, e.g. subscription, notification, following. If userId is not provided, subscriptions of current user are removed.
    curl command example : curl -u {username}:{password} '{publish instance}/services/communities/user/subscriptions'
						   --data 'operation=removeUserSubscriptions&subscriptiontype=subscription&subscribedId={subscribeId1}&subscribedId={subscribeId2}&..[optional]&userId={userId}'



