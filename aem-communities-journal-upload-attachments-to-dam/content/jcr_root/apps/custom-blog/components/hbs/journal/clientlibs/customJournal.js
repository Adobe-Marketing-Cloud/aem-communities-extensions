(function($CQ, _, Backbone, SCF) {
	"use strict";

   // var CustomBlog = SCF.Components["social/journal/components/hbs/journal"].Model;
    var CustomBlog = SCF.Journal.extend({

       addComment: function(data, scb, fcb) {

           $CQ('.scf-attachment-error').remove(); //remove previous error messages (if any)

            var success = _.bind(this.addCommentSuccess, this);
            var error = _.bind(function(jqxhr, text, error) {
                //Handles Server errror in case of bad attachments, etc.
                if (jqxhr.status == 401) {
                    var siteLink = $($(".scf-js-site-title")[0]).attr("href");
                    window.location.href = "http://" + location.host + siteLink.replace(".html", "/signin.html");
                } else {
                    if (500 == jqxhr.status) { //vs bugfix
                        var _parentEl = $CQ('.scf-composer-block')[0];
                        if (null === _parentEl) {
                            _parentEl = $CQ(document.body);
                        }
                        $CQ(
                            '<div class="scf-attachment-error"><h3 class="scf-js-error-message">Server error. Please try again.</h3><div>'
                        ).appendTo(_parentEl);

                        return false;
                    }
                    this.trigger(this.events.ADD_ERROR, this.parseServerError(jqxhr, text, error));
                }

            }, this);


            var postData;
            var damPostData;
            var hasAttachment = (typeof data.files !== "undefined");
            var hasTags = (typeof data.tags !== "undefined");
            if (hasAttachment) {
                // Create a formdata object and add the files
                if (window.FormData) {
                    postData = new FormData();
                }

                if (postData) {

                    var dam_attachements_ss = new Array();
                    var that=this;

                    $CQ.each(data.files, function(key, value) {
                        damPostData = new FormData();
                        damPostData.append(":replaceAsset",true);
                        damPostData.append("file", value);
						console.log(value.name);
                        damPostData.append("fileName",value.name);
                        //postData.append("");
                     //[Note] Posting data to dam here instead and the files data is not added to postData so that it doesn't get added to ASRP.
                     //This as of now will only work for admin user
                     //A servlet need to be created that would consume this call instead of dam's createasset.html
                     //Servlet will post this request to dam using a service user or the admin user session.
                     //The path here needs to be made dynamic - based on the ugc path -something like /content/dam/<site>/<group>/<blog-title>
                     //Handle here in the call to keep connection alive for sending large binaries.
                    $CQ.ajax("/content/dam/test.createasset.html", {
                	dataType: "json",
                	type: "POST",
                	processData: !hasAttachment,
                	contentType: (hasAttachment) ? false : "application/x-www-form-urlencoded; charset=UTF-8",
                	xhrFields: {
                    	withCredentials: true
                	},
                	data: that.addEncoding(damPostData),
                	"success": success,
                	"error": error
            		});

                   // [Note] add the link to the dam file as field dam_attachements_ss -- this will add this to the properties of created ugc
                   //keeping a name of field as *_ss (for multivalued fields) or *_s (for sinlge valued fields) will make them indexed in solr.
                   // File name can also be added here as a separate field.
                   postData.append("dam_attachements_ss","/content/dam/test/"+value.name);

                    });

                    postData.append("id", "nobot");
                    postData.append(":operation", this.createOperation);

                    delete data.files;
                    if (hasTags) {
                        $CQ.each(data.tags, function(key, value) {
                            postData.append("tags", value);
                        });
                    }
                    delete data.tags;
                    $CQ.each(data, function(key, value) {
                        postData.append(key, value);
                    });
                }
            } else {
                postData = {
                    "id": "nobot",
                    ":operation": this.createOperation
                };
                _.extend(postData, data);
                postData = this.getCustomProperties(postData, data);
            }

            $CQ.ajax(SCF.config.urlRoot + this.get("id") + SCF.constants.URL_EXT, {
                dataType: "json",
                type: "POST",
                processData: !hasAttachment,
                contentType: (hasAttachment) ? false : "application/x-www-form-urlencoded; charset=UTF-8",
                xhrFields: {
                    withCredentials: true
                },
                data: this.addEncoding(postData),
                "success": success,
                "error": error
            });

        }




    });
    var CustomBlogView = SCF.JournalView.extend({

        openAttachmentDialog: function(e) {
            if (SCF.Util.mayCall(e, "preventDefault")) {
                e.preventDefault();
            }
            this.$el.find("input[type='file']").first().click();
        },
        addToCommentModel: function(data, e) {
            this.model.addComment(data);
            if (e.target) {
                e.preventDefault();
            }
            return false;
        },

        addComment: function(e) {
            var data = this.extractCommentData(e);
            if (data === false) return false;
            return this.addToCommentModel(data, e);
        },
        extractCommentData: function(e) {
            var form = this.getForm("new-comment");
            if (form === null || form.validate()) {
                var msg = this.getField("message");
                var subtitle = this.getField("subtitle");
                var tags = this.getField("tags");
                var data = _.extend(this.getOtherProperties(), {
                    "message": msg,
                    "subtitle": subtitle,
                    "tags": tags
                });
                if (!SCF.Session.get("loggedIn")) {
                    data.userIdentifier = this.getField("anon-name");
                    data.email = this.getField("anon-email");
                    data.url = this.getField("anon-web");
                }
                if (typeof this.files !== "undefined") {
                    data.files = this.files;
                }
                return data;
            } else {
                return false;
            }
        }
    });

    var CustomBlogTopic = SCF.BlogTopic.extend({

        getCustomProperties: function() {
            var customData = {
                subject: this.get("subject")
            };
            customData.subtitle = this.get("subtitle");
            if (this.has("isDraft")) {
                customData.isDraft = this.get("isDraft");
                var publishDate = this.get("publishDate");
                if (!_.isEmpty(publishDate)) {
                    customData.publishDate = publishDate;
                    customData.isScheduled = true;
                }
            }
            if (this.getConfigValue("usingPrivilegedUsers")) {
                var composedFor = this.get("composedFor");
                if (!_.isEmpty(composedFor)) {
                    customData.composedFor = composedFor;
                }
            }

            return customData;
        }
    });

    var CustomBlogTopicView = SCF.BlogTopicView.extend({

            edit: function(e) {
            this.$el.find(".scf-js-journal-comment-section").toggleClass("scf-is-hidden");
            SCF.TopicView.prototype.edit.call(this, e);
            this.$el.find(".scf-js-topic-details").hide();
            this.$el.find(".scf-js-topic-details-tags-editable").show();
            this.$el.find(".scf-comment-toolbar .scf-comment-edit").hide();

            var subject = this.model.get('subject');
            this.setField("editSubject", subject);
            this.focus("editSubject");

            var subtitle = this.model.get('subtitle');
            this.setField("editSubtitle", subtitle);

            if (!this.eventBinded) {
                this.bindDatePicker(e);
                this.eventBinded = true;
            }
        },
        getOtherProperties: function(isReply) {
            var subject = this.getField("editSubject").trim();
            var subtitle = this.getField("editSubtitle").trim();
            var tags = this.getField("editTags");
            var props = {
                'tags': tags
            };
            if (!isReply) {
                props["subject"] = subject;
                props["subtitle"] = subtitle;
            }
            var publishMode = $(this.$el.find(".scf-js-pubish-type > button > span")).text();
            var publishDate = null;
            if (!_.isEmpty(publishMode) && publishMode == $(this.$el.find(
                    ".scf-js-pubish-type > ul > li > a")[1])
                .text()) {
                props.isDraft = true;
            } else if (!_.isEmpty(publishMode) && publishMode == $(this.$el.find(
                    ".scf-js-pubish-type > ul > li > a")[2]).text()) {
                props.isDraft = true;
                props.isScheduled = true;
                props.publishDate = getDateTime(this.$el.find(
                        ".scf-js-event-basics-start-input").val(),
                    this.$el.find(".scf-js-event-basics-start-hour").val(), this.$el.find(
                        ".scf-js-event-basics-start-min").val(), this.$el.find(".scf-js-event-basics-start-time-ampm").val());
            } else {
                props.isDraft = false;
            }
            if (this.model.getConfigValue("usingPrivilegedUsers")) {
                var composedFor = this.getField("composedFor");
                if (!_.isEmpty(composedFor)) {
                    props.composedFor = composedFor;
                }
            }
            this.eventBinded = false;
            return props;
        }
    });

    SCF.registerComponent('/apps/custom-blog/components/hbs/entry_topic', CustomBlogTopic, CustomBlogTopicView);
	SCF.registerComponent('/apps/custom-blog/components/hbs/journal', CustomBlog, CustomBlogView);


})($CQ, _, Backbone, SCF);