@echo off

rem ZebroGaMQ: Communication Middleware for Mobile Gaming
rem Copyright: Copyright (C) 2009-2012
rem Contact: denis.conan@telecom-sudparis.eu, michel.simatic@telecom-sudparis.eu
rem
rem This library is free software; you can redistribute it and/or
rem modify it under the terms of the GNU Lesser General Public
rem License as published by the Free Software Foundation; either
rem version 3 of the License, or any later version.
rem
rem This library is distributed in the hope that it will be useful,
rem but WITHOUT ANY WARRANTY; without even the implied warranty of
rem MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
rem Lesser General Public License for more details.
rem
rem You should have received a copy of the GNU Lesser General Public
rem License along with this library; if not, write to the Free Software
rem Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307
rem USA
rem
rem Developer(s): Denis Conan, Gabriel Adgeg

setlocal
set PYTHONPATH=%PYTHONPATH%;%cd%\Termination-application\;%cd%\..\src\Python-server\

rem terminate
cd Termination-application
call python termination.py
rem sleep 1 second
ping 127.0.0.1 -n 1 > NUL

rem stop RabbitMQ broker => stop all the clients
echo End of demonstration
echo Stopping the broker...
call rabbitmqctl stop_app
call rabbitmqctl stop

endlocal