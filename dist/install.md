УСТАНОВКА:
+ 1) загрузить папку dbschema на сервер
   su plm
   /usr/PLM/start.sh mql
- 2) выполнить MQL команду: "exec prog MxUpdate --path ПУТЬ_К_ПАПКЕ_DBSCHEMA --update -a * --compile;"
+ 3) скопировать папку 3dspace в /usr/PLM/3DSpace/linux_a64/code/tomee/webapps/3dspace
+ 4) зарегистрировать виджеты в вебинтерфейсе(3ddashboard -> Platform Managment -> Members -> Create Additional App). Указать имя, тип=widget, url=https://3dspace-study.sw-tech.by:444/3dspace/webapps/ИМЯ_ВИДЖЕТА/index.html
- 5) обновить БЛОКИ(не весь файл) языковых переменных из папки locals в /usr/PLM/3DSpace/linux_a64/code/tomee/webapps/3dspace/WEB-INF/classes
   (WEB-INF/classes/Resource.properties)
+ 6) перезагрузить tomcat "/*su plm*/ /usr/PLM/start.sh recas"
- 7) поднять python и установить расширение xlsxwriter и положить скрипт рядом с файлом datalicweb_ts.txt с выгруженными данными о лицензиях с помощью утилиты DSLicSrv.exe 