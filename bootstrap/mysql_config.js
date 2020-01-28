var mysql = require("mysql");
var iniparser = require('iniparser');

var config = iniparser.parseSync('./config/db.ini');

var pool=mysql.createPool({
  connectionLimit : 20,
    host: config['host'],
    user: config['user'],
    password:config['pwd'],
    database: config['dbname']
});

var DB = (function () {

  function _query(query, params, callback) {
      pool.getConnection(function (err, connection) {
          if (err) {
            //  connection.release();
              callback(null, err);
              throw err;
          }
         
          connection.query(query, params, function (err, rows) {
              connection.release();
              if (!err && rows.length >0) {
                callback(null,rows);
               // var json = JSON.stringify(rows);
               // callback(null,json);
                 
              }
              else if(!err && rows.length==0) {
                  callback(null, 'no data');
              }
              else callback(null, err);

          });

          connection.on('error', function (err) {
              connection.release();
              callback(null, err);
              throw err;
          });
      });
  };

  return {
      query: _query
  };
})();

module.exports = DB;


// //./mysql_config.js
// module.exports = {
//     connectionLimit: 20,
//     host: 'localhost',
//     user: 'root',
//     password: 'jhlee321',
//     port: 3306,
//     database: 'test'
//   };
  
  
  