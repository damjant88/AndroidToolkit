@echo off

set PACKAGE=%1
set DEVICE=%2

if "%PACKAGE%"=="" (
  echo Usage: setup_permissions.bat ^<package_name^> ^<device_id^>
  exit /b 1
)

if "%DEVICE%"=="" (
  echo Missing device ID
  exit /b 1
)

echo 📱 Working on %DEVICE%

adb -s %DEVICE% shell pm grant %PACKAGE% android.permission.ACCESS_FINE_LOCATION
adb -s %DEVICE% shell pm grant %PACKAGE% android.permission.ACCESS_COARSE_LOCATION
adb -s %DEVICE% shell pm grant %PACKAGE% android.permission.ACCESS_BACKGROUND_LOCATION
adb -s %DEVICE% shell pm grant %PACKAGE% android.permission.READ_PHONE_STATE
adb -s %DEVICE% shell pm grant %PACKAGE% android.permission.READ_PHONE_NUMBERS
adb -s %DEVICE% shell pm grant %PACKAGE% android.permission.POST_NOTIFICATIONS

adb -s %DEVICE% shell cmd deviceidle whitelist +%PACKAGE%
adb -s %DEVICE% shell cmd appops set %PACKAGE% AUTO_REVOKE_PERMISSIONS_IF_UNUSED ignore

echo ✅ Done!
pause