aem-communities-journal-extension
=================================
This extension adds a subtitle property to the blog, which gets displayed under the title in blog.

Building the sample
===================

* Change directory to the root of the repository aem-communities-journal-extension.
* Run *mvn clean install*.
* A successful build should create a bundle artifact in *bundles/aem-communities-journal-extension* and a package artifact(with bundle embedded) in *content/target*.

Installing the sample
=====================

* Use the package manager at http://[server]/crx/packmgr/index.jsp and upload the zip file found at *content/target*.
* Install the package.
* Add a property "subtitle" in /system/console/configMgr/com.adobe.cq.social.journal.client.endpoints.impl.JournalOperationsService (or search JournalOperationProvider in /system/console/configMgr) of author and publish instance.
* Add the new journal from design dialog in your site and you are good to go. Please make sure, you have added relevant configurations such allow Rich Text Editor, allow following etc by editing the site.


IMPORTANT NOTE
==============
* For AEM version greater than or equal to 6.4, design path has changed. Please, use branch 6.3 for AEM versions smaller than or equal to 6.3.