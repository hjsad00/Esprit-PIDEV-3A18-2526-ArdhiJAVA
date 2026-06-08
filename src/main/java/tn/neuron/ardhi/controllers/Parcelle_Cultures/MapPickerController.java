package tn.neuron.ardhi.controllers.Parcelle_Cultures;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import tn.neuron.ardhi.services.Parcelle_Cultures.LocationService;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.function.Consumer;

public class MapPickerController implements Initializable {

    @FXML private WebView webView;
    @FXML private Label lblLat;
    @FXML private Label lblLon;
    @FXML private Label lblAdresse;

    private double selectedLat = 0;
    private double selectedLon = 0;
    private String selectedAdresse = "";

    private Consumer<SelectionResult> onSelectionMade;
    private final LocationService locationService = new LocationService();

    public static class SelectionResult {
        public double lat;
        public double lon;
        public String adresse;
        public SelectionResult(double lat, double lon, String adresse) {
            this.lat = lat; this.lon = lon; this.adresse = adresse;
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        WebEngine engine = webView.getEngine();
        
        // Use a listener to setup the bridge safely
        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                try {
                    // Using reflection to avoid direct dependency issues during FXML load if module is restricted
                    Object win = engine.executeScript("window");
                    if (win instanceof netscape.javascript.JSObject) {
                        ((netscape.javascript.JSObject) win).setMember("javaConnector", new JavaConnector());
                    }
                } catch (Exception e) {
                    System.err.println("Erreur Bridge JS: " + e.getMessage());
                }
            }
        });

        String html = "<!DOCTYPE html><html><head>" +
                "<meta charset='utf-8'/>" +
                "<link rel='stylesheet' href='https://unpkg.com/leaflet@1.9.4/dist/leaflet.css'/>" +
                "<script src='https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'></script>" +
                "<style>" +
                "html,body{margin:0;padding:0;height:100%;overflow:hidden;}" +
                "#map{height:100%;width:100%;background-color:#f4f4f4;}" +
                ".leaflet-tile{will-change:unset!important;backface-visibility:visible!important;}" +
                ".leaflet-fade-anim .leaflet-tile,.leaflet-zoom-anim .leaflet-zoom-animated{will-change:auto!important;}" +
                "</style>" +
                "</head><body>" +
                "<div id='map'></div>" +
                "<script>" +
                // Fixes spécifiques JavaFX WebView inspirés de html/map.html
                "L.Browser.any3d=false;" +
                "L.Browser.retina=false;" +
                "var map = L.map('map',{" +
                " fadeAnimation:false,zoomAnimation:false,markerZoomAnimation:false," +
                " zoomSnap:1,zoomDelta:1,trackResize:true,preferCanvas:true" +
                "}).setView([36.8065,10.1815],8);" +
                "L.tileLayer('https://mt1.google.com/vt/lyrs=y&x={x}&y={y}&z={z}',{" +
                " maxZoom:20,attribution:'© Google Satellite'}).addTo(map);" +
                "var marker;" +
                // Fix JavaFX WebView: forcer recalcul après chaque zoom/move
                "setInterval(function(){map.invalidateSize({animate:false});},500);" +
                // Utiliser mousedown au niveau DOM pour coordonnées pixel fiables
                "document.getElementById('map').addEventListener('click',function(evt){" +
                "  var rect=this.getBoundingClientRect();" +
                "  var x=evt.clientX-rect.left;" +
                "  var y=evt.clientY-rect.top;" +
                "  var latlng=map.containerPointToLatLng(L.point(x,y));" +
                "  if(marker) map.removeLayer(marker);" +
                "  marker=L.marker(latlng).addTo(map);" +
                "  if(window.javaConnector) window.javaConnector.onMapClick(latlng.lat,latlng.lng);" +
                "});" +
                "</script></body></html>";

        engine.loadContent(html);
    }

    public void setOnLocationSelected(Consumer<SelectionResult> callback) {
        this.onSelectionMade = callback;
    }

    @FXML
    public void confirmer() {
        if (selectedLat != 0 && selectedLon != 0) {
            if (onSelectionMade != null) {
                onSelectionMade.accept(new SelectionResult(selectedLat, selectedLon, selectedAdresse));
            }
            if (webView.getScene() != null && webView.getScene().getWindow() != null) {
                ((Stage) webView.getScene().getWindow()).close();
            }
        }
    }

    public class JavaConnector {
        public void onMapClick(double lat, double lng) {
            Platform.runLater(() -> {
                selectedLat = lat;
                selectedLon = lng;
                lblLat.setText(String.format("%.6f", lat));
                lblLon.setText(String.format("%.6f", lng));
                
                new Thread(() -> {
                    try {
                        String addr = locationService.reverseGeocode(lat, lng);
                        Platform.runLater(() -> {
                            if (addr != null && !addr.isEmpty()) {
                                selectedAdresse = addr;
                                lblAdresse.setText(addr);
                            } else {
                                lblAdresse.setText("Adresse locale detectee");
                            }
                        });
                    } catch (Exception e) {
                        Platform.runLater(() -> lblAdresse.setText("Erreur geocodage"));
                    }
                }).start();
            });
        }
    }
}
