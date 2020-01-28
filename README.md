# clinicaltrial.kr
식약처에 등록된 임상시험 승인정보의 데이터를 정형화하여 분석한 사이트 참조 : www.clinicaltrial.kr 

## 주요 기능
+ 대상자를 그룹으로 분류하여 그룹별 방송 가능
+ 주기마다 다른 호출시간 설정가능
+ 스케쥴을 테이블에서 편집가능
+ 대상자 범위 100명까지 설정 가능

## Prerequisites
+ JAVA 1.8
+ nodejs 8.0

## Installation
+ ./bootstrap/config/db.properties 를 작성한다.
+ go to ./bootstrap
```
npm install
```
+ go to  ./src/
+ run java with libraries
```
sudo javac -cp ../library/*:. ./*.java
sudo java -cp ../library/*:. AnalysisCT
```

+ go to ./bootstrap folder 
+ run express server
```
node server.js 
```

## Pics
![Clinicaltrial.kr](/ct1.png)

## Contact to developer(s)
Jhyoung lee - jhlee@jbcp.kr
