#!/bin/sh

# TCM: TOTEM Communication Middleware
# Copyright: Copyright (C) 2009-2012
# Contact: denis.conan@telecom-sudparis.eu, michel.simatic@telecom-sudparis.eu
#
# This library is free software; you can redistribute it and/or
# modify it under the terms of the GNU Lesser General Public
# License as published by the Free Software Foundation; either
# version 3 of the License, or any later version.
#
# This library is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
# Lesser General Public License for more details.
#
# You should have received a copy of the GNU Lesser General Public
# License along with this library; if not, write to the Free Software
# Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307
# USA
#
# Developer(s): Denis Conan, Gabriel Adgeg

PYTHONPATH=$PYTHONPATH:$PWD/Python-gamelogicserver/:$PWD/../src/Python-server/

# stop and re-launch the RabbitMQ broker
rabbitmqctl stop
sleep 2
rabbitmq-server -detached
sleep 2
rabbitmqctl stop_app
rabbitmqctl reset
rabbitmqctl start_app

# launch the Game Server
#(cd ../src/Python-server; ./run.sh)
#sleep 2

python ../src/Python-server/net/totem/gameserver/gameserver.py&
GAMESERVER_PID=$!
echo $GAMESERVER_PID >> ../src/Python-server/temp_pid.txt
sleep 2

# launch the Game Master Application
(cd Java-application; ./run.sh "michel" "simatic" "Master" "Tidy-City" "Instance-1")
sleep 2

# launch the Spectator Application
(cd Java-application; ./run.sh "denis" "conan" "Spectator" "Tidy-City" "Instance-1" "*.*.*.*")
sleep 3

# launch the Player Application
(cd Java-application; ./run.sh "lisa" "blum" "Player" "Tidy-City" "Instance-1")
sleep 3

echo ""
echo ""
echo "Execute the termination script to stop the demonstration."
echo ""
echo ""
