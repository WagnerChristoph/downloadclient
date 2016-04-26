/*
 * DownloadClient Geodateninfrastruktur Bayern
 *
 * (c) 2016 GSt. GDI-BY (gdi.bayern.de)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.bayern.gdi.gui;

/**
 * @author Jochen Saalfeld (jochen@intevation.de)
 */

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.geotools.data.ows.Layer;
import org.geotools.data.ows.WMSCapabilities;
import org.geotools.data.wms.WebMapServer;
import org.geotools.data.wms.request.GetMapRequest;
import org.geotools.data.wms.response.GetMapResponse;
import org.geotools.geometry.jts.ReferencedEnvelope;


/**
 * This class is going to Manage the Display of a Map based on a WFS Service.
 * It should have some widgets to zoom and to draw a Bounding Box.
 */
public class WMSMap extends Parent {

    //http://docs.geotools.org/latest/userguide/tutorial/raster/image.html
    //https://github.com/rafalrusin/geotools-fx-test/blob/master/src/geotools
    // /fx/test/GeotoolsFxTest.java
    private String outerBBOX;
    private String serviceURL;
    private String serviceName;
    private int dimensionX;
    private int dimensionY;
    private static final String FORMAT = "image/png";
    private static final boolean TRANSPARACY = true;
    private static final String INIT_SPACIAL_REF_SYS = "EPSG:31468";
    private static final int INIT_LAYER_NUMBER = 1;
    private String spacialRefSystem;
    WebMapServer wms;
    private static final Logger log
            = Logger.getLogger(WMSMap.class.getName());
    private WMSCapabilities capabilities;
    private List layers;
    private VBox vBox;
    private Label sourceLabel;
    private ImageView iw;

    private TextField epsgField;
    private TextField boundingBoxField;
    private Button updateImageButton;

    /**
     * gets the children of this node.
     * @return the children of the node
     */
    @Override
    public ObservableList getChildren() {
        return super.getChildren();
    }

    /**
     * adds a node to this map.
     * @param n the node
     */
    public void add(Node n) {
        this.vBox.getChildren().remove(n);
        this.vBox.getChildren().add(n);
    }

    /**
     * Constructor.
     * @param serviceURL URL of the Service
     * @param serviceName Name of the Service
     * @param outerBBOX Outer Bounds of the Picture
     * @param dimensionX X Dimension of the picuter
     * @param dimensionY Y Dimenstion of the Picture
     * @param spacialRefSystem Spacial Ref System ID
     */
    public WMSMap(String serviceURL,
                  String serviceName,
                  String outerBBOX,
                  int dimensionX,
                  int dimensionY,
                  String spacialRefSystem) {
        this.serviceURL = serviceURL;
        this.serviceName = serviceName;
        this.outerBBOX = outerBBOX;
        this.dimensionX = dimensionX;
        this.dimensionY = dimensionY;
        this.spacialRefSystem = spacialRefSystem;
        this.iw = new ImageView();
        this.epsgField = new TextField(this.spacialRefSystem);
        this.boundingBoxField = new TextField(this.outerBBOX);
        this.updateImageButton = new Button("Update Image");
        vBox = new VBox();

        try {
            URL serviceURLObj = new URL(this.serviceURL);
            wms = new WebMapServer(serviceURLObj);
            capabilities = wms.getCapabilities();
            layers = capabilities.getLayerList();
            this.setMapImage(this.outerBBOX,
                    this.spacialRefSystem,
                    this.INIT_LAYER_NUMBER);

            sourceLabel = new Label(this.serviceName);
            sourceLabel.setLabelFor(this.iw);
            this.add(iw);
            this.add(sourceLabel);
            this.add(epsgField);
            this.add(boundingBoxField);
            this.add(updateImageButton);
            this.getChildren().add(vBox);
            this.updateImageButton.setOnAction(
                    new UpdateImageButtonEventHandler()
            );

        } catch (Exception e) {
        //} catch (IOException | org.geotools.ows.ServiceException e) {
            //ServiceExcption from geotools.ows DOES NOT WORK! THERE IS NO
            //CLASS FOUND WHEN BOUNDLED IM MAVEN!
            //NO SPECIFIC HANDLING OF EXCEPTIONS HERE!
            log.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    /**
     * Constructor.
     * @param serviceURL URL of the Service
     * @param serviceName Name of the Service
     * @param outerBBOX Outer Bounds of the Picture
     * @param dimensionX X Dimension of the picuter
     * @param dimensionY Y Dimenstion of the Picture
     */
    public WMSMap(String serviceURL,
                  String serviceName,
                  String outerBBOX,
                  int dimensionX,
                  int dimensionY) {
        this(serviceURL,
                serviceName,
                outerBBOX,
                dimensionX,
                dimensionY,
                INIT_SPACIAL_REF_SYS);

    }

    /**
     * Constructor.
     */
    public WMSMap() {
    }

    /**
     * sets the Map Image.
     * @param bBox the Bounding Box
     * @param spacialRefSys The EPSG of the Bounding Box
     * @param layerNumber The number of the Layer
     */
    private void setMapImage(String bBox,
                             String spacialRefSys,
                             int layerNumber) {
        try {
            GetMapRequest request = wms.createGetMapRequest();
            request.setFormat(this.FORMAT);
            request.setDimensions(this.dimensionX, this.dimensionY);
            request.setTransparent(this.TRANSPARACY);
            request.setSRS(spacialRefSys);
            request.setBBox(bBox);
            request.addLayer((Layer) layers.get(layerNumber));

            GetMapResponse response
                    = (GetMapResponse) wms.issueRequest(request);
            log.log(Level.INFO, "WMS Call for Map Image: "
                    + request.getFinalURL().toString());
            Image im = new Image(response.getInputStream());
            this.iw.setImage(im);
        } catch (Exception e) {
        //} catch (IOException | org.geotools.ows.ServiceException e) {
            //ServiceExcption from geotools.ows DOES NOT WORK! THERE IS NO
            //CLASS FOUND WHEN BOUNDLED IM MAVEN!
            //NO SPECIFIC HANDLING OF EXCEPTIONS HERE!
            log.log(Level.SEVERE, e.getMessage(), e);
            this.errorPopup(e);
        }
    }

    /**
     * gets the referenced Evelope from the Map.
     * @return the reference Evelope
     */
    public ReferencedEnvelope getBounds() {
        return new ReferencedEnvelope();
    }

    /**
     * raises a dialogue with an exception.
     * @param ex the exception
     */
    public void errorPopup(Exception ex) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle("Something went wrong");
        alert.setHeaderText("An Excpetion was raised!");
        alert.setContentText(ex.getMessage());


// Create expandable Exception.
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        ex.printStackTrace(pw);
        String exceptionText = sw.toString();

        Label label = new Label("The exception stacktrace was:");

        TextArea textArea = new TextArea(exceptionText);
        textArea.setEditable(false);
        textArea.setWrapText(true);

        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);
        GridPane.setVgrow(textArea, Priority.ALWAYS);
        GridPane.setHgrow(textArea, Priority.ALWAYS);

        GridPane expContent = new GridPane();
        expContent.setMaxWidth(Double.MAX_VALUE);
        expContent.add(label, 0, 0);
        expContent.add(textArea, 0, 1);

// Set expandable Exception into the dialog pane.
        alert.getDialogPane().setExpandableContent(expContent);

        alert.showAndWait();
    }
    /**
     * Event Handler for the choose Service Button.
     */
    private class UpdateImageButtonEventHandler implements
            EventHandler<ActionEvent> {
        @Override
        public void handle(ActionEvent e) {
            setMapImage(epsgField.getText(),
                    boundingBoxField.getText(),
                    INIT_LAYER_NUMBER);
        }
    }
}
