/*
 *
 * ADOBE CONFIDENTIAL
 * __________________
 *
 *  Copyright 2014 Adobe Systems Incorporated
 *  All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Adobe Systems Incorporated and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Adobe Systems Incorporated and its
 * suppliers and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Adobe Systems Incorporated.
 */

(function($CQ, _, Backbone, SCF, ModerationFilterManager) {
    // embed cq.social.author.hbs.dashboard in clientlibs
    "use strict";

    var filterName = "social:tags";

    var filter = ModerationFilterManager.ModerationFilter.extend({

    getValue : function(data){
       // user filter selections data
       if ($(".scf-js-social-console-taglist coral-taglist").length) {
          var tags = $(".scf-js-social-console-taglist coral-taglist").get(0).values;
          data[filterName] = tags;
          }
       return data;
       },

    setValue : function(dataObject){
        if (dataObject.hasOwnProperty(filterName)) {
            var userlistCtrl = $(".scf-js-social-console-taglist").get(0);
            Coral.commons.ready(userlistCtrl, function() {
            var $tag = $(".scf-js-social-console-taglist coral-taglist").get(0);
            $tag.on('coral-collection:add', function(event) {
                        event.detail.item.label.innerHTML = event.detail.item.value;
                    });
            for (var i = 0; i < dataObject[filterName].length; i++) {
               var val = dataObject[filterName][i];
               $tag.items.add({
                  value: val,
                  content: {
                     innerHTML: val
                    },
                  selected: true
                  });
               }
            });
            }
       },
     loadTags : function(ctrl,tagListURL) {

		var that = this;
        that.request = null;
        var selectedTags = "";

         /* global Coral */
        Coral.commons.ready(ctrl, function() {
            ctrl.set({
                forceSelection: true
            });
            ctrl.on("coral-autocomplete:showsuggestions", function(event) {
                if (that.request) {
                    that.request.abort();
                }
                event.preventDefault();
                var url = SCF.config.urlRoot + tagListURL;
                if( event.detail.value){
                    url += '?term='+ encodeURIComponent(event.detail.value);
                }
                that.request = $.get(url,
                    function(data) {
                        var suggestions = [];
                        if (data && data.length > 0) {
                            $CQ.each(data, function(index, item) {
                                suggestions.push({
                                    value: item.tagid,
                                    content: '<span class="scf-js-console-tag-name-block">' + item.label + '</span>' +
                                        '<div class="scf-js-console-tag-id">' + item.tagid + '</div>'

                                });
                            });
                        }
                        ctrl.addSuggestions(suggestions);
                    }, "json");
            });
            ctrl.on("coral-autocomplete:hidesuggestions", function() {
                $(ctrl).find(".coral-Autocomplete-input").val("");
                ctrl.invalid = false;
                $(ctrl).find("coral-tag-label .scf-js-console-tag-id").hide();

                if (that.request) {
                    that.request.abort();
                }
            });
            if (selectedTags.length) {
                $CQ.each(selectedTags, function(index, item) {
                    ctrl.items.add({
                        value: item.tagid,
                        content: {
                            innerHTML: item.tagid
                        },
                        selected: true
                    });
                });
            }
        });

     }
    });
ModerationFilterManager.addFilter(filterName, filter);

$(document).ready(function() {
    var ctrl = $.find(".scf-js-social-console-taglist")[0];
	filter.loadTags(ctrl,"/services/tagfilter");
    });

})($CQ, _, Backbone, SCF, ModerationFilterManager);
