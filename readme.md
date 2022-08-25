Бизнес логика: m.kim@unitsky.com
Вопросы для обсуждения:
Тяжелый легкий клиенты
Spaces
Права доступа
Процесс route
Процесс CA
Консоль MQL

Тестовый сервер: https://3dspace-m001.sw-tech.by:444/3dspace
Тестовый без CAS: https://3dspace-m001.sw-tech.by:444/internal
Версия: https://3dspace-m001.sw-tech.by:444/internal/sw/version

Прод: https://3dspace.sw-tech.by:444/3dspace
Прод без CAS: https://3dspace.sw-tech.by:444/internal
Версия: https://3dspace.sw-tech.by:444/internal/sw/version

Документация по 3DExperience http://3dehelp.sw-tech.by/
Важное: Installation and Setup | Customize | 3DEXPERIENCE Platform: 3DSpace | Legacy Tools

Фундаментальная документация по mql:
https://ft.igatec.com/pydio/mnt/data/public/f06c17

Установка новых виджетов: dist/install.md

Скрипт деплоя:
Сбилдить артифакт
Скопировать из dist\3dspace\WEB-INF\lib\SWTServices.jar
Залить скомпиленную jar в папку 3dspace\webapps\SWTDeploy\SWTServices.jar
Залить скомпиленную jar в папку 3dspace\webapps\SWTDeploy\NoCas\SWTServices.jar
Залить папку dbshema в папку 3dspace\webapps\SWTDeploy\dbshema
Залить папку 3dspace в папку 3dspace
Рестарт dashboard: скопировать пустой файл restart_dash в 3dspace\webapps\SWTDeploy\restart_dash
Компилирование скриптов: скопировать файл compile с текстом названия класса в 3dspace\webapps\SWTDeploy\compile

Дополнительные вопросы по настройке сервера и доступа: a.ustinovich@unitsky.com

Разработка велась с 10.10.38.10 и все прова на доступ к дебаг портам настроены на этот ip. Смена ip потребует переназначения всех сетевых правил