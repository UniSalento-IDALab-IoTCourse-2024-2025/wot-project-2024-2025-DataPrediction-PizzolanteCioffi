package it.unisalento.iot2425.watchapp.dataprediction.repositories;

import it.unisalento.iot2425.watchapp.dataprediction.domain.Prediction;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Map;

public interface PredictionRepository extends MongoRepository<Prediction, String> {

    List<Prediction> findByPatientIdAndDateAndBehaviour(String patientId, String date, int i);
}
