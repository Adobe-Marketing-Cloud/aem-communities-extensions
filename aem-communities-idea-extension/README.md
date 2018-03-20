aem-communities-idea-extension
==============================

Building the sample
===================

* Change directory to the root of the repository aem-communities-idea-extension.
* Run *mvn clean install*.
* A successful build should create a bundle artifact in *bundles/aem-communities-idea-extension* and a package artifact(with bundle embedded) in *content/target*.

Installing the sample
=====================

* Use the package manager at http://[server]/crx/packmgr/index.jsp and upload the zip file found at *content/target*.
* Install the package.
* Check out the ideation component at http://[server]/content/acme/en/ideas.html.

