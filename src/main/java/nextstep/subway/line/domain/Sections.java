package nextstep.subway.line.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.persistence.CascadeType;
import javax.persistence.Embeddable;
import javax.persistence.OneToMany;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.WeightedMultigraph;
import org.jgrapht.graph.builder.GraphBuilder;

import nextstep.subway.common.exception.SubwayErrorCode;
import nextstep.subway.common.exception.SubwayException;
import nextstep.subway.station.domain.Station;

@Embeddable
public class Sections {
    @OneToMany(mappedBy = "line", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, orphanRemoval = true)
    private List<Section> sections = new ArrayList<>();

    private Sections(List<Section> sections) {
        this.sections = new ArrayList<>(sections);
    }

    protected Sections() {
    }

    public static Sections empty() {
        return new Sections();
    }

    public static Sections from(List<Section> sections) {
        return new Sections(sections);
    }

    public boolean addSection(Section section) {
        return this.sections.add(section);
    }

    public List<Station> getStations() {
        Stations stations = makeStations();

        return stations.getStations();
    }

    private Stations makeStations() {
        if (sections.isEmpty()) {
            return new Stations();
        }

        Station station = sections.get(0).getUpStation();

        Station upStation = findUpStation(station);

        List<Station> stations = makeStations(upStation);
        return new Stations(stations);
    }

    private List<Station> makeStations(Station downStation) {
        List<Station> stations = new ArrayList<>();
        stations.add(downStation);

        Optional<Section> nextLineStation = findUpLineStation(downStation);
        if (!nextLineStation.isPresent()) {
            return stations;
        }

        Station station = nextLineStation.get().getDownStation();
        stations.addAll(makeStations(station));
        return stations;
    }

    public void removeStation(Station station) {
        validateRemoveStation();

        Optional<Section> upLineStation = findUpLineStation(station);
        Optional<Section> downLineStation = findDownLineStation(station);

        if (upLineStation.isPresent() && downLineStation.isPresent()) {
            Section section = Section.combine(downLineStation.get(), upLineStation.get());
            sections.add(section);
        }

        upLineStation.ifPresent(section -> sections.remove(section));
        downLineStation.ifPresent(section -> sections.remove(section));
    }

    private void validateRemoveStation() {
        if (sections.size() <= 1) {
            throw new SubwayException(SubwayErrorCode.CANNOT_DELETE_LAST_LINE);
        }
    }

    private Station findUpStation(Station downStation) {
        Optional<Section> nextLineStation = findDownLineStation(downStation);
        if (!nextLineStation.isPresent()) {
            return downStation;
        }

        Station upStation = nextLineStation.get().getUpStation();
        return findUpStation(upStation);
    }

    private Optional<Section> findDownLineStation(Station station) {
        return sections.stream()
            .filter(section -> section.hasDownStation(station))
            .findFirst();
    }

    private Optional<Section> findUpLineStation(Station station) {
        return sections.stream()
            .filter(section -> section.hasUpStation(station))
            .findFirst();
    }

    public void addStation(Section section) {
        Stations stations = makeStations();

        stations.checkCanAddStation(section);

        if (stations.isNotEmpty()) {
            updateUpStationIfExists(section, stations);
            updateDownStationIfExists(section, stations);
        }

        addSection(section);
    }

    private void updateUpStationIfExists(Section section, Stations stations) {
        boolean isUpStationExisted = stations.containsStation(section.getUpStation());
        if (isUpStationExisted) {
            findUpLineStation(section.getUpStation())
                .ifPresent(upLineStation -> upLineStation
                    .updateUpStation(section.getDownStation(), section.getDistance()));
        }
    }

    private void updateDownStationIfExists(Section section, Stations stations) {
        boolean isDownStationExisted = stations.containsStation(section.getDownStation());
        if (isDownStationExisted) {
            findDownLineStation(section.getDownStation())
                .ifPresent(downLineStation -> downLineStation
                    .updateDownStation(section.getUpStation(), section.getDistance()));
        }
    }

    public Graph<Station, DefaultWeightedEdge> makeGraph() {
        GraphBuilder<Station, DefaultWeightedEdge, WeightedMultigraph<Station, DefaultWeightedEdge>> graphBuilder =
            new GraphBuilder<>(new WeightedMultigraph<>(DefaultWeightedEdge.class));

        sections.forEach(section -> {
            graphBuilder.addVertex(section.getUpStation());
            graphBuilder.addVertex(section.getDownStation());
            graphBuilder.addEdge(section.getUpStation(), section.getDownStation(), section.getDistanceValue());
        });

        return graphBuilder.buildAsUnmodifiable();
    }

}
