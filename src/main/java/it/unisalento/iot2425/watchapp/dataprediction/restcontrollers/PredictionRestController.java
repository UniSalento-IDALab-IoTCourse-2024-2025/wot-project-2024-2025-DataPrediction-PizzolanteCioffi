package it.unisalento.iot2425.watchapp.dataprediction.restcontrollers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.unisalento.iot2425.watchapp.dataprediction.domain.Prediction;
import it.unisalento.iot2425.watchapp.dataprediction.dto.PredictionDTO;
import it.unisalento.iot2425.watchapp.dataprediction.dto.PredictionListDTO;
import it.unisalento.iot2425.watchapp.dataprediction.messaging.MqttPublisherService;
import it.unisalento.iot2425.watchapp.dataprediction.repositories.PredictionRepository;
import it.unisalento.iot2425.watchapp.dataprediction.security.JwtUtilities;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(value ="/api/prediction")
public class PredictionRestController {

    @Autowired
    private JwtUtilities jwtUtilities;

    @Autowired
    private PredictionRepository predictionRepository;

    @Autowired
    MqttPublisherService publisherService;

    @RequestMapping(value = "/AIModel",
            method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> AIModel(HttpServletRequest request) throws JsonProcessingException {

        final String authHeader = request.getHeader("Authorization");

        String token = authHeader.substring(7);

        String patientId = jwtUtilities.extractUserId(token);

        String today= LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        //prendiamo tutti i dati della data di oggi

        String uri = "http://23.21.148.21:8081/api/data/getAllByDate?date=" + today + "&patientId="+ patientId;

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);

        HttpEntity<String> entity = new HttpEntity<>("parameters", headers);
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(uri, HttpMethod.GET, entity, new ParameterizedTypeReference<>() {});
        Map<String, Object> jsonBody = response.getBody();

        //prendiamo l'id dell'assistente a partire da quello del paziente

        uri = "http://54.167.160.164:8080/api/users/patient/" + patientId;

        restTemplate = new RestTemplate();
        headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        entity = new HttpEntity<>("parameters", headers);
        response = restTemplate.exchange(uri, HttpMethod.GET, entity, new ParameterizedTypeReference<>() {});
        Map<String, Object> httpData = response.getBody();
        String assistantId = (String) httpData.get("assistantId");
        String address = (String) httpData.get("address");

        jsonBody.put("address", address);

        ObjectMapper mapper = new ObjectMapper();
        String jsonBodyString = mapper.writeValueAsString(jsonBody);


        //chiamiamo il modello di AI per effettuare inferenza sopra i dati presi dalla getAllByDate

        uri = "http://54.147.158.159:8084/api/model";

        restTemplate = new RestTemplate();
        headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        entity=new HttpEntity<>(jsonBodyString, headers);

        response = restTemplate.exchange(uri, HttpMethod.POST, entity, new ParameterizedTypeReference<>() {});

        //python resistuisce un json avente behaviour che sia già castato ad intero (e patientId).

        Map<String, Object> result = response.getBody();
        Integer behaviour= (Integer) result.get("behaviour");

        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        ZonedDateTime now=ZonedDateTime.now(ZoneId.of("Europe/Rome"));
        String time = now.toLocalTime().format(timeFormatter);

        Prediction prediction = new Prediction();
        prediction.setAssistantId(assistantId);
        prediction.setPatientId(patientId);
        prediction.setDate(today);
        prediction.setTime(time);
        prediction.setBehaviour(behaviour);

        predictionRepository.save(prediction);

        //ottenuto il behaviour per quel patient, lo aggiorno

        uri = "http://54.167.160.164:8080/api/users/updateBehaviour/" + patientId;

        restTemplate = new RestTemplate();
        headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> entityPut = new HashMap<>();
        entityPut.put("behaviour", behaviour);

        HttpEntity<Map<String, Object>> entity1 =new HttpEntity<>(entityPut, headers);

        response = restTemplate.exchange(uri, HttpMethod.PUT, entity1, new ParameterizedTypeReference<Map<String, Object>>() {});

        //chiama api per vedere se ci sono più di 4 predizioni "non buone"
        Integer numBadPrediction =4;

        uri = "http://localhost:8080/api/prediction/countByDateAndPatient?date=" + today + "&patientId=" + patientId ;

        restTemplate = new RestTemplate();
        headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);

        entity = new HttpEntity<>("parameters", headers);
        ResponseEntity<Integer> response1= restTemplate.exchange(uri, HttpMethod.GET, entity, Integer.class);

        Integer count=response1.getBody();

        if(count > numBadPrediction){
            //mandiamo la notifica in coda.
            Map<String, Object> message = new HashMap<>();

            message.put("patientId", patientId);
            message.put("assistantId", assistantId);
            message.put("date", today);
            message.put("message", "Abbiamo rilevato un comportamento potenzialmente dannoso per la salute. Ti invitiamo a consultare la sezione dedicata dell’applicazione per ricevere indicazioni su come migliorare la situazione");

            ObjectMapper objectMapper = new ObjectMapper();
            byte[] payload = objectMapper.writeValueAsBytes(message);
            publisherService.publish("data", payload, 1);

        }

        PredictionDTO predictionDTO = new PredictionDTO();
        predictionDTO.setAssistantId(prediction.getAssistantId());
        predictionDTO.setPatientId(prediction.getPatientId());
        predictionDTO.setDate(prediction.getDate());
        predictionDTO.setTime(predictionDTO.getTime());
        predictionDTO.setId(prediction.getId());
        predictionDTO.setBehaviour(prediction.getBehaviour());

        return ResponseEntity.status(HttpStatus.OK).body(predictionDTO);

    }

    @RequestMapping(value = "/countByDateAndPatient",
            method = RequestMethod.GET)
    public ResponseEntity<Integer> countByDateAndPatient(
            @RequestParam String patientId,
            @RequestParam String date) {
        int count = predictionRepository
                .findByPatientIdAndDateAndBehaviour(patientId, date, 1)
                .size();
        return ResponseEntity.ok(count);
    }


    @RequestMapping(value="/",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public PredictionListDTO getAll() {
        List<Prediction> notifications = predictionRepository.findAll();
        List<PredictionDTO> list = new ArrayList<>();

        for (Prediction notification : notifications) {

            PredictionDTO notificationDTO = new PredictionDTO();
            notificationDTO.setId(notification.getId());
            notificationDTO.setAssistantId(notification.getAssistantId());
            notificationDTO.setPatientId(notification.getPatientId());
            notificationDTO.setDate(notification.getDate());
            notificationDTO.setTime(notification.getTime());
            notificationDTO.setBehaviour(notification.getBehaviour());

            list.add(notificationDTO);
        }

        PredictionListDTO notificationListDTO = new PredictionListDTO();
        notificationListDTO.setList(list);

        return notificationListDTO;

    }









}
