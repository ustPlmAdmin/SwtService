define("DS/SWTUtils/api", ["DS/WAFData/WAFData"], function (WAFData) {
    var api = {
        SecurityContext: "",
        getBaseUrl: function () {
            let baseUrl = "https://3dspace.sw-tech.by:444/3dspace/"
            if (window.location.hostname.indexOf("study") !== -1)
                baseUrl = "https://3dspace-study.sw-tech.by:444/3dspace/"
            if (window.location.hostname.indexOf("m001") !== -1)
                baseUrl = "https://3dspace-m001.sw-tech.by:444/3dspace/"
            return baseUrl
        },
        url: function (id) {
            return this.getBaseUrl() + "common/emxNavigator.jsp?objectId=" + id;
        },
        init: function (callback) {
            WAFData.authenticatedRequest(this.getBaseUrl() + "resources/modeler/pno/person?current=true&select=preferredcredentials", {
                'method': 'GET',
                'type': 'json',
                'onComplete': function (e) {
                    api.SecurityContext = e.preferredcredentials.role.name + "." +
                        e.preferredcredentials.organization.name + "." +
                        e.preferredcredentials.collabspace.name
                    callback()
                }
            });
        },
        getBlob: function (url, params, success, error) {
            WAFData.authenticatedRequest(this.getBaseUrl() + "sw/" + url, {
                timeout: 60 * 60 * 1000,
                responseType: 'blob',
                data: params,
                headers: {
                    SecurityContext: api.SecurityContext
                },
                onComplete: function (data) {
                    success(data);
                }, onFailure: error
            });
        },
        getString: function (url, params, success, error) {
            this.getBlob(url, params, function (blob) {
                let reader = new FileReader();
                reader.onload = function () {
                    if (success != null)
                        success(reader.result)
                }
                reader.readAsText(blob);
            }, error)
        },
        getJson: function (url, params, success, error) {
            this.getString(url, params, function (data) {
                if (success != null)
                    success(data === "" ? null : JSON.parse(data));
            }, error);
        },
        download: function (filename, url, params, success, error) {
            this.getBlob(url, params, function (blob) {
                var a = document.createElement("a")
                a.href = URL.createObjectURL(blob)
                a.download = filename
                a.click()

                if (success != null)
                    success()
            }, error)

        }
    };
    return api;
    }
)