/*******************************************************************************
* Copyright (C) 2021 the Eclipse BaSyx Authors
*
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*
* SPDX-License-Identifier: MIT
******************************************************************************/
package org.eclipse.basyx.extensions.submodel.mqtt;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.basyx.extensions.shared.mqtt.MqttEventService;
import org.eclipse.basyx.submodel.metamodel.api.ISubmodel;
import org.eclipse.basyx.submodel.metamodel.api.identifier.IIdentifier;
import org.eclipse.basyx.submodel.metamodel.api.identifier.IdentifierType;
import org.eclipse.basyx.submodel.metamodel.api.reference.IKey;
import org.eclipse.basyx.submodel.metamodel.api.reference.IReference;
import org.eclipse.basyx.submodel.metamodel.map.identifier.Identifier;
import org.eclipse.basyx.submodel.restapi.observing.ISubmodelAPIObserver;
import org.eclipse.basyx.submodel.restapi.observing.ObservableSubmodelAPI;
import org.eclipse.basyx.vab.modelprovider.VABPathTools;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;
import org.etsi.uri.x01903.v13.impl.CRLIdentifierTypeImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link ISubmodelAPIObserver} Triggers MQTT events for
 * different CRUD operations on the submodel.
 * 
 * @author conradi
 *
 */
public class MqttSubmodelAPIObserver extends MqttEventService implements ISubmodelAPIObserver {
	private static Logger logger = LoggerFactory.getLogger(MqttSubmodelAPIObserver.class);

	// The underlying SubmodelAPI
//	protected ObservableSubmodelAPI observedAPI;

	// Submodel Element whitelist for filtering
	protected boolean useWhitelist = false;
	protected Set<String> whitelist = new HashSet<>();
	
	private IIdentifier aasIdentifier;
	
	private IIdentifier submodelIdentifier;

	public MqttSubmodelAPIObserver(MqttClient client, IIdentifier aasId, IIdentifier submodelIdentifier, ObservableSubmodelAPI observedAPI) throws MqttException {
		super(client);
		
		mqttClient.connect();
		
		this.aasIdentifier = aasId;
		
		this.submodelIdentifier = submodelIdentifier;
		
		observedAPI.addObserver(this);
		
		sendMqttMessage(MqttSubmodelAPIHelper.TOPIC_CREATESUBMODEL, getSubmodelId2(observedAPI).getId());
	}
	
	public MqttSubmodelAPIObserver(String clientId, IIdentifier aasId, IIdentifier submodelIdentifier, String user, char[] pw, String serverEndpoint, ObservableSubmodelAPI observedAPI) throws MqttException {
		super(serverEndpoint, clientId, user, pw);
		
		this.aasIdentifier = aasId;
		
		this.submodelIdentifier = submodelIdentifier;
		
		observedAPI.addObserver(this);
		
		sendMqttMessage(MqttSubmodelAPIHelper.TOPIC_CREATESUBMODEL, getSubmodelId2(observedAPI).getId());
	}
	
	/**
	 * Constructor for adding this MQTT extension on top of another SubmodelAPI
	 * 
	 * @param observedAPI
	 *            The underlying submodelAPI
	 * @throws MqttException
	 */
	public MqttSubmodelAPIObserver(ObservableSubmodelAPI observedAPI, String serverEndpoint, String clientId) throws MqttException {
		this(observedAPI, serverEndpoint, clientId, new MqttDefaultFilePersistence());
		
//		this(new MqttClient(serverEndpoint, clientId, new MqttDefaultFilePersistence()), getAASId2(observedAPI), getSubmodelId2(observedAPI));
		System.out.print("In Constructor 1");
	}

	/**
	 * Constructor for adding this MQTT extension on top of another SubmodelAPI with
	 * a custom persistence strategy
	 */
	public MqttSubmodelAPIObserver(ObservableSubmodelAPI observedAPI, String brokerEndpoint, String clientId, MqttClientPersistence persistence) throws MqttException {
		this(new MqttClient(brokerEndpoint, clientId, persistence), getAASId2(observedAPI), getSubmodelId2(observedAPI), observedAPI);
//		super(brokerEndpoint, clientId, persistence);
		logger.info("Create new MQTT submodel for endpoint " + brokerEndpoint);
		System.out.print("In Constructor 2");
//		this.observedAPI = observedAPI;
//		observedAPI.addObserver(this);
//		sendMqttMessage(MqttSubmodelAPIHelper.TOPIC_CREATESUBMODEL, getSubmodelId2(observedAPI).getId());
	}

	/**
	 * Constructor for adding this MQTT extension on top of another SubmodelAPI
	 * 
	 * @param observedAPI
	 *            The underlying submodelAPI
	 * @throws MqttException
	 */
	public MqttSubmodelAPIObserver(ObservableSubmodelAPI observedAPI, String serverEndpoint, String clientId, String user, char[] pw) throws MqttException {
		this(observedAPI, serverEndpoint, clientId, user, pw, new MqttDefaultFilePersistence());
		System.out.print("In Constructor 3");
	}

	/**
	 * Constructor for adding this MQTT extension on top of another SubmodelAPI with
	 * credentials and persistency strategy
	 */
	public MqttSubmodelAPIObserver(ObservableSubmodelAPI observedAPI, String serverEndpoint, String clientId, String user, char[] pw, MqttClientPersistence persistence) throws MqttException {
		this(clientId, getAASId2(observedAPI), getSubmodelId2(observedAPI), user, pw, serverEndpoint, observedAPI);
//		super(serverEndpoint, clientId, user, pw);
		logger.info("Create new MQTT submodel for endpoint " + serverEndpoint);
		System.out.print("In Constructor 4");
//		this.observedAPI = observedAPI;
//		observedAPI.addObserver(this);
//		sendMqttMessage(MqttSubmodelAPIHelper.TOPIC_CREATESUBMODEL, observedAPI.getSubmodel().getIdentification().getId());
	}

	/**
	 * Constructor for adding this MQTT extension on top of another SubmodelAPI.
	 * 
	 * @param observedAPI
	 *            The underlying submodelAPI
	 * @param client
	 *            An already connected mqtt client
	 * @throws MqttException
	 */
	public MqttSubmodelAPIObserver(ObservableSubmodelAPI observedAPI, MqttClient client) throws MqttException {
		this(client, getAASId2(observedAPI), getSubmodelId2(observedAPI), observedAPI);
//		super(client);
//		this.observedAPI = observedAPI;
		System.out.print("In Constructor 5");
//		observedAPI.addObserver(this);
//		sendMqttMessage(MqttSubmodelAPIHelper.TOPIC_CREATESUBMODEL, observedAPI.getSubmodel().getIdentification().getId());
	}

	/**
	 * Adds a submodel element to the filter whitelist. Can also be a path for
	 * nested submodel elements.
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
			System.out.println("getCombined Msg Added : " + getCombinedMessage(aasIdentifier.getId(), submodelIdentifier.getId(), idShortPath));
			sendMqttMessage(MqttSubmodelAPIHelper.TOPIC_ADDELEMENT, getCombinedMessage(aasIdentifier.getId(), submodelIdentifier.getId(), idShortPath));
		}
	}

	@Override
	public void elementDeleted(String idShortPath) {
		if (filter(idShortPath)) {
			System.out.println("getCombined Msg Delete : " + getCombinedMessage(aasIdentifier.getId(), submodelIdentifier.getId(), idShortPath));
			sendMqttMessage(MqttSubmodelAPIHelper.TOPIC_DELETEELEMENT, getCombinedMessage(aasIdentifier.getId(), submodelIdentifier.getId(), idShortPath));
		}
	}

	@Override
	public void elementUpdated(String idShortPath, Object newValue) {
		if (filter(idShortPath)) {
			System.out.println("getCombined Msg Update : " + getCombinedMessage(aasIdentifier.getId(), submodelIdentifier.getId(), idShortPath));
			sendMqttMessage(MqttSubmodelAPIHelper.TOPIC_UPDATEELEMENT, getCombinedMessage(aasIdentifier.getId(), submodelIdentifier.getId(), idShortPath));
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

//	private String getSubmodelId() {
//		ISubmodel submodel = observedAPI.getSubmodel();
//		return submodel.getIdentification().getId();
//	}
	
	private static IIdentifier getSubmodelId2(ObservableSubmodelAPI observedAPI) {
		ISubmodel submodel = observedAPI.getSubmodel();
		System.out.println("Submodel Idshort : " + submodel.getIdShort());
		return submodel.getIdentification();
	}

//	private String getAASId() {
//		ISubmodel submodel = observedAPI.getSubmodel();
//		IReference parentReference = submodel.getParent();
//		System.out.println("Parent Ref : " + parentReference.toString());
//		if (parentReference != null) {
//			List<IKey> keys = parentReference.getKeys();
//			System.out.println("Keys : " +keys.toString());
//			if (keys != null && keys.size() > 0) {
//				System.out.println("Key 0: " +keys.get(0).getIdType().toString());
//				return keys.get(0).getValue();
//			}
//		}
//		return null;
//	}
	
	private static IIdentifier getAASId2(ObservableSubmodelAPI observedAPI) {
		ISubmodel submodel = observedAPI.getSubmodel();
		IReference parentReference = submodel.getParent();
		System.out.println("Parent Ref : " + parentReference.toString());
		if (parentReference != null) {
			List<IKey> keys = parentReference.getKeys();
			System.out.println("Keys : " +keys.toString());
			if (keys != null && keys.size() > 0) {
				System.out.println("Key 0: " +keys.get(0).getIdType().toString());
				IIdentifier aasId = createIdentifier(IdentifierType.fromString(keys.get(0).getIdType().toString()), keys.get(0).getValue());
				System.out.println("AAS Id : " + aasId.toString());
				System.out.println("AAS Id : " + aasId.getId());
				System.out.println("AAS Id Type : " + aasId.getIdType());
				return aasId;
			}
		}
		System.out.println("Returning Null");
		return null;
	}
	
	private static IIdentifier createIdentifier(IdentifierType idType, String id) {
		return new Identifier(idType, id);
	}

}