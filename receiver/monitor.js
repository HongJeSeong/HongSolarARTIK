'use strict';
require('date-utils');
var ConfigWSParameters = require("./config.json");
var WebSocket = require("ws");
var ConfigWSConnection = {

    "scheme": "wss",
    "domain": "api.artik.cloud",
    "version": "v1.1",
    "path": "live"

};

function getConnectionParameters(config) {

    return "authorization=bearer+" + config.userToken + "&" +
                "sdids=" + config.deviceID;
}

function getConnectionString(config, parameters) {

    var connectionString =
            config.scheme + "://" +
            config.domain + "/" +
            config.version + "/" +
            config.path + "?" +
            getConnectionParameters(parameters);

    console.log("Connecting to: ", connectionString);

    return connectionString;

}
/*DB*/
var sqlite3 = require('sqlite3').verbose();
var db = new sqlite3.Database('053db.db');

/*create connection*/
var ws = new WebSocket(
    getConnectionString(ConfigWSConnection, ConfigWSParameters));

/*listen*/
ws.on("message", function (data) {
    console.log("Received message with data: %s\n", data);
var ping=data.indexOf("ping");
var error=data.indexOf("error");
var rssi=data.indexOf("rssi");

if(ping==-1)
if(error==-1)
if(rssi!=-1)
  db.serialize(function() {
  db.run("CREATE TABLE if not exists data (rssi double, time TEXT)");

  var resr=data.substring(rssi+6,rssi+11);
  var out=parseFloat(resr);
  var stmt = db.prepare("INSERT INTO data VALUES (?,?)");
  var d = new Date();
  var n = d.toFormat('YYYY-MM-DD HH24:MI:SS');
      stmt.run(-out,n);
      stmt.finalize();

  db.each("SELECT rssi, time FROM data", function(err, row) {
      console.log(row.rssi + ": " + row.time+"[receive data]");
  });
});

});

ws.on("open", function () {
    console.log("Websocket connection is open ...");
});

ws.on("close", function () {
    console.log("Websocket connection is closed ...");
    db.close();
});

