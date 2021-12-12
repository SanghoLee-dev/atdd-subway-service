package nextstep.subway.line.domain;

import java.util.Arrays;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import nextstep.subway.station.domain.Station;

@Entity
public class Section {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "line_id")
    private Line line;

    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "up_station_id")
    private Station upStation;

    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "down_station_id")
    private Station downStation;

    private int distance;

    public Section() {
    }

    public Section(Line line, Station upStation, Station downStation, int distance) {
        this.line = line;
        this.upStation = upStation;
        this.downStation = downStation;
        this.distance = distance;
    }

    void divideBy(Section section) {
        if (!isOverlapped(section)) {
            throw new IllegalArgumentException("section must be overlapped to divide");
        }
        if (section.getDistance() >= distance) {
            throw new IllegalArgumentException("역과 역 사이의 거리보다 좁은 거리를 입력해주세요");
        }
        if (upStation.equals(section.getUpStation())) {
            upStation = section.getDownStation();
        }
        if (downStation.equals(section.getDownStation())) {
            downStation = section.getUpStation();
        }
        distance = distance - section.getDistance();
    }

    boolean isOverlapped(Section section) {
        return upStation.equals(section.getUpStation()) || downStation.equals(section.getDownStation());
    }

    void connectWith(final Section section) {
        if (!isNextSection(section) && !section.isNextSection(this)) {
            throw new IllegalArgumentException("section cannot be connected");
        }
        if (isNextSection(section)) {
            upStation = section.getUpStation();
        }
        if (section.isNextSection(this)) {
            downStation = section.getDownStation();
        }
        distance = distance + section.getDistance();
    }

    boolean isNextSection(final Section section) {
        return upStation.equals(section.downStation);
    }

    List<Station> getStations() {
        return Arrays.asList(upStation, downStation);
    }

    public Long getId() {
        return id;
    }

    public Line getLine() {
        return line;
    }

    public Station getUpStation() {
        return upStation;
    }

    public Station getDownStation() {
        return downStation;
    }

    public int getDistance() {
        return distance;
    }
}
