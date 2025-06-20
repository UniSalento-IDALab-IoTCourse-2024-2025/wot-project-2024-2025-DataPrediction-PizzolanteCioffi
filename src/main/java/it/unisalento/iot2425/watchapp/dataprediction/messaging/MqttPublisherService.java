package it.unisalento.iot2425.watchapp.dataprediction.messaging;

import org.eclipse.paho.client.mqttv3.*;
import org.springframework.stereotype.Service;

@Service
public class MqttPublisherService {
    private static final String BROKER_URL = "tcp://23.21.148.21:1883";
    private static final String CLIENT_ID = "spring-mqtt-publisher";

    private MqttClient mqttClient;

    public MqttPublisherService() {
        try {
            mqttClient = new MqttClient(BROKER_URL, CLIENT_ID);
            MqttConnectOptions options = new MqttConnectOptions();
            options.setAutomaticReconnect(true);
            options.setCleanSession(true);
            mqttClient.connect(options);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void publish(String topic, byte[] payload, int qos){
        try {
            MqttMessage mqttMessage = new MqttMessage(payload);
            mqttMessage.setQos(qos);
            mqttClient.publish(topic, mqttMessage);
            System.out.println("Messaggio pubblicato su " + topic + ": " + mqttMessage);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
}
