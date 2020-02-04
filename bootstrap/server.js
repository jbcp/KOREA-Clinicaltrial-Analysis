// express 기본 모둘 부르기
var express = require('express');
var app = express(); //익스프레스 객체 생성
var http = require('http');
var dateTime = require('node-datetime');
// var path=require('path');
//var ejs = require('ejs');
//var mysql = require('mysql');
var fs=require('fs');
var util=require('util');
var stats = fs.statSync("./config/db.ini");


var fileUpdateDateTime = new Date(util.inspect(stats.mtime)).toLocaleDateString();

    
//express 미들웨어 부르기
var bodyParser = require('body-parser');
var expressErrorHandler = require('express-error-handler');

app.set('views', __dirname + '/public/views');
app.set('view engine', 'ejs');
app.engine('html', require('ejs').renderFile);

var port=80;
var server = app.listen(port, function () {
    // server.close();

    var dt = dateTime.create();
    var formatted = dt.format('Y-m-d H:M:S');

    console.log("============================================================");
    console.log(formatted + " ::Express server has started port "+port);
});




var startDate, endDate;

app.use(express.static('public'));

app.use(bodyParser.json());
//app.use(bodyParser.urlencoded());
var DB = require('./mysql_config.js');


var async = require('async');

var dbResults;
var items = {
    START_DATE: 0,
    END_DATE: 1,
    NUM_TOTAL: 2,
    NUM_THIS_YEAR: 3,
    NUM_EARLY_CT: 4,
    NUM_IIT: 5,
    BY_YEAR: 6,
    BY_PHASE: 7,
    BY_TYPE: 8,
    BY_PHASE_PER_YEAR: 9,
    BY_TYPE_PER_YEAR: 10,
    START_DATE_SITE:11,
    END_DATE_SITE:12,
    BY_YEAR_BY_PHASE:13
};
var itemArr = [];
function numberWithCommas(x) {
    return x.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ",");
 }

async.parallel([
        function (callback) { //0: START_DATE
            DB.query("SELECT DATE_FORMAT(approval_date, '%Y-%m-%d') as value FROM clinicaltrial ORDER BY approval_date ASC LIMIT 1", null, function (error, result) {
              
                callback(null, result);
            });
        },
        function (callback) { //1: END_DATE
            DB.query("select DATE_FORMAT(approval_date, '%Y-%m-%d') as value from clinicaltrial  ORDER BY approval_date DESC LIMIT 1", null, function (error, result) {

                callback(null, result);
            });
        },
        function (callback) { //2: NUM_TOTAL
            DB.query("SELECT count(*)  as value FROM clinicaltrial", null, function (error, result) {
       
              result[0].value=numberWithCommas(result[0].value);
               
                callback(null, result);
            });
        },
        function (callback) { //3: NUM_THIS_YEAR
            DB.query("SELECT COUNT(*) as value FROM clinicaltrial WHERE YEAR(approval_date) = YEAR(CURDATE())", null, function (error, result) {
               result[0].value=numberWithCommas(result[0].value);
                callback(null, result);
            });
        },
        function (callback) { //4 NUM_EARLY_CT
            DB.query("select count(*) as value from clinicaltrial where  phase ='0상' OR phase like '%1%' OR (phase like '%2%' AND NOT phase like '%2b%')", null, function (error, result) {
               result[0].value=numberWithCommas(result[0].value);
                callback(null, result);
            });
        },
        function (callback) { //5 NUM_IIT
            DB.query("select count(*) as value from clinicaltrial  where phase='연구자 임상시험'", null, function (error, result) {
              result[0].value=numberWithCommas(result[0].value);
                callback(null, result);
            });
        },
        function (callback) { //6 BY_YEAR
            DB.query("select YEAR(approval_date) as x , count(*) as y  from clinicaltrial GROUP by YEAR(approval_date)", null, function (error, result) {
                callback(null, result);
            });
        },
        function (callback) { //7 BY_PHASE
            // DB.query("select phase as x, count(*) as y from clinicaltrial group by phase order by phase", null, function (error, result) {
            DB.query("select phase as x, count(*) as y from clinicaltrial  GROUP BY phase order by  phase is null, FIELD(x, '0상',  '1b상','1상','1/2a상' ,'1/2상', '1/3상','2상', '2a상','2b상', '2b/3상','2/3상','3a상','3b상', '3상','3/4상','4상','연장','연구자 임상시험','생동')", null, function (error, result) {

                callback(null, result);
            });
        },
        function (callback) { //8 BY_TYPE
            DB.query("select type as x, count(*) as y from clinicaltrial  GROUP BY type order by type DESC", null, function (error, result) {

                callback(null, result);
            });
        },
        function (callback) { //9   BY_PHASE_PER_YEAR

            DB.query("select phase,  YEAR (approval_date) as year, count(*) as y from clinicaltrial  group by phase,year order by phase,year", null, function (error, result) {
                callback(null, result);
            });
        },
        function (callback) { //10   BY_TYPE_PER_YEAR

            DB.query("select type, YEAR (approval_date) as year, count(*) as y from clinicaltrial  group by type,year order by type DESC , year ASC ", null, function (error, result) {
                callback(null, result);
            });
        },
        function (callback) { //11: START_DATE_SITE
            DB.query("SELECT DATE_FORMAT(approval_date, '%Y-%m-%d') as value FROM VIEW_CT_SITECT_LEAD ORDER BY approval_date ASC LIMIT 1", null, function (error, result) {
              
                callback(null, result);
            });
        },
        function (callback) { //12: END_DATE_SITE
            DB.query("select DATE_FORMAT(approval_date, '%Y-%m-%d') as value from VIEW_CT_SITECT_LEAD  ORDER BY approval_date DESC LIMIT 1", null, function (error, result) {

                callback(null, result);
            });
        },
        function (callback) { //13: BY_YEAR_BY_PHASE
            DB.query("select YEAR(approval_date) as x , count(*) as y, phase   from clinicaltrial GROUP by YEAR(approval_date), phase", null, function (error, result) {

                callback(null, result);
            });
        }
    ],
    function (err, results) {
        dbResults = results;
        // console.log(dbResults[items.START_DATE][0] + '   dbResults[items.BY_YEAR]===>' + dbResults[items.START_DATE][0].value);
        // console.log(dbResults.length + '   dbResults[items.BY_YEAR]===>' + JSON.stringify(dbResults[items.BY_YEAR]));
        // console.log(dbResults.length + '   dbResults[items.BY_PHASe]===>' + JSON.stringify(dbResults[items.BY_PHASE]));

//console.log(dbResults.length + '   dbResults[items.BY_TYPE_PER_YEAR]===>' + JSON.stringify(dbResults[items.BY_TYPE_PER_YEAR]));
    }
);

app.get('/', function (req, res) {
    res.render('index', {
        // startDate: dbResults[items.START_DATE][0].value,
        // endDate: dbResults[items.END_DATE][0].value,
        // total: dbResults[items.NUM_TOTAL][0].value,
        // thisYear: dbResults[items.NUM_THIS_YEAR][0].value,
        // earlyCT: dbResults[items.NUM_EARLY_CT][0].value,
        // IIT: dbResults[items.NUM_IIT][0].value,
        // byYear: dbResults[items.BY_YEAR],
        // lastUpdate:fileUpdateDateTime
    });

});
app.get('/about.html', function (req, res) {
    res.render('us', {
           });

});
app.get('/summary.html', function (req, res) {
    res.render('summary', {
        startDate: dbResults[items.START_DATE][0].value,
        endDate: dbResults[items.END_DATE][0].value,
        total: dbResults[items.NUM_TOTAL][0].value,
        thisYear: dbResults[items.NUM_THIS_YEAR][0].value,
        earlyCT: dbResults[items.NUM_EARLY_CT][0].value,
        IIT: dbResults[items.NUM_IIT][0].value,
        byYear: dbResults[items.BY_YEAR],
        lastUpdate:fileUpdateDateTime,
        byYearByPhase:dbResults[items.BY_YEAR_BY_PHASE]
    });

});
app.get('/phase.html', function (req, res) {
    res.render('phase', {
        startDate: dbResults[items.START_DATE][0].value,
        endDate: dbResults[items.END_DATE][0].value,
        total: dbResults[items.NUM_TOTAL][0].value,
        lastUpdate:fileUpdateDateTime,

        byPhase: dbResults[items.BY_PHASE],

        byPhasePerYear: dbResults[items.BY_PHASE_PER_YEAR]


    });

});
app.get('/type.html', function (req, res) {
    res.render('type', {
        startDate: dbResults[items.START_DATE][0].value,
        endDate: dbResults[items.END_DATE][0].value,
        total: dbResults[items.NUM_TOTAL][0].value,
        lastUpdate:fileUpdateDateTime,

        byType: dbResults[items.BY_TYPE],

        byTypePerYear: dbResults[items.BY_TYPE_PER_YEAR]

    });

});
/////////////////////////////////////////////////////////////////////////////////
var allResults;
var viewItems = {
    NUM_SITE: 0,
    BY_SITE: 1,
    NUM_PHASE1: 2,
    BY_PHASE1: 3,
    NUM_PHASE1_HEALTHY: 4,
    BY_PHASE1_HEALTHY: 5,
    NUM_LOCATION: 6,
    BY_LOCATION: 7,
    BY_YEAR_BY_SITE:8,
    BY_SITE_BY_YEAR:9,
    NUM_PHASE2: 10,
    BY_PHASE2: 11,
    NUM_PHASE3: 12,
    BY_PHASE3: 13,
    NUM_BE: 14,//생동
    BY_BE: 15,//생동
    NUM_IIT: 16,
    BY_IIT: 17,
    BY_LOCATION_BY_YEAR:18, //지역별 연도별
    BY_PHASE1_BY_YEAR :19,
    BY_PHASE1_HEALTHY_BY_YEAR:20,
    BY_PHASE2_BY_YEAR :21,
    BY_PHASE3_BY_YEAR :22,
    BY_BE_BY_YEAR :23,
    BY_IIT_BY_YEAR :24  
};

async.parallel([ /// 시험책임자기준(allsites)  pi.ejs
        function (callback) { //0 NUM_SITE  시험책임자기준(allsites) : 전체 임상시험 수
            DB.query("select count(*) as value from VIEW_CT_SITECT_ALL", null, function (error, result) {
           //result[0].value=numberWithCommas(result[0].value);
                callback(null, result);
            });
        },

        function (callback) { //1 BY_SITE   시험책임자기준(allsites) : 병원별 전체 임상시험  
            DB.query("select count(*) as y, site_name as x, site_name from VIEW_CT_SITECT_ALL group by site_name order by y desc, site_name asc LIMIT 30", null, function (error, result) {
                callback(null, result);
            });
        },
        function (callback) { //2 NUM_PHASE1  시험책임자기준(allsites) : 병원별 1상 임상시험 수 
            DB.query("select count(*) as value from VIEW_CT_SITECT_ALL where  phase like '%1%'", null, function (error, result) {
               // result[0].value=numberWithCommas(result[0].value);
                callback(null, result);
            });
        },
        function (callback) { //3 BY_PHASE1   시험책임자기준(allsites) :  병원별 1상 임상시험 
            DB.query("select count(*) as y, site_name as x  from VIEW_CT_SITECT_ALL where  phase like '%1%'  group by site_name order by y desc, site_name asc LIMIT 30", null, function (error, result) {
                callback(null, result);
            });
        },
        function (callback) { //4  NUM_PHASE1_HEALTHY  시험책임자기준(allsites) : 병원별 1상 중 건강한 자원자 임상시험 수
            DB.query("select count(*) as value  from VIEW_CT_SITECT_ALL where  title like '%건강%'  and  phase like '%1%'", null, function (error, result) {
               // result[0].value=numberWithCommas(result[0].value);
                callback(null, result);
            });
        },
        function (callback) { //5 BY_PHASE1_HEALTHY:  시험책임자기준(allsites) 병원별 1상 중 건강한 자원자 임상시험  
            DB.query("select count(*) as y, site_name as x  from VIEW_CT_SITECT_ALL where  title like '%건강%' and  phase like '%1%'  group by site_name order by y desc, site_name asc LIMIT 30", null, function (error, result) {
                callback(null, result);
            });
        },
        function (callback) { //6 NUM_LOCATION  시험책임자기준(allsites) : 지역별 전체 임상시험  임상시험 수
            DB.query("select count(*) as value from VIEW_CT_SITECT_ALL where  location is not null ", null, function (error, result) {
                //result[0].value=numberWithCommas(result[0].value);
                callback(null, result);
            });
        },
        function (callback) { //7 BY_LOCATION   시험책임자기준(allsites) : 지역별 전체 임상시험 
            DB.query("select count(*) as y, location as x from VIEW_CT_SITECT_ALL  where  location is not null group by location order by y desc", null, function (error, result) {
                //console.log('  result[1]==>' + result.length);
                //console.log(' result[1]===>' + JSON.stringify(result));
                callback(null, result);
            });
        },
        function (callback) { //8 BY_YEAR_BY_SITE   시험책임자기준(allsites) : 연도별 병원별 전체 임상시험 
            DB.query("select count(*) as y ,site_name as x, YEAR(approval_date) as id from  VIEW_CT_SITECT_ALL where YEAR(approval_date)  in (SELECT distinct(YEAR(approval_date) )as x FROM (select *  from site_ct )as tbs) group by id,x order by id,y desc", null, function (error, result) {
                //console.log('  result[1]==>' + result.length);
                callback(null, result);
            });
        },
        function (callback) { //9 BY_SITE_BY_YEAR   시험책임자기준(allsites) : 병원별 연도별 전체 임상시험 
            DB.query(" SELECT count(*) as y ,site_name as id, YEAR(approval_date) as x from  VIEW_CT_SITECT_ALL where site_name in  (SELECT site_name  from (select site_name from VIEW_CT_SITECT_ALL group by site_name order by count(*) desc, site_name asc LIMIT 30 ) as b) group by id,x order by id,x asc", null, function (error, result) {
                callback(null, result);
            });
        },
        function (callback) { //10 NUM_PHASE2  시험책임자기준(allsites) : 병원별 2상 임상시험 수 
            DB.query("select count(*) as value from VIEW_CT_SITECT_ALL where  phase like '%2%'", null, function (error, result) {
                //result[0].value=numberWithCommas(result[0].value);
                callback(null, result);
            });
        },
        function (callback) { //11 BY_PHASE2  시험책임자기준(allsites) :  병원별 2상 임상시험 
            DB.query("select count(*) as y, site_name as x  from VIEW_CT_SITECT_ALL where  phase like '%2%'  group by site_name order by y desc, site_name asc LIMIT 30", null, function (error, result) {
                callback(null, result);
            });
        },
        function (callback) { //12 NUM_PHASE3 시험책임자기준(allsites) : 병원별 3상 임상시험 수 
            DB.query("select count(*) as value from VIEW_CT_SITECT_ALL where  phase like '%3%'", null, function (error, result) {
                //result[0].value=numberWithCommas(result[0].value);
                callback(null, result);
            });
        },
        function (callback) { //13 BY_PHASE3   시험책임자기준(allsites):  병원별 3상 임상시험 
            DB.query("select count(*) as y, site_name as x  from VIEW_CT_SITECT_ALL where  phase like '%3%'  group by site_name order by y desc, site_name asc LIMIT 30", null, function (error, result) {
                callback(null, result);
            });
        },
        function (callback) { //14 NUM_BE 시험책임자기준(allsites) : 병원별 생동 임상시험 수 
            DB.query("select count(*) as value from VIEW_CT_SITECT_ALL where  phase = '생동'", null, function (error, result) {
               // result[0].value=numberWithCommas(result[0].value);
                callback(null, result);
            });
        },
        function (callback) { //15 BY_BE   시험책임자기준(allsites):  병원별 생동 임상시험 
            DB.query("select count(*) as y, site_name as x  from VIEW_CT_SITECT_ALL where  phase = '생동'  group by site_name order by y desc, site_name asc LIMIT 30", null, function (error, result) {
              
             
                callback(null, result);
            });
        },
        function (callback) { //16 NUM_IIT 시험책임자기준(allsites) : 병원별 IIT 임상시험 수 
            DB.query("select count(*) as value from VIEW_CT_SITECT_ALL where  phase = '연구자 임상시험'", null, function (error, result) {
               // result[0].value=numberWithCommas(result[0].value);
                callback(null, result);
            });
        },
        function (callback) { //17 BY_IIT   시험책임자기준(allsites):  병원별 IIT 생동 임상시험 
            DB.query("select count(*) as y, site_name as x  from VIEW_CT_SITECT_ALL where  phase = '연구자 임상시험'  group by site_name order by y desc, site_name asc LIMIT 30", null, function (error, result) {
              
             
                callback(null, result);
            });
        },
        function (callback) { //18 BY_LOCATION_BY_YEAR   시험책임자기준(allsites) : 지역별 연도별 전체 임상시험 
            DB.query("select count(*) as y ,location as id, YEAR(approval_date) as x from  VIEW_CT_SITECT_ALL where YEAR(approval_date)  in (SELECT distinct(YEAR(approval_date) ) FROM (select *  from site_ct )as tbs) group by id,x order by id,y desc", null, function (error, result) {
                //console.log('  result[1]==>' + result.length);
                //console.log(' result[1]===>' + JSON.stringify(result));
                callback(null, result);
            });
        },
        function (callback) { //19 BY_PHASE1_BY_YEAR   시험책임자기준(allsites) : Phase1 연도별 전체 임상시험 
            DB.query("SELECT count(*) as y ,site_name as id, YEAR(approval_date) as x from  VIEW_CT_SITECT_ALL where  phase like '%1%'  and site_name in  (SELECT site_name  from (select site_name from VIEW_CT_SITECT_ALL where  phase like '%1%' group by site_name order by count(*) desc, site_name asc LIMIT 30 ) as b)  group by id,x order by id,x asc", null, function (error, result) {
                //console.log('  result[1]==>' + result.length);
                //console.log(' result[1]===>' + JSON.stringify(result));
                callback(null, result);
            });
        },
        function (callback) { //20  BY_PHASE1_HEALTHY_BY_YEAR   시험책임자기준(allsites) : phase1 건강한 자원자 연도별 전체 임상시험 
            DB.query("SELECT count(*) as y ,site_name as id, YEAR(approval_date) as x from  VIEW_CT_SITECT_ALL where  phase like '%1%' and title like '%건강%'  and site_name in  (SELECT site_name  from (select site_name from VIEW_CT_SITECT_ALL where  phase like '%1%' and title like '%건강%'  group by site_name order by count(*) desc, site_name asc LIMIT 30 ) as b)  group by id,x order by id, site_name asc", null, function (error, result) {
                //console.log('  result[1]==>' + result.length);
                //console.log(' result[1]===>' + JSON.stringify(result));
                callback(null, result);
            });
        },
        function (callback) { //21 BY_PHASE2_BY_YEAR   시험책임자기준(allsites) : Phase2 연도별 전체 임상시험
            DB.query("SELECT count(*) as y ,site_name as id, YEAR(approval_date) as x from  VIEW_CT_SITECT_ALL where  phase like '%2%'  and site_name in  (SELECT site_name  from (select site_name from VIEW_CT_SITECT_ALL where  phase like '%2%' group by site_name order by count(*) desc, site_name asc LIMIT 30 ) as b)  group by id,x order by id,x asc", null, function (error, result) {
                //console.log('  result[1]==>' + result.length);
                //console.log(' result[1]===>' + JSON.stringify(result));
                callback(null, result);
            });
        },
        function (callback) { //22  BY_PHASE3_BY_YEAR   시험책임자기준(allsites) : Phase3 연도별 전체 임상시험
            DB.query("SELECT count(*) as y ,site_name as id, YEAR(approval_date) as x from  VIEW_CT_SITECT_ALL where  phase like '%3%'  and site_name in  (SELECT site_name  from (select site_name from VIEW_CT_SITECT_ALL where  phase like '%3%' group by site_name order by count(*) desc, site_name asc LIMIT 30 ) as b)  group by id,x order by id,x asc", null, function (error, result) {
                //console.log('  result[1]==>' + result.length);
                //console.log(' result[1]===>' + JSON.stringify(result));
                callback(null, result);
            });
        },
        function (callback) { //23 BY_BE_BY_YEAR   시험책임자기준(allsites) : 생동 연도별 전체 임상시험 
            DB.query("SELECT count(*) as y ,site_name as id, YEAR(approval_date) as x from  VIEW_CT_SITECT_ALL where  phase = '생동'  and site_name in  (SELECT site_name  from (select site_name from VIEW_CT_SITECT_ALL where  phase = '생동' group by site_name order by count(*) desc, site_name asc LIMIT 30 ) as b)  group by id,x order by id,x asc", null, function (error, result) {
                //console.log('  result[1]==>' + result.length);
                //console.log(' result[1]===>' + JSON.stringify(result));
                callback(null, result);
            });
        },
        function (callback) { //24 BY_IIT_BY_YEAR   시험책임자기준(allsites) : 연구자 연도별 전체 임상시험 
            DB.query("SELECT count(*) as y ,site_name as id, YEAR(approval_date) as x from  VIEW_CT_SITECT_ALL where   phase = '연구자 임상시험'  and site_name in  (SELECT site_name  from (select site_name from VIEW_CT_SITECT_ALL where   phase = '연구자 임상시험' group by site_name order by count(*) desc, site_name asc LIMIT 30 ) as b)  group by id,x order by id,x asc", null, function (error, result) {
                //console.log('  result[1]==>' + result.length);
                //console.log(' result[1]===>' + JSON.stringify(result));
                callback(null, result);
            });
        }

    ],
    function (err, results) {
        allResults = results;
// console.log( '   allResults[allItems.BY_YEAR_BY_SITE]===>' + JSON.stringify(allResults[viewItems.BY_SITE_BY_YEAR]));
    // console.log( '   allResults[allItems.BY_PHASE1]===>' + JSON.stringify(allResults[viewItems.BY_PHASE1]));
    // console.log( '   allResults[allItems.BY_PHASE1_HEALTHY]===>' + JSON.stringify(allResults[viewItems.BY_PHASE1_HEALTHY]));
       //  console.log( '   allResults[allItems.START_DATE]===>' + allResults[viewItems.START_DATE][0].value);
        //console.log(viewResults.length + '   viewItems[viewItems.BY_SITE_OF_ALL]===>' + JSON.stringify(viewResults[viewItems.BY_SITE_OF_ALL]));
      //onsole.log(allResults.length + '    allResults[allItems.BY_SITE]===>' + JSON.stringify(allResults[viewItems.BY_SITE]));
    }
);



app.get('/pi.html', function (req, res) {
    res.render('pi', {
        startDate: dbResults[items.START_DATE][0].value,
        endDate: dbResults[items.END_DATE][0].value,
        total: dbResults[items.NUM_TOTAL][0].value,
        lastUpdate:fileUpdateDateTime,
        startDateSite: dbResults[items.START_DATE_SITE][0].value,
        endDateSite: dbResults[items.END_DATE_SITE][0].value,
       
        endDateSite2: dbResults[items.END_DATE_SITE][0].value.toString(),
        numSiteOfAll: allResults[viewItems.NUM_SITE][0].value,
        bySiteOfAll: allResults[viewItems.BY_SITE],
        numPhase1OfAll: allResults[viewItems.NUM_PHASE1][0].value,
        byPhase1OfAll: allResults[viewItems.BY_PHASE1],

        numPhase1HealthyOfAll: allResults[viewItems.NUM_PHASE1_HEALTHY][0].value,
        byPhase1HealthyOfAll: allResults[viewItems.BY_PHASE1_HEALTHY],
        numLocationOfAll: allResults[viewItems.NUM_LOCATION][0].value,
        byLocationOfAll: allResults[viewItems.BY_LOCATION],
        byYearBySite: allResults[viewItems.BY_YEAR_BY_SITE],
        bySiteByYear: allResults[viewItems.BY_SITE_BY_YEAR],

        numPhase2OfAll: allResults[viewItems.NUM_PHASE2][0].value,
        byPhase2OfAll: allResults[viewItems.BY_PHASE2],
        numPhase3OfAll: allResults[viewItems.NUM_PHASE3][0].value,
        byPhase3OfAll: allResults[viewItems.BY_PHASE3],
        numBEOfAll: allResults[viewItems.NUM_BE][0].value,
        byBEOfAll: allResults[viewItems.BY_BE],
        numIITOfAll: allResults[viewItems.NUM_IIT][0].value,
        byIITOfAll: allResults[viewItems.BY_IIT],
        byLocationByYearOfAll: allResults[viewItems.BY_LOCATION_BY_YEAR],
        byPhase1ByYearOfAll: allResults[viewItems.BY_PHASE1_BY_YEAR],
        byPhase1HealthyByYearOfAll: allResults[viewItems.BY_PHASE1_HEALTHY_BY_YEAR],
        byPhase2ByYearOfAll: allResults[viewItems.BY_PHASE2_BY_YEAR],
        byPhase3ByYearOfAll: allResults[viewItems.BY_PHASE3_BY_YEAR],
        byBEByYearOfAll: allResults[viewItems.BY_BE_BY_YEAR],
        byIITByYearOfAll: allResults[viewItems.BY_IIT_BY_YEAR]
    
    });

});
/////////////////////////////////////////////////////////////////////////////////
var leadResults;
async.parallel([ ///시험조정자기준(lead)  ci.ejs
        function (callback) { //0 NUM_SITE  시험조정자기준(lead) : 전체 임상시험 수
            DB.query("select count(*) as value from VIEW_CT_SITECT_LEAD ", null, function (error, result) {
                //result[0].value=numberWithCommas(result[0].value);
                callback(null, result);
            });
        },

        function (callback) { //1 BY_SITE   시험조정자기준(lead) : 병원별 전체 임상시험 
            DB.query("select count(*) as y, site_name as x  from VIEW_CT_SITECT_LEAD group by site_name order by y desc, site_name asc LIMIT 30", null, function (error, result) {
                callback(null, result);
            });
        },
        function (callback) { //2 NUM_PHASE1  시험조정자기준(lead) : 병원별 1상 임상시험 수 
            DB.query("select count(*) as value from VIEW_CT_SITECT_LEAD where  phase like '%1%'", null, function (error, result) {
               // result[0].value=numberWithCommas(result[0].value);
                callback(null, result);
            });
        },
        function (callback) { //3 BY_PHASE1   시험조정자기준(lead) :  병원별 1상 임상시험 
            DB.query("select count(*) as y, site_name as x  from VIEW_CT_SITECT_LEAD where  phase like '%1%'  group by site_name order by y desc , site_name asc LIMIT 30", null, function (error, result) {
                callback(null, result);
            });
        },
        function (callback) { //4  NUM_PHASE1_HEALTHY  시험조정자기준(lead) : 병원별 1상 중 건강한 자원자 임상시험 수
            DB.query("select count(*) as value from VIEW_CT_SITECT_LEAD where  title like '%건강%'  and  phase like '%1%'", null, function (error, result) {
                //result[0].value=numberWithCommas(result[0].value);
                callback(null, result);
            });
        },
        function (callback) { //5 BY_PHASE1_HEALTHY:  시험조정자기준(lead)병원별 1상 중 건강한 자원자 임상시험  
            DB.query("select count(*) as y, site_name as x from VIEW_CT_SITECT_LEAD where  title like '%건강%' and  phase like '%1%'  group by site_name order by y desc , site_name asc LIMIT 30", null, function (error, result) {
                callback(null, result);
            });
        },
        function (callback) { //6 NUM_LOCATION  시험조정자기준(lead) : 지역별 전체 임상시험  임상시험 수
            DB.query("select count(*) as value from VIEW_CT_SITECT_LEAD where  location is not null ", null, function (error, result) {
               // result[0].value=numberWithCommas(result[0].value);
                callback(null, result);
            });
        },
        function (callback) { //7 BY_LOCATIONL   시험조정자기준(lead) : 지역별 전체 임상시험 
            DB.query("select count(*) as y, location as x from VIEW_CT_SITECT_LEAD  where  location is not null group by location order by y desc", null, function (error, result) {
                //console.log('  result[1]==>' + result.length);
                callback(null, result);
            });
        },
        function (callback) { //8 BY_YEAR_BY_SITE   시험조정자기준(lead) : 연도별 병원별 전체 임상시험 
            DB.query("select count(*) as y ,site_name as x, YEAR(approval_date) as id from  VIEW_CT_SITECT_LEAD where YEAR(approval_date)  in (SELECT distinct(YEAR(approval_date) )as x FROM (select *  from site_ct )as tbs) group by id,x order by id,y desc", null, function (error, result) {
                //console.log('  result[1]==>' + result.length);
                callback(null, result);
            });
        },
        function (callback) { //9 BY_SITE_BY_YEAR   시험조정자기준(lead) : 병원별 연도별 전체 임상시험 
            DB.query(" SELECT count(*) as y ,site_name as id, YEAR(approval_date) as x from  VIEW_CT_SITECT_LEAD where site_name in  (SELECT site_name  from (select site_name from VIEW_CT_SITECT_LEAD group by site_name order by count(*) desc, site_name LIMIT 30 ) as b) group by id,x order by id,x asc", null, function (error, result) {
                callback(null, result);
            });
        },
        function (callback) { //10 NUM_PHASE2  시험조정자기준(lead) : 병원별 2상 임상시험 수 
            DB.query("select count(*) as value from VIEW_CT_SITECT_LEAD where  phase like '%2%'", null, function (error, result) {
               // result[0].value=numberWithCommas(result[0].value);
                callback(null, result);
            });
        },
        function (callback) { //113 BY_PHASE2  시험조정자기준(lead) :  병원별 2상 임상시험 
            DB.query("select count(*) as y, site_name as x  from VIEW_CT_SITECT_LEAD where  phase like '%2%'  group by site_name order by y desc , site_name asc LIMIT 30", null, function (error, result) {
                callback(null, result);
            });
        },
        function (callback) { //12 NUM_PHASE3  시험조정자기준(lead) : 병원별 3상 임상시험 수 
            DB.query("select count(*) as value from VIEW_CT_SITECT_LEAD where  phase like '%3%'", null, function (error, result) {
                //result[0].value=numberWithCommas(result[0].value);
                callback(null, result);
            });
        },
        function (callback) { //13 BY_PHASE3   시험조정자기준(lead) :  병원별 3상 임상시험 
            DB.query("select count(*) as y, site_name as x  from VIEW_CT_SITECT_LEAD where  phase like '%3%'  group by site_name order by y desc, site_name asc LIMIT 30", null, function (error, result) {
                callback(null, result);
            });
        },
        function (callback) { //14 NUM_BE 시험조정자기준(lead) : 병원별 생동 임상시험 수 
            DB.query("select count(*) as value from VIEW_CT_SITECT_LEAD where  phase = '생동'", null, function (error, result) {
                //result[0].value=numberWithCommas(result[0].value);
                callback(null, result);
            });
        },
        function (callback) { //15 BY_BE   시험조정자기준(lead):  병원별 생동 임상시험 
            DB.query("select count(*) as y, site_name as x  from VIEW_CT_SITECT_LEAD where  phase = '생동'  group by site_name order by y desc, site_name asc LIMIT 30", null, function (error, result) {
              
                callback(null, result);
            });
        } ,    function (callback) { //16 NUM_IIT  시험조정자기준(lead):  병원별 IIT 임상시험 수 
            DB.query("select count(*) as value from VIEW_CT_SITECT_LEAD where  phase = '연구자 임상시험'", null, function (error, result) {
               // result[0].value=numberWithCommas(result[0].value);
                callback(null, result);
            });
        },
        function (callback) { //17 BY_IIT    시험조정자기준(lead):   병원별 IIT 생동 임상시험 
            DB.query("select count(*) as y, site_name as x  from VIEW_CT_SITECT_LEAD where  phase = '연구자 임상시험'  group by site_name order by y desc , site_name asc LIMIT 30", null, function (error, result) {
              
             
                callback(null, result);
            });
        },
        function (callback) { //18 BY_LOCATION_BY_YEAR  시험조정자기준(lead): 지역별 연도별 전체 임상시험 
            DB.query("select count(*) as y ,location as id, YEAR(approval_date) as x from  VIEW_CT_SITECT_LEAD where YEAR(approval_date)  in (SELECT distinct(YEAR(approval_date) ) FROM (select *  from site_ct )as tbs) group by id,x order by id,y desc", null, function (error, result) {
                
                callback(null, result);
            });
        },
        function (callback) { //19 BY_PHASE1_BY_YEAR   시험조정자기준(lead): Phase1 연도별 전체 임상시험 
            DB.query("SELECT count(*) as y ,site_name as id, YEAR(approval_date) as x from  VIEW_CT_SITECT_LEAD where  phase like '%1%'  and site_name in  (SELECT site_name  from (select site_name from VIEW_CT_SITECT_LEAD where  phase like '%1%' group by site_name order by count(*) desc, site_name asc LIMIT 30 ) as b)  group by id,x order by id,x asc", null, function (error, result) {
                //console.log('  result[1]==>' + result.length);
                //console.log(' result[1]===>' + JSON.stringify(result));
                callback(null, result);
            });
        },
        function (callback) { //20  BY_PHASE1_HEALTHY_BY_YEAR   시험조정자기준(lead) : phase1 건강한 자원자 연도별 전체 임상시험 
            DB.query("SELECT count(*) as y ,site_name as id, YEAR(approval_date) as x from  VIEW_CT_SITECT_LEAD where  phase like '%1%' and title like '%건강%'  and site_name in  (SELECT site_name  from (select site_name from VIEW_CT_SITECT_LEAD where  phase like '%1%' and title like '%건강%'  group by site_name order by count(*) desc, site_name asc LIMIT 30 ) as b)  group by id,x order by id, site_name asc", null, function (error, result) {
                //console.log('  result[1]==>' + result.length);
                //console.log(' result[1]===>' + JSON.stringify(result));
                callback(null, result);
            });
        },
        function (callback) { //21 BY_PHASE2_BY_YEAR   시험조정자기준(lead): Phase2 연도별 전체 임상시험
            DB.query("SELECT count(*) as y ,site_name as id, YEAR(approval_date) as x from  VIEW_CT_SITECT_LEAD where  phase like '%2%'  and site_name in  (SELECT site_name  from (select site_name from VIEW_CT_SITECT_LEAD where  phase like '%2%' group by site_name order by count(*) desc, site_name asc LIMIT 30 ) as b)  group by id,x order by id,x asc", null, function (error, result) {
                //console.log('  result[1]==>' + result.length);
                //console.log(' result[1]===>' + JSON.stringify(result));
                callback(null, result);
            });
        },
        function (callback) { //22  BY_PHASE3_BY_YEAR   시험조정자기준(lead) : Phase3 연도별 전체 임상시험
            DB.query("SELECT count(*) as y ,site_name as id, YEAR(approval_date) as x from  VIEW_CT_SITECT_LEAD where  phase like '%3%'  and site_name in  (SELECT site_name  from (select site_name from VIEW_CT_SITECT_LEAD where  phase like '%3%' group by site_name order by count(*) desc, site_name asc LIMIT 30 ) as b)  group by id,x order by id,x asc", null, function (error, result) {
                //console.log('  result[1]==>' + result.length);
                //console.log(' result[1]===>' + JSON.stringify(result));
                callback(null, result);
            });
        },
        function (callback) { //23 BY_BE_BY_YEAR  시험조정자기준(lead) : 생동 연도별 전체 임상시험 
            DB.query("SELECT count(*) as y ,site_name as id, YEAR(approval_date) as x from  VIEW_CT_SITECT_LEAD where  phase = '생동'  and site_name in  (SELECT site_name  from (select site_name from VIEW_CT_SITECT_LEAD where  phase = '생동' group by site_name order by count(*) desc, site_name asc LIMIT 30 ) as b)  group by id,x order by id,x asc", null, function (error, result) {
                //console.log('  result[1]==>' + result.length);
                //console.log(' result[1]===>' + JSON.stringify(result));
                callback(null, result);
            });
        },
        function (callback) { //24 BY_IIT_BY_YEAR   시험조정자기준(lead): 연구자 연도별 전체 임상시험 
            DB.query("SELECT count(*) as y ,site_name as id, YEAR(approval_date) as x from  VIEW_CT_SITECT_LEAD where   phase = '연구자 임상시험'  and site_name in  (SELECT site_name  from (select site_name from VIEW_CT_SITECT_LEAD where   phase = '연구자 임상시험' group by site_name order by count(*) desc, site_name asc LIMIT 30 ) as b)  group by id,x order by id,x asc", null, function (error, result) {
                //console.log('  result[1]==>' + result.length);
                //console.log(' result[1]===>' + JSON.stringify(result));
                callback(null, result);
            });
        }


        
    ],
    function (err, results) {
        leadResults = results;

  //  console.log( '   leadResults[viewItems.BY_PHASE3]===>' + JSON.stringify(leadResults[viewItems.BY_PHASE3]));
  //  console.log( '   leadResults[viewItems.NUM_PHASE3]===>' + JSON.stringify(leadResults[viewItems.NUM_PHASE3]));
    //  console.log( '   leadResults[viewItems.NUM_SITE]===>' + JSON.stringify(leadResults[viewItems.NUM_SITE]));
     // console.log(viewResults.length + '   viewItems[viewItems.BY_SITE_OF_ALL]===>' + JSON.stringify(viewResults[viewItems.BY_SITE_OF_ALL]));
 //console.log(leadResults.length + '    allResults[viewItems.BY_YEAR_SITE]===>' + JSON.stringify(leadResults[viewItems.BY_YEAR_SITE]));
 //console.log(leadResults.length + '    allResults[viewItems.BY_YEAR_BY_SITE]===>' + JSON.stringify(leadResults[viewItems.BY_YEAR_BY_SITE]));
    }
);



app.get('/ci.html', function (req, res) {
    res.render('ci', {
        startDate: dbResults[items.START_DATE][0].value,
        endDate: dbResults[items.END_DATE][0].value,
        total: dbResults[items.NUM_TOTAL][0].value,
        lastUpdate:fileUpdateDateTime,
        startDateSite: dbResults[items.START_DATE_SITE][0].value,
        endDateSite: dbResults[items.END_DATE_SITE][0].value,

        numSiteOfLead: leadResults[viewItems.NUM_SITE][0].value,
        bySiteOfLead: leadResults[viewItems.BY_SITE],
        numPhase1OfLead: leadResults[viewItems.NUM_PHASE1][0].value,
        byPhase1OfLead: leadResults[viewItems.BY_PHASE1],

        numPhase1HealthyOfLead: leadResults[viewItems.NUM_PHASE1_HEALTHY][0].value,
        byPhase1HealthyOfLead: leadResults[viewItems.BY_PHASE1_HEALTHY],
        numLocationOfLead: leadResults[viewItems.NUM_LOCATION][0].value,
        byLocationOfLead: leadResults[viewItems.BY_LOCATION],
        byYearBySite: leadResults[viewItems.BY_YEAR_BY_SITE],
        bySiteByYear: leadResults[viewItems.BY_SITE_BY_YEAR],

        numPhase2OfLead: leadResults[viewItems.NUM_PHASE2][0].value,
        byPhase2OfLead: leadResults[viewItems.BY_PHASE2],
        numPhase3OfLead: leadResults[viewItems.NUM_PHASE3][0].value,
        byPhase3OfLead: leadResults[viewItems.BY_PHASE3],
        numBEOfLead: leadResults[viewItems.NUM_BE][0].value,
        byBEOfLead: leadResults[viewItems.BY_BE],
        numIITOfLead: leadResults[viewItems.NUM_IIT][0].value,
        byIITOfLead: leadResults[viewItems.BY_IIT],
        byLocationByYearOfLead: leadResults[viewItems.BY_LOCATION_BY_YEAR],
        byPhase1ByYearOfLead: leadResults[viewItems.BY_PHASE1_BY_YEAR],
        byPhase1HealthyByYearOfLead: leadResults[viewItems.BY_PHASE1_HEALTHY_BY_YEAR],
        byPhase2ByYearOfLead: leadResults[viewItems.BY_PHASE2_BY_YEAR],
        byPhase3ByYearOfLead: leadResults[viewItems.BY_PHASE3_BY_YEAR],
        byBEByYearOfLead: leadResults[viewItems.BY_BE_BY_YEAR],
        byIITByYearOfLead: leadResults[viewItems.BY_IIT_BY_YEAR]
        
    });

});
// var iniparser = require('iniparser');
// var config = iniparser.parseSync('./config/db.ini');
// var dbname=config['dbname'];


app.get('/download', function(req, res){
 

    var file ='./public/excelfiles/'+'clinicaltrialkr.xls';   
    //console.log(file);
    res.download(file); // Set disposition and send it.

  });

