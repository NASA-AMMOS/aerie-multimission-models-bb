package missionmodel.dsn.support;

public class DSNStationType {
    private String dssType;
    private Double maxAzTrackingRate;
    private Double maxElTrackingRate;
    private Double maxElevation;

    public DSNStationType(String dssType, Double maxAzTrackingRate, Double maxElTrackingRate, Double maxElevation) {
        this.dssType = dssType;
        this.maxAzTrackingRate = maxAzTrackingRate;
        this.maxElTrackingRate = maxElTrackingRate;
        this.maxElevation = maxElevation;
    }

    public String getDSSType() {
        return dssType;
    }

    public Double getMaxAzTrackingRate() {
        return maxAzTrackingRate;
    }

    public Double getMaxElTrackingRate() {
        return maxElTrackingRate;
    }

    public Double getMaxElevation() {
        return maxElevation;
    }
}
