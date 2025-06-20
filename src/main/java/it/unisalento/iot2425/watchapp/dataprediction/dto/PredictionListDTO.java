package it.unisalento.iot2425.watchapp.dataprediction.dto;

import java.util.List;

public class PredictionListDTO {
    private List<PredictionDTO> list;

    public List<PredictionDTO> getList() {
        return list;
    }

    public void setList(List<PredictionDTO> list) {
        this.list = list;
    }
}
