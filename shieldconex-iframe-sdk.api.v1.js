(function() {
    var defaultBaseUrl = "https://shieldconex.com";
    var SCFR = "ShieldconexIFrame";
    var defaultIFrameAttributes = {
        frameborder: 0,
        width: "100%",
        'aria-live': 'polite'
    };

    var eventMethod = window.addEventListener
        ? "addEventListener"
        : "attachEvent";
    var eventer = window[eventMethod];
    var messageEvent = eventMethod === "attachEvent"
        ? "onmessage"
        : "message";
    var loadEvent = eventMethod === "attachEvent"
        ? "onload"
        : "load";


    /*
    {
        baseUrl,
        templateId,
        templateData,
        preview,
        parent,
        immediate,
        attributes
    }
    */
    window[SCFR] = function (cfg) {
        this.id = '_' + Math.random().toString(36).substr(2, 9);
        this.cfg = cfg || { templateId: null };
        this.cfg.immediate = (this.cfg.immediate === undefined) ? false : this.cfg.immediate;
        this.cfg.baseUrl = (this.cfg.baseUrl === undefined || this.cfg.baseUrl == null) ?  defaultBaseUrl : this.cfg.baseUrl;
        this.cfg.attributes = this.cfg.attributes || JSON.parse(JSON.stringify(defaultIFrameAttributes));
        this.iframe = null;
        this.onRendered = null;
        this.rendered = false;

        if (this.cfg.immediate && this.cfg.parent && (this.cfg.templateId || this.cfg.preview && this.cfg.templateData)) {
            var parentNode = window.document.getElementById(this.cfg.parent);
            if (parentNode != null) {
                this.render();
            } else {
                eventer(loadEvent, this.render.bind(this));
            }
        }
    };

    this[SCFR].prototype.render = function(templateId, parent, attributes) {
        templateId = templateId || this.cfg.templateId;
        parent = parent || this.cfg.parent;
        attributes = attributes || this.cfg.attributes;
        var parentNode = window.document.getElementById(parent);
        if (parentNode == null) {
            throw new Error("Cannot find iframe parent object");
        }

        var po = document.createElement('iframe');
        var uriPart = templateId;
        if (this.cfg.preview) {
            uriPart = '_';
        } else {
            po.src = this.cfg.baseUrl + '/iframe/' + uriPart;
        }

        po.setAttribute('id', 'frame_' + templateId);
        po.setAttribute('name', 'frame_' + templateId);

        for (var p in attributes) {
            var val = attributes[p];
            po.setAttribute(p, val);
        }

        this.iframe = parentNode.appendChild(po);
        eventer(messageEvent, onIncomingMessage.bind(this));

        this.rendered = true;
        if (typeof this.onRendered === "function") {
            this.onRendered(this);
        }
    };

    this[SCFR].prototype.tokenize = function(echo, extras) {
        if (!this.rendered) {
            throw new Error("Render should be performed before tokenization");
        }

        var message = {
            method: "tokenize",
            senderId: this.id,
            echo: echo,
            referrer: window.location.href,
            extras: extras || {}
        };

        this.iframe.contentWindow.postMessage(message, "*");
    };

    this[SCFR].prototype.clear = function(echo, extras) {
        if (!this.rendered) {
            throw new Error("Render should be performed before clear");
        }

        var message = {
            method: "clear",
            senderId: this.id,
            echo: echo,
            referrer: window.location.href,
            extras: extras || {}
        };

        this.iframe.contentWindow.postMessage(message, "*");
    };

    var onIncomingMessage = function(evt) {
        var data = evt.data || {};
        if (data.receiverId === this.id || data.receiverId === "*") {
            switch(data.method) {
                case "scfr:error":
                    this.onError(data.echo, data.errors);
                    break;
                case "scfr:processing":
                    this.onProcessing(data.echo, data.status);
                    break;
                case "scfr:token":
                    this.onToken(data.echo, data.token, data.state);
                    break;
                case "scfr:validating":
                    this.onValidating(data.echo, data.validation);
                    break;
                case "scfr:clear":
                    this.onClear(data.echo, data.validation);
                    break;
            }
        }
    };

    this[SCFR].prototype.onError = function(echo, errors) {
        var err = new Error("Error occured");
        err.echo = echo;
        err.errors = errors;
        throw err;
    };

    this[SCFR].prototype.onProcessing = function(echo, status) {
        console.log('tokenization status: ' + status);
    };

    this[SCFR].prototype.onToken = function(echo, token, state) {
        console.log('BFID: ' + token);
    };

    this[SCFR].prototype.onValidating = function(echo, validation) {
        console.log('onValidating: ' + validation);
    };

    this[SCFR].prototype.onClear = function(echo) {
        console.log('onClear: ' + echo);
    };

})();
