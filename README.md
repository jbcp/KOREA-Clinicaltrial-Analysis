# Clinicaltrial.kr
식약처에 등록된 임상시험 승인정보의 데이터를 정형화하여 분석한 사이트 참조 : www.clinicaltrial.kr 

## 주요 기능
+ 한국 임상시험 승인 현황을 파악할수 있다.

## Prerequisites
+ JAVA 1.8
+ nodejs 8.0

## Installation
+ ./bootstrap/config/db.properties 를 작성한다.
```
ID = [rds id]
PASSWD = [rds password]
rdsIP = [localhost / rds ip]
dbPort = [port]

```
+ go to ./bootstrap
```
npm install
cd ../src/
```
+ run java with libraries
```
sudo javac -cp ../library/*:. ./*.java
sudo java -cp ../library/*:. AnalysisCT
```

+ after ending previous processing, go to ./bootstrap folder 
+ run express server
```
node server.js 
```

## Pics
![Clinicaltrial.kr](/ct1.png)

## Contact to developer(s)
Jhyoung lee - jhlee@jbcp.kr
