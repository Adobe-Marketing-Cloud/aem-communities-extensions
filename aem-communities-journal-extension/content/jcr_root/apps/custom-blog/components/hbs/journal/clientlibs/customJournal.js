(function($CQ, _, Backbone, SCF) {
	"use strict";

    var CustomBlog = SCF.Components["social/journal/components/hbs/journal"].Model;
    var CustomBlogView = SCF.JournalView.extend({
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
                subject: $("input[name=editSubject]").val().trim()
            };
            customData.subtitle = $("input[name=editSubtitle]").val().trim();
            customData.message = $("textarea[name=editMessage]").val().trim();
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

            this.subject = customData.subject;
            this.subtitle = customData.subtitle;
            this.message = customData.message;

            return customData;
        }
    });

    var CustomBlogTopicView = SCF.BlogTopicView.extend({
        //on clicking edit og entry topic
        edit: function(e) {
            this.$el.find(".scf-js-journal-comment-section").toggleClass("scf-is-hidden");

            e.stopPropagation();
            this.model.set('editTranslationInProgress', false);
            var editBox = $($('input[name=editSubtitle]')).closest(".scf-js-comment-edit-box");
            editBox.toggle();

            //resizing might be needed for content field of RTE
            $('textarea[name=editMessage]').height(400);
            $($('input[name=editSubtitle]')).closest(".scf-js-comment-msg").toggle();
            this.$el.find(".scf-comment-action").hide();
            var message = this.model.get("message");
            if (!this.model.getConfigValue("isRTEEnabled")) {
                //Assume text is not encoded
                message = $CQ("<div/>").html(text).text();
            }
            var attachments = this.$el.find(".scf-js-edit-attachments").not(this.$("[data-scf-component] .scf-js-edit-attachments"));
            var that = this;
            if ($CQ().imagesLoaded) {
                attachments.imagesLoaded(function() {
                    that.editableAttachments = new SCFCards(attachments);
                });
            }

            this.$el.find(".scf-js-topic-details").hide();
            this.$el.find(".scf-js-topic-details-tags-editable").show();
            this.$el.find(".scf-comment-toolbar .scf-comment-edit").hide();

            var subject = this.model.get('subject');
            this.setField("editSubject", subject);
            this.focus("editSubject");

            var subtitle = this.model.get('subtitle');
            this.setField("editSubtitle", subtitle);

            this.setField("editMessage", message);

            if (!this.eventBinded) {
                this.bindDatePicker(e);
                this.eventBinded = true;
            }
        },
        //we need to extend get data because we are using a custom composer
        getData: function() {
            var textareaVal = $("textarea[name=editMessage]").val().trim();
            var tags = $("input[name=editTags]").val();
            var data = _.extend(this.getOtherProperties(), {
                message: textareaVal,
                "tags": tags
            });

            if (typeof this.files != 'undefined') {
                data.files = this.files;
            }
            return data;
        },
    });

    SCF.registerComponent('/apps/custom-blog/components/hbs/entry_topic', CustomBlogTopic, CustomBlogTopicView);
	SCF.registerComponent('/apps/custom-blog/components/hbs/journal', CustomBlog, CustomBlogView);

})($CQ, _, Backbone, SCF);