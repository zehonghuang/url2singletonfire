//https://code.jquery.com/jquery-3.1.0.min.js
var system = require('system');
var webpage = require('webpage');
var cprocess = require("child_process");
var fs = require('fs');

var url = system.args[1];

var img_path = system.args[2];

var separator = system.os.name.toUpperCase == 'WINDOWS'?'\\':'/';

var resUrl = [];
// create and set page
var page = webpage.create();

//console.log(url);

page.onConsoleMessage = function (msg) {
    //console.log('console: ' + msg);
};

page.viewportSize = {
    width:  1024,
    height: 768 * 3
}

//我的Initialized
page.onInitialized = function() {
    // body...
};


//我的onLoadFinished
page.onLoadFinished = function(status) {

}

page.onResourceRequested = function(requestDate, networkRequest) {
    var res = requestDate.url;
    if(/google.*.com/i.test(res)) {
         networkRequest.abort();
    }
}


page.onResourceReceived = function(response) {
    // console.log(response.url);
}

page.onResourceTimeout = function(response) {

}

page.onResourceError = function(resourceError) {
    // phantom.exit();
    // body...
}

// send request
page.open(url,  function(status) {
//    console.log('start...');
	setTimeout(checkReadyState, 3000);
});

function checkReadyState() {
//    console.log('coming!')
    var readyState = page.evaluate(function () {
            return document.readyState;
        });
    // console.log(readyState);
    if ("complete" === readyState) {
        pageContext = onPageReady();
        console.log(pageContext);
        page.render(img_path);
        setTimeout(function() {
            phantom.exit();
        }, 10000);
    }
}

setTimeout(function() {
    phantom.exit();
}, 40000);

function onPageReady() {
    return page.evaluate(function () {
        return document.documentElement.innerHTML;
    });
}

function sleep(time) {
    for(var start = Date.now(); Date.now() - start <= time; ){}
}