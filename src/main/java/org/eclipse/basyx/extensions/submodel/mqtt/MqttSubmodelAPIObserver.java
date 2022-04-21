/*******************************************************************************
* Copyright (C) 2021 the Eclipse BaSyx Authors
*
* This program and the accompanying materials are made
* available under the terms of the Eclipse Public License 2.0
* which is available at https://www.eclipse.org/legal/epl-2.0/
*
* SPDX-License-Identifier: EPL-2.0
******************************************************************************/
package org.eclipse.basyx.extensions.submodel.mqtt;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.basyx.extensions.shared.mqtt.MqttEventService;
import org.eclipse.basyx.submodel.metamodel.api.ISubmodel;
import org.eclipse.basyx.submodel.metamodel.api.identifier.IIdentifier;
import org.eclipse.basyx.submodel.metamodel.api.reference.IKey;
import org.eclipse.basyx.submodel.metamodel.api.reference.IReference;
import org.eclipse.basyx.submodel.restapi.observing.ISubmodelAPIObserver;
import org.eclipse.basyx.submodel.restapi.observing.ObservableSubmodelAPI;
import org.eclipse.basyx.vab.modelprovider.VABPathTools;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link ISubmodelAPIObserver}
 * Triggers MQTT events for different CRUD operations on the submodel.
 * 
 * @author conradi
 *
 */
public class MqttSubmodelAPIObserver extends MqttEventService implements ISubmodelAPIObserver {
	private static Logger logger = LoggerFactory.getLogger(MqttSubmodelAPIObserver.class);
	
	// Submodel Element whitelist for filtering
	protected boolean useWhitelist = false;
	protected Set<String> whitelist = new HashSet<>();
	
	private IIdentifier aasIdentifier;
	
	private IIdentifier submodelIdentifier;
	
	public MqttSubmodelAPIObserver(MqttClient client, IIdentifier aasId, IIdentifier submodelIdentifier) throws MqttException {
		super(client);
		
		this.aasIdentifier = aasId;
		
		this.submodelIdentifier = submodelIdentifier;
	}
	
	/**
	 * @deprecated This method is deprecated please use {@link #MqttSubmodelAPIObserver(MqttClient, IIdentifier, IIdentifier)}
	 * Constructor for adding this MQTT extension on top of another SubmodelAPI
	 * 
	 * @param observedAPI The underlying submodelAPI
	 * @throws MqttException
	 */
	@Deprecated
	public MqttSubmodelAPIObserver(ObservableSubmodelAPI observedAPI, String serverEndpoint, String clientId) throws MqttException {
		this(observedAPI, serverEndpoint, clientId, new MqttDefaultFilePersistence());
		
		this(clientId, getAASId(observedAPI), getSubmodelId(observedAPI));
	}

	/**
	 * @deprecated This method is deprecated please use {@link #MqttSubmodelAPIObserver(MqttClient, IIdentifier, IIdentifier)}
	 * Constructor for adding this MQTT extension on top of another SubmodelAPI with
	 * a custom persistence strategy
	 */
	@Deprecated
	public MqttSubmodelAPIObserver(ObservableSubmodelAPI observedAPI, String brokerEndpoint, String clientId, MqttClientPersistence persistence) throws MqttException {
		super(brokerEndpoint, clientId, persistence);
		logger.info("Create new MQTT submodel for endpoint " + brokerEndpoint);
		observedAPI.addObserver(this);
		sendMqttMessage(MqttSubmodelAPIHelper.TOPIC_CREATESUBMODEL, observedAPI.getSubmodel().getIdentification().getId());
	}

	/**
	 * @deprecated This method is deprecated please use {@link #MqttSubmodelAPIObserver(MqttClient, IIdentifier, IIdentifier)}
	 * Constructor for adding this MQTT extension on top of another SubmodelAPI
	 * 
	 * @param observedAPI The underlying submodelAPI
	 * @throws MqttException
	 */
	@Deprecated
	public MqttSubmodelAPIObserver(ObservableSubmodelAPI observedAPI, String serverEndpoint, String clientId, String user, char[] pw)
			throws MqttException {
		this(observedAPI, serverEndpoint, clientId, user, pw, new MqttDefaultFilePersistence());
	}

	/**
	 * @deprecated This method is deprecated please use {@link #MqttSubmodelAPIObserver(MqttClient, IIdentifier, IIdentifier)}
	 * Constructor for adding this MQTT extension on top of another SubmodelAPI with
	 * credentials and persistency strategy
	 */
	@Deprecated
	public MqttSubmodelAPIObserver(ObservableSubmodelAPI observedAPI, String serverEndpoint, String clientId, String user, char[] pw, MqttClientPersistence persistence) throws MqttException {
		super(serverEndpoint, clientId, user, pw);
		logger.info("Create new MQTT submodel for endpoint " + serverEndpoint);
		observedAPI.addObserver(this);
		sendMqttMessage(MqttSubmodelAPIHelper.TOPIC_CREATESUBMODEL, observedAPI.getSubmodel().getIdentification().getId());
	}

	/**
	 * @deprecated This method is deprecated please use {@link #MqttSubmodelAPIObserver(MqttClient, IIdentifier, IIdentifier)}
	 * Constructor for adding this MQTT extension on top of another SubmodelAPI.
	 * 
	 * @param observedAPI The underlying submodelAPI
	 * @param client      An already connected mqtt client
	 * @throws MqttException 
	 */
	@Deprecated
	public MqttSubmodelAPIObserver(ObservableSubmodelAPI observedAPI, MqttClient client) throws MqttException {
		super(client);
		observedAPI.addObserver(this);
		sendMqttMessage(MqttSubmodelAPIHelper.TOPIC_CREATESUBMODEL, observedAPI.getSubmodel().getIdentification().getId());
	}

	/**
	 * Adds a submodel element to the filter whitelist. Can also be a path for nested submodel elements.
	 * 
	 * @param shortId
	 */
	public void observeSubmodelElement(String shortId) {
		whitelist.add(VABPathTools.stripSlashes(shortId));
	}

	/**
	 * Sets a new filter whitelist.
	 * 
	 * @param shortIds
	 */
	public void setWhitelist(Set<String> shortIds) {
		this.whitelist.clear();
		for (String entry : shortIds) {
			this.whitelist.add(VABPathTools.stripSlashes(entry));
		}
	}

	/**
	 * Disables the submodel element filter whitelist
	 * 
	 */
	public void disableWhitelist() {
		useWhitelist = false;
	}

	/**
	 * Enables the submodel element filter whitelist
	 * 
	 */
	public void enableWhitelist() {
		useWhitelist = true;
	}
	
	@Override
	public void elementAdded(String idShortPath, Object newValue) {
		if (filter(idShortPath)) {
			sendMqttMessage(MqttSubmodelAPIHelper.TOPIC_ADDELEMENT, getCombinedMessage(getAASId(), getSubmodelId(), idShortPath));
		}
	}

	@Override
	public void elementDeleted(String idShortPath) {
		if (filter(idShortPath)) {
			sendMqttMessage(MqttSubmodelAPIHelper.TOPIC_DELETEELEMENT, getCombinedMessage(getAASId(), getSubmodelId(), idShortPath));
		}
	}

	@Override
	public void elementUpdated(String idShortPath, Object newValue) {
		if (filter(idShortPath)) {
			sendMqttMessage(MqttSubmodelAPIHelper.TOPIC_UPDATEELEMENT, getCombinedMessage(getAASId(), getSubmodelId(), idShortPath));
		}
	}	
	
	public static String getCombinedMessage(String aasId, String submodelId, String elementPart) {
		elementPart = VABPathTools.stripSlashes(elementPart);
		return "(" + aasId + "," + submodelId + "," + elementPart + ")";
	}

	private boolean filter(String idShort) {
		idShort = VABPathTools.stripSlashes(idShort);
		return !useWhitelist || whitelist.contains(idShort);
	}
	
	private String getSubmodelId() {
		ISubmodel submodel = observedAPI.getSubmodel();
		return submodel.getIdentification().getId();
	}

	private String getAASId() {
		ISubmodel submodel = observedAPI.getSubmodel();
		IReference parentReference = submodel.getParent();
		System.out.println("Parent Ref : " + parentReference.toString());
		if (parentReference != null) {
			List<IKey> keys = parentReference.getKeys();
			if (keys != null && keys.size() > 0) {
				return keys.get(0).getValue();
			}
		}
		return null;
	}

}
