echo off
set status="online"

:wait_loop

timeout 2

for /f "tokens=*" %%a in ('curl -G "https://3dspace-m001.sw-tech.by:444/internal/sw/ping" -s --write-out "%%{http_code}"') do set httpcode=%%a
echo %httpcode%
if %status%=="offline" IF %httpcode% == 200 goto restore_email
if %status%=="offline" IF %httpcode% == 302 goto restore_email

if %status%=="online" IF %httpcode% == 200 goto wait_loop
if %status%=="online" IF %httpcode% == 302 goto wait_loop

if %status%=="offline" goto wait_loop

echo %status%
.\sendEmail.exe -o tls=yes -f m.gaiduk@sw-tech.by -t m.gaiduk@sw-tech.by -s mail.sw-tech.by:25 -xu m.gaiduk@sw-tech.by -xp XXX -u Offline -m Offline
set status="offline"
goto wait_loop


:restore_email
echo %status%
.\sendEmail.exe -o tls=yes -f m.gaiduk@sw-tech.by -t m.gaiduk@sw-tech.by -s mail.sw-tech.by:25 -xu m.gaiduk@sw-tech.by -xp XXXX -u Online -m Online
set status="online"
goto wait_loop


