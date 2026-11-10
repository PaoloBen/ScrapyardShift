package model.contract;

import model.station.StationType;

public interface Processable {
    void applyStation(StationType station);
}