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

package net.totem.gamelogic.gamemaster;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import net.totem.gamelogic.ActionInvocationException;
import net.totem.gamelogic.Util;

import com.rabbitmq.client.ConsumerCancelledException;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.ShutdownSignalException;

public class ConsumeChannel extends RabbitMQGameInstanceChannel {
	
	public final static String RAW_ACTION_KIND = "rawActionKind";
	public final static String RAW_ACTION_NAME = "rawAction";

	private List<List<Map<String, ? extends GameMasterActionInterface>>> listsOfListOfActions;
	private boolean rawActionsEnabled;

	ConsumeChannel(	final ChannelsManager channelsManager,
			final GameMasterState state,
			final List<Map<String, ? extends GameMasterActionInterface>> loai,
			boolean enableRawActions) 
			throws IOException {
		super(channelsManager, state);
		listsOfListOfActions = new Vector<List<Map<String, ? extends GameMasterActionInterface>>>();
		listsOfListOfActions.add(ListOfActions.ListOfActionsMaps);
		listsOfListOfActions.add(loai);
		this.rawActionsEnabled = enableRawActions;
		startConsumeLoopThread(state);
	}
	
	private void startConsumeLoopThread(final GameMasterState state){
		new Thread() {
			public void run() {
				consumeLoop(state);
			}
		}.start();		
	}

	
	private void consumeLoop(final GameMasterState state) {
		if (state == null) {
			throw new IllegalArgumentException("consumeLoop with null state");
		}
		if (channel == null) {
			throw new IllegalStateException("consumeLoop with null channel");
		}
		if (state.login == null) {
			throw new IllegalArgumentException(
			"consumeLoop with null queue name");
		}
		// instantiate consumer
		QueueingConsumer consumer = getQueingConsumer(state);
		QueueingConsumer.Delivery delivery = null;
		// consuming routine
		while (!state.hasConnectionExited()) {
			try {
				delivery = consumer.nextDelivery();
				// acknowledge message: avoid to keep useless consumer receiving message from broker
				channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
			} catch (ShutdownSignalException e) {
				consumer = newQueueingConsumer(e, state);
				if(consumer != null){
					// directly try to get a new delivery
					continue;
				}
			} catch (ConsumerCancelledException e) {
				consumer = newQueueingConsumer(e, state);
				if(consumer != null){
					// directly try to get a new delivery
					continue;
				}
			} catch (InterruptedException e) {
				consumer = newQueueingConsumer(e, state);
				if(consumer != null){
					// directly try to get a new delivery
					continue;
				}
			} catch (IOException e) {
				// Error on acknowledgment of message: message will be inserted at the end of the master queue
				consumer = newQueueingConsumer(e, state);
				if(consumer != null){
					// directly try to get a new delivery
					continue;
				}	
			} 
			// handle message
			handleDelivery(delivery,state);
		}
	}


	// Return a QueueingConsumer that expect acknowledgment of messages
	private QueueingConsumer getQueingConsumer(final GameMasterState state){
		QueueingConsumer consumer = new QueueingConsumer(channel);
		try {
			channel.basicConsume(state.login, false, consumer);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		return consumer;
	}


	private void handleDelivery(QueueingConsumer.Delivery delivery, final GameMasterState state){
		if ((delivery != null) && (delivery.getBody() != null) && (delivery.getEnvelope().getRoutingKey() != null)) {
			String body = new String(delivery.getBody());
			String headerString = delivery.getEnvelope().getRoutingKey();
			Util.println(" [Master " + state.login + "] received on "
					+ state.virtualHost + ": " + body + ", with routing key = "
					+ headerString);
			String[] header = headerString.split("\\"
					+ Util.getRabbitMQProperties().getProperty(
					"routingKeySeparator"));
			if (header.length == 4) {
				String actionKind = header[2];
				String actionName = header[3];
				try {
					executeAction(actionKind, actionName, state, header, body);
				} catch (ActionInvocationException e) {
					Util.println(" [Master " + state.login
							+ "] Unknown action: " + e.getMessage());
					e.printStackTrace();
				}
			}else{
				Util.println(" [Master " + state.login + "] Malformed message "
						+ "(header of length " + header.length
						+ "instead of 4)");
				Util.println(" [Master " + state.login + "] Ignore message");
			}
		}
	}
	

	private Object executeAction(final String nameKind,
			final String nameAction, final GameMasterState state,
			final String[] header, final String body)
	throws ActionInvocationException {
		Object result = null;
		boolean found = false;
		for (List<Map<String, ? extends GameMasterActionInterface>> aml : listsOfListOfActions) {
			for (Map<String, ? extends GameMasterActionInterface> am : aml) {
				GameMasterActionInterface action = am.get(nameKind
						+ Util.getRabbitMQProperties().getProperty(
						"routingKeySeparator") + nameAction);
				if (action != null) {
					found = true;
					result = action.execute(state, header, body);
				}
			}
		}
		if (!found) {
			if(rawActionsEnabled){
				result = executeRawAction(state, header, body);
			}else{
				throw new ActionInvocationException("Unknown "
						+ "game instance action '"
						+ nameKind
						+ Util.getRabbitMQProperties().getProperty(
						"routingKeySeparator") + nameAction + "'");
			}
		}
		return result;
	}
	
	
	private Object executeRawAction(final GameMasterState state, 
			final String[] header, final String body) 
	throws ActionInvocationException{
		Object result = null;
		boolean found = false;
		for (List<Map<String, ? extends GameMasterActionInterface>> aml : listsOfListOfActions) {
			for (Map<String, ? extends GameMasterActionInterface> am : aml) {
				GameMasterActionInterface action = am.get(RAW_ACTION_KIND
						+ Util.getRabbitMQProperties().getProperty(
						"routingKeySeparator") + RAW_ACTION_NAME);
				if (action != null) {
					found = true;
					result = action.execute(state, header, body);
				}
			}
		}
		if (!found) {
			throw new ActionInvocationException("WARNING : Raw action kind is not registered in the list of Game Master Actions. "+
					"You must either create and register it, or set enableRawActions to false in the" +
			" constructpr of your ChannelsManager.");
		}
		return result;
	}


	/*
	 * If state has not exited, this method:
	 * - print the exception message
	 * - close channel and connection
	 * - try to reconnect
	 * - return a new QueuingConsumer attached to the new channel.
	 */
	private QueueingConsumer newQueueingConsumer(Exception e, final GameMasterState state){
		QueueingConsumer consumer = null;
		if(!state.hasConnectionExited()){
			Util.println(" [Master " + state.login + "] "+e.getClass().getSimpleName()+" : "+ e.getMessage());
			e.printStackTrace();
			if (channel.isOpen()) {
				// close channel and connection
				close();
			}
			boolean reconnected = initCommunicationWithBroker(state);
			if(reconnected){
				// re instantiate consumer
				consumer = getQueingConsumer(state);
			}
		}
		return consumer;
	}
}
