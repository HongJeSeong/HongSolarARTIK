require('date-utils');
var ConfigWSRegistrationInfo = require('./config.json');

var sqlite3 = require('sqlite3').verbose();
var useDb = new sqlite3.Database('/workspace/websocket/receiver/053db.db');
var inverterDb = new sqlite3.Database('/hongsolar/hsEnergyData.db');

var WebSocket = require('ws');
const websocketUrl = "wss://api.artik.cloud/v1.1/websocket?ack=true";


console.log("Connecting to url: ", websocketUrl);
var ws = new WebSocket(websocketUrl);
var init=0;
        var accumulated_gen;
        var accumulated_use;
        var co_dec;
        var hanjeon;
        var month_gen;
        var month_use;
        var now_gen;
        var now_use;
        var today_gen;
        var today_use;
        var time_use;
    var todayTime = "";
    var monthTime = "";



ws.on('open', function() {
    console.log('WebSocket connection is open ...');
    sendRegistrationMessage();
});


ws.on('message', function(data, flags) {
    console.log('Received message: %s\n', data);

    var message = JSON.parse(data);

    if (message.type === 'action') {
        //제어를 위한 코드
    }

    if (message.type === 'ping') {

  var d = new Date();
  var n = d.toFormat('YYYY-MM-DD HH24:MI:SS');
  var time1 = n.substring(0,14);
  var time2 = n.substring(0,15);
  var like1 = "%"+time1+"%";
  var like2 = "%"+time2+"%";
console.log(n);
 useDb.each('SELECT * FROM data WHERE time LIKE ?',[like2],
 function(err, row) {
   now_use=row.rssi;
   var realUse = now_use / 360;
   time_use=row.time;

   if(monthTime != "" && monthTime == time_use.substring(5,6)) {
      month_use += today_use;
   } else if(monthTime != "" && monthTime != time_use.substring(5,6)) {
      month_use = 0;
   }

   if(todayTime != "" && todayTime == time_use.substring(8,9)) {
      today_use += realUse;
      accumulated_use += realUse;
   } else if(todayTime != "" && todayTime != time_use.substring(8,9)) {
      today_use = 0;
   }

    console.log(row);
  });
inverterDb.each('SELECT * FROM virtual_inverter WHERE time LIKE ?',[like1]
,
 function(err, row) {
        accumulated_gen=row.ACCUMULATED_GEN;
        co_dec=row.CO_DEC;
        hanjeon=row.HANJEON;
        month_gen=row.MONTH_GEN;
        now_gen=row.NOW_GEN;
        today_gen=row.TODAY_GEN;
    console.log(row);
    console.log(row.HANJEON);
  });
if(init==1){
  updateDeviceField({"accumulated_gen":accumulated_gen,"accumulated_use":accumulated_use,
"co_dec":co_dec,"hanjeon":hanjeon,
"month_gen":month_gen,"month_use":month_use,"now_gen":now_gen,"now_use":now_use,
"today_gen":today_gen,"today_use":today_use});
 }
 init=1;
}
});


ws.on('close', function() {
    console.log('Websocket connection is closed ...');
        useDb.close();
        inverterDb.close();
});



function sendRegistrationMessage() {
    var payload = {
        'type': 'register',
        'sdid': ConfigWSRegistrationInfo.deviceID,
        'authorization': 'bearer ' + ConfigWSRegistrationInfo.deviceToken,
        'cid': getTimeMillis(),
    };

    console.log(
        'Sending register message payload: %s',
        JSON.stringify(payload));

    sendMessage(payload);

}

function updateDeviceField(data) {
    var payload = {
        'sdid': ConfigWSRegistrationInfo.deviceID,
        'data': data,
        'cid': getTimeMillis(),
    };

    sendMessage(payload, 'Send message and update field:');
}

function sendMessage(payload, prefix) {
    console.log(prefix, JSON.stringify(payload));

    ws.send(JSON.stringify(payload), {
        binary: false,
        mask: true,
    });
}


function getTimeMillis() {
    return parseInt(Date.now().toString());
}

