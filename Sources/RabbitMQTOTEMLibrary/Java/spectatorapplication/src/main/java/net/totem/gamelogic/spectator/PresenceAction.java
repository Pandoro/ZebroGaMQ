/**
 TCM: TOTEM Communication Middleware
 Copyright: Copyright (C) 2009-2012
 Contact: denis.conan@telecom-sudparis.eu, michel.simatic@telecom-sudparis.eu

 This library is free software; you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public
 License as published by the Free Software Foundation; either
 version 3 of the License, or any later version.

 This library is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 Lesser General Public License for more details.

 You should have received a copy of the GNU Lesser General Public
 License along with this library; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307
 USA

 Developer(s): Denis Conan, Gabriel Adgeg
 */

package net.totem.gamelogic.spectator;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import net.totem.gamelogic.ActionInvocationException;
import net.totem.gamelogic.Util;

public enum PresenceAction implements SpectatorActionInterface {
	HEARTBEAT("heartbeat") {
		public Object execute(final SpectatorState state,
				final String[] header, final String body)
				throws ActionInvocationException {
			return null;
		}
	},
	ASK_PARTICIPANTS_LIST("askParticipantsList") {
		public Object execute(final SpectatorState state,
				final String[] header, final String body)
				throws ActionInvocationException {
			return null;
		}
	},
	PARTICIPANTS_LIST("participantsList") {
		public Object execute(final SpectatorState state,
				final String[] header, final String body)
				throws ActionInvocationException {
			return null;
		}
	};

	private final static Map<String, PresenceAction> privateActionMap = new HashMap<String, PresenceAction>();
	public final static Map<String, PresenceAction> actionMap = Collections
			.unmodifiableMap(privateActionMap);
	public final static int KIND_NUMBER = 20;
	public final static int LOWER_ACTION_NUMBER = 0;
	public final static int UPPER_ACTION_NUMBER = 1000;

	private final String codeKind;
	private final String nameKind = "presence";
	private final String codeAction;
	private final String nameAction;

	static {
		for (PresenceAction gra : PresenceAction.values()) {
			privateActionMap.put(gra.toString(), gra);
		}
	}

	private PresenceAction(final String nameAction) {
		this.codeKind = String.valueOf(KIND_NUMBER);
		this.codeAction = String.valueOf(LOWER_ACTION_NUMBER + ordinal());
		this.nameAction = nameAction;
	}

	public String getCodeKind() {
		return codeKind;
	}

	public String getNameKind() {
		return nameKind;
	}

	public String getCodeAction() {
		return codeAction;
	}

	public String getNameAction() {
		return nameAction;
	}

	public String toString() {
		return nameKind
				+ Util.getRabbitMQProperties().getProperty(
						"routingKeySeparator") + nameAction;
	}
}
